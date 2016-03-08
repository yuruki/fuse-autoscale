/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  MODIFICATION: This file is from jboss-fuse/fabric8 project.
 *  It has been modified for fuse-autoscale project by GitHub user yuruki.
 */
package com.github.yuruki.fuse.autoscale;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Closeables;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Fabric auto-scaler which when it becomes the master auto-scales
 * profiles according to their requirements defined via
 * {@link FabricService#setRequirements(io.fabric8.api.FabricRequirements)}
 */
@ThreadSafe
@Component(name = "io.fabric8.autoscale", label = "Fabric8 auto scaler", immediate = true,
        policy = ConfigurationPolicy.REQUIRE, metatype = true)
public final class AutoScaleController extends AbstractComponent implements GroupListener<AutoScalerNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleController.class);

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Property(value = "15000", label = "Poll period", description = "The number of milliseconds between polls to check if the system still has its requirements satisfied.")
    private static final String POLL_TIME = "pollTime";
    private Long pollTime;
    @Property(value = "default", label = "Autoscaler group ID", description = "ID for the autoscaler group.")
    private static final String AUTOSCALER_GROUP_ID = "autoscalerGroupId";
    private String autoscalerGroupId;
    @Property(value = AutoScaledGroupOptions.PROFILE_PATTERN_DEFAULT, label = "Profile name pattern", description = "Profiles matching this regex will be autoscaled.")
    private static final String PROFILE_PATTERN = "profilePattern";
    private Matcher profilePattern;
    @Property(value = AutoScaledGroupOptions.CONTAINER_PATTERN_DEFAULT, label = "Container name pattern", description = "Containers matching this regex will be used for profile assignment autoscaling.")
    private static final String CONTAINER_PATTERN = "containerPattern";
    private Matcher containerPattern;
    @Property(value = AutoScaledGroupOptions.CONTAINER_PREFIX_DEFAULT, label = "Container name prefix for new containers", description = "New containers will be created with this prefix and an index.")
    private static final String CONTAINER_PREFIX = "containerPrefix";
    private String containerPrefix;
    @Property(value = AutoScaledGroupOptions.SCALE_CONTAINERS_DEFAULT, label = "Scale containers", description = "true = scale with containers, false = scale with profile assignments.")
    private static final String SCALE_CONTAINERS = "scaleContainers";
    private Boolean scaleContainers;
    @Property(value = AutoScaledGroupOptions.DEFAULT_MAX_INSTANCES_PER_HOST_DEFAULT, label = "Default maximum instances per host", description = "Default maximum for instances per host when profile doesn't define it.")
    private static final String DEFAULT_MAX_INSTANCES_PER_HOST = "defaultMaxInstancesPerHost";
    private Integer defaultMaximumInstancesPerHost;
    @Property(value = AutoScaledGroupOptions.MIN_CONTAINER_COUNT_DEFAULT, label = "Minimum number of containers", description = "Minimum number of applicable containers to perform autoscaling.")
    private static final String MIN_CONTAINER_COUNT = "minContainerCount";
    private Integer minContainerCount;
    @Property(value = AutoScaledGroupOptions.MAX_DEVIATION_DEFAULT, label = "Maximum deviation, n * average (n >= 0)", description = "If one container has more than average + (n * average) profiles assigned, the excess will be reassigned.")
    private static final String MAX_DEVIATION = "maxDeviation";
    private Double maxDeviation;
    @Property(value = AutoScaledGroupOptions.INHERIT_REQUIREMENTS_DEFAULT, label = "Inherit requirements", description = "Profile dependencies will inherit their requirements from parent if their requirements are not set.")
    private static final String INHERIT_REQUIREMENTS = "inheritRequirements";
    private Boolean inheritRequirements;
    @Property(value = AutoScaledGroupOptions.AVERAGE_INSTANCES_PER_CONTAINER_DEFAULT, label = "Desired average profile instance count per container", description = "Desired average profile instance count per container. Negative value equals no value.")
    private static final String AVERAGE_INSTANCES_PER_CONTAINER = "averageInstancesPerContainer";
    private Integer averageInstancesPerContainer;
    @Property(value = AutoScaledGroupOptions.IGNORE_ERRORS_DEFAULT, label = "Don't cancel auto-scaling on error", description = "When enabled, errors will be logged but the auto-scaling will be performed regardless.")
    private static final String IGNORE_ERRORS = "ignoreErrors";
    private Boolean ignoreErrors;
    @Property(value = AutoScaledGroupOptions.MAX_CONTAINERS_PER_HOST_DEFAULT, label = "Maximum allowed auto-scaled containers per host", description = "Maximum number of auto-scaled containers per host for this group.")
    private static final String MAX_CONTAINERS_PER_HOST = "maxContainersPerHost";
    private Integer maxContainersPerHost;
    @Property(value = AutoScaledGroupOptions.VERBOSE_DEFAULT, label = "Verbose logging", description = "Log more information on what the autoscaler is doing.")
    private static final String VERBOSE = "verbose";
    private Boolean verbose;
    @Property(value = AutoScaledGroupOptions.DRY_RUN_DEFAULT, label = "Don't apply changes", description = "Don't apply any changes. Implies verbose = true.")
    private static final String DRY_RUN = "dryRun";
    private Boolean dryRun;

    private AtomicReference<Timer> timer = new AtomicReference<Timer>();

    @GuardedBy("volatile")
    private volatile Group<AutoScalerNode> group;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };
    private ZooKeeperMasterCache zkMasterCache;

    @Activate
    void activate(final Map<String, String> properties) {
        this.pollTime = Long.parseLong(properties.get(POLL_TIME));
        this.profilePattern = Pattern.compile(properties.get(PROFILE_PATTERN)).matcher("");
        this.containerPattern = Pattern.compile(properties.get(CONTAINER_PATTERN)).matcher("");
        this.containerPrefix = properties.get(CONTAINER_PREFIX);
        this.scaleContainers = Boolean.parseBoolean(properties.get(SCALE_CONTAINERS));
        this.defaultMaximumInstancesPerHost = Integer.parseInt(properties.get(DEFAULT_MAX_INSTANCES_PER_HOST));
        this.autoscalerGroupId = properties.get(AUTOSCALER_GROUP_ID);
        this.minContainerCount = Integer.parseInt(properties.get(MIN_CONTAINER_COUNT));
        this.maxDeviation = Double.parseDouble(properties.get(MAX_DEVIATION)) >= 0 ? Double.parseDouble(properties.get(MAX_DEVIATION)) : 1;
        this.inheritRequirements = Boolean.parseBoolean(properties.get(INHERIT_REQUIREMENTS));
        this.averageInstancesPerContainer = Integer.parseInt(properties.get(AVERAGE_INSTANCES_PER_CONTAINER));
        this.ignoreErrors = Boolean.parseBoolean(properties.get(IGNORE_ERRORS));
        this.maxContainersPerHost = Integer.parseInt(properties.get(MAX_CONTAINERS_PER_HOST));
        this.verbose = Boolean.parseBoolean(properties.get(VERBOSE));
        this.dryRun = Boolean.parseBoolean(properties.get(DRY_RUN));
        CuratorFramework curator = this.curator.get();
        enableMasterZkCache(curator);
        group = new ZooKeeperGroup<AutoScalerNode>(curator, ZkPath.AUTO_SCALE_CLUSTER.getPath() + "/" + autoscalerGroupId, AutoScalerNode.class);
        group.add(this);
        group.update(createState());
        group.start();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        disableMasterZkCache();
        disableTimer();
        deactivateComponent();
        group.remove(this);
        Closeables.closeQuietly(group);
        group = null;
    }

    @Override
    public void groupEvent(Group<AutoScalerNode> group, GroupEvent event) {
        DataStore dataStore = fabricService.get().adapt(DataStore.class);
        switch (event) {
            case CONNECTED:
            case CHANGED:
                if (isValid()) {
                    AutoScalerNode state = createState();
                    try {
                        if (group.isMaster()) {
                            enableMasterZkCache(curator.get());
                            LOGGER.info("AutoScaleController is the master");
                            group.update(state);
                            dataStore.trackConfiguration(runnable);
                            enableTimer();
                            onConfigurationChanged();
                        } else {
                            LOGGER.info("AutoScaleController is not the master");
                            group.update(state);
                            disableTimer();
                            dataStore.untrackConfiguration(runnable);
                            disableMasterZkCache();
                        }
                    } catch (IllegalStateException e) {
                        // Ignore
                    }
                } else {
                    LOGGER.info("Not valid with master: " + group.isMaster()
                            + " fabric: " + fabricService.get()
                            + " curator: " + curator.get());
                }
                break;
            case DISCONNECTED:
                dataStore.untrackConfiguration(runnable);
        }
    }


    protected void enableMasterZkCache(CuratorFramework curator) {
        zkMasterCache = new ZooKeeperMasterCache(curator);
    }

    protected void disableMasterZkCache() {
        if (zkMasterCache != null) {
            zkMasterCache = null;
        }
    }

    protected void enableTimer() {
        Timer newTimer = new Timer("fabric8-autoscaler");
        if (timer.compareAndSet(null, newTimer)) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    LOGGER.debug("autoscale timer");
                    autoScale();
                }
            };
            newTimer.schedule(timerTask, pollTime, pollTime);
        }
    }

    protected void disableTimer() {
        Timer oldValue = timer.getAndSet(null);
        if (oldValue != null) {
            oldValue.cancel();
        }
    }


    private void onConfigurationChanged() {
        LOGGER.debug("Configuration has changed; so checking the auto-scaling requirements");
        autoScale();
    }

    private void autoScale() {
        try {
            FabricService service = fabricService.getOptional();
            if (service == null) {
                throw new Exception("FabricService not available");
            }
            AutoScaledGroupOptions options = new AutoScaledGroupOptions(
                containerPattern,
                profilePattern,
                scaleContainers,
                inheritRequirements,
                maxDeviation,
                averageInstancesPerContainer,
                containerPrefix,
                minContainerCount,
                defaultMaximumInstancesPerHost,
                ignoreErrors,
                maxContainersPerHost,
                verbose,
                dryRun);
            List<ProfileRequirements> profileRequirements = service.getRequirements().getProfileRequirements();
            AutoScaledGroup autoScaledGroup = new AutoScaledGroup(
                autoscalerGroupId,
                options,
                service.getContainers(),
                profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]),
                new ContainerFactory(service));
            autoScaledGroup.apply();
        } catch (Exception e) {
            LOGGER.error("AutoScaledGroup {} canceled", autoscalerGroupId, e);
        }
    }

    private AutoScalerNode createState() {
        AutoScalerNode state = new AutoScalerNode();
        return state;
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
