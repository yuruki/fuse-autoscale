/**
 *  Copyright 2016 Jyrki Ruuskanen
 *
 *  Jyrki Ruuskanen licenses this file to you under the Apache License, version
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
 */
package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import io.fabric8.api.Container;
import io.fabric8.api.ProfileRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoScaledGroup extends ProfileContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaledGroup.class);

    private final AutoScaledGroupOptions options;
    private final ContainerFactory containerFactory;

    private Integer profileInstances;
    private Integer requiredHosts;
    private Map<String, ProfileRequirements> profileRequirementsMap = new HashMap<>();
    private Long maxInstancesPerContainer;

    public AutoScaledGroup(final String groupId, final AutoScaledGroupOptions options, final Container[] containers, final ProfileRequirements[] profiles, ContainerFactory containerFactory) throws Exception {
        this.id = groupId;
        this.options = options;
        this.containerFactory = containerFactory;
        update(containers, profiles);
    }

    private void update(Container[] containers, ProfileRequirements[] profiles) throws Exception {
        processProfileRequirements(profiles);
        processContainers(containers);
        applyProfileRequirements();
    }

    private void processProfileRequirements(final ProfileRequirements[] profiles) {
        profileRequirementsMap.clear(); // Reset profile requirements
        profileInstances = 0; // Reset total number of required profile instances
        requiredHosts = 0; // Reset number of hosts needed to satisfy the requirements

        for (ProfileRequirements profile : pruneProfileRequirements(profiles, options.getProfilePattern(), options.getInheritRequirements())) {
            if (profile.getMaximumInstancesPerHost() == null) {
                profile.setMaximumInstancesPerHost(options.getDefaultMaxInstancesPerHost());
            }
            profileRequirementsMap.put(profile.getProfile(), profile);
            if (profile.hasMinimumInstances() && profile.getMaximumInstancesPerHost() > 0) {
                requiredHosts = Math.max(requiredHosts, (profile.getMinimumInstances() + profile.getMaximumInstancesPerHost() - 1) / profile.getMaximumInstancesPerHost());
            }
            if (profile.hasMinimumInstances()) {
                profileInstances += profile.getMinimumInstances();
            } else {
                profileInstances++; // These are dependencies without minimum instances set
            }
        }
    }

    private void processContainers(Container[] containers) throws Exception {
        childMap.clear(); // Reset hosts

        // Collect root container hosts
        for (Container container : containers) {
            if (container.isRoot()) {
                addChild(new AutoScaledHost(container.getIp(), container));
            }
        }

        // Collect all applicable containers
        if (options.getScaleContainers()) {
            for (Container container : containers) {
                if (options.getContainerPattern().reset(container.getId()).matches()) {
                    AutoScaledContainer.newAutoScaledContainer(this, container, containerFactory);
                }
            }
            if (options.getAverageInstancesPerContainer() < 1) {
                throw new Exception("averageInstancesPerContainer < 1");
            }
            // Ceiling of profile instances per container
            int requiredContainers = 0;
            if (profileInstances > 0) {
                requiredContainers = (profileInstances + options.getAverageInstancesPerContainer() - 1) / options.getAverageInstancesPerContainer();
            }
            if (requiredContainers < requiredHosts) {
                requiredContainers = requiredHosts;
            }
            // Scale containers
            scaleContainers(requiredContainers - getGrandChildren().size(), requiredHosts - getChildren().size(), containerFactory);
        } else {
            for (Container container : containers) {
                if (options.getContainerPattern().reset(container.getId()).matches() && container.isAlive()) {
                    AutoScaledContainer.newAutoScaledContainer(this, container, containerFactory);
                }
            }
            if (getGrandChildren().size() < options.getMinContainerCount()) {
                throw new Exception("Not enough containers (" + getGrandChildren().size() + "), " + options.getMinContainerCount() + " required");
            }
        }
    }

    private void applyProfileRequirements() throws Exception {
        // Calculate and check max profile instances per container
        maxInstancesPerContainer = calculateMaxInstancesPerContainer(getGrandChildren().size(), profileInstances, options.getAverageInstancesPerContainer(), options.getMaxDeviation());
        adjustWithMaxInstancesPerContainer();

        // Apply collected profile requirements on the containers
        for (ProfileRequirements profile : profileRequirementsMap.values()) {
            addProfile(profile);
        }
    }

    private void scaleContainers(int containerDelta, int hostDelta, ContainerFactory containerFactory) throws Exception {
        LOGGER.debug("Scaling containers with container delta: {}, host delta {}", containerDelta, hostDelta);
        if (containerDelta > 0) {
            // Add containers
            for (int i = 0; i < containerDelta; i++) {
                try {
                    String containerId = createContainerId();
                    AutoScaledContainer.newAutoScaledContainer(this, containerId, i < hostDelta, containerFactory);
                } catch (Exception e) {
                    if (options.getIgnoreErrors()) {
                        LOGGER.error("Failed to create new auto-scaled container. This exception is ignored", e);
                    } else {
                        throw e;
                    }
                }
            }
        } else if (containerDelta < 0) {
            // Remove containers
            nextRemoval: for (int i = containerDelta; i < 0; i++) {
                List<ProfileContainer> hosts = new LinkedList<>(getSortedChildren());
                Collections.reverse(hosts);
                for (ProfileContainer host : hosts) {
                    try {
                        host.removeChild(1);
                        continue nextRemoval;
                    } catch (Exception e) {
                        // ignored
                    }
                }
                break; // No more removable containers available
            }
        }
    }

    private String createContainerId() throws Exception {
        if (options.getContainerPattern().reset(options.getContainerPrefix()).matches()) {
            Set<String> containerNames = new HashSet<>();
            for (ProfileContainer container : getEveryGrandChild()) {
                containerNames.add(container.getId());
            }
            for (int i = 1; i <= containerNames.size() + 1; i++) {
                if (!containerNames.contains(options.getContainerPrefix() + i)) {
                    return options.getContainerPrefix() + i;
                }
            }
        } else {
            throw new Exception("Container prefix doesn't match the container pattern.");
        }
        throw new Exception("Couldn't determine new container ID. This should never happen.");
    }

    // Check the profile requirements against profile pattern and check the profile dependencies
    private static List<ProfileRequirements> pruneProfileRequirements(final ProfileRequirements[] profileRequirements, final Matcher profilePattern, final Boolean inheritRequirements) {
        Map<String, ProfileRequirements> profileRequirementsMap = new HashMap<>();
        for (ProfileRequirements p : profileRequirements) {
            profileRequirementsMap.put(p.getProfile(), p);
        }
        Map<String, ProfileRequirements> prunedProfileRequirements = new HashMap<>();
        for (ProfileRequirements p : profileRequirements) {
            if (p.hasMinimumInstances()) {
                // Skip root requirements without minimum instances
                pruneProfileRequirements(p, prunedProfileRequirements, profileRequirementsMap, profilePattern, inheritRequirements);
            }
        }
        return new ArrayList<>(prunedProfileRequirements.values());
    }

    private static Map<String, ProfileRequirements> pruneProfileRequirements(final ProfileRequirements parent, final Map<String, ProfileRequirements> prunedProfileRequirements, final Map<String, ProfileRequirements> profileRequirementsMap, final Matcher profilePattern, final Boolean inheritRequirements) {
        if (parent == null || !profilePattern.reset(parent.getProfile()).matches()) {
            // At the end or profile doesn't match the profile pattern
            return prunedProfileRequirements;
        }
        // Add this profile requirement to the result
        prunedProfileRequirements.put(parent.getProfile(), parent);
        if (parent.getDependentProfiles() == null || parent.getDependentProfiles().isEmpty()) {
            // Profile doesn't have dependencies
            return prunedProfileRequirements;
        }
        if (!parent.hasMinimumInstances()) {
            // Profile doesn't have instances, skip the dependencies
            return prunedProfileRequirements;
        }
        // Check the profile dependencies
        for (String profile : parent.getDependentProfiles()) {
            if (!profilePattern.reset(profile).matches()) {
                // Profile dependency doesn't match profile pattern
                LOGGER.error("Profile dependency {} for profile {} doesn't match profile pattern.", profile, parent.getProfile());
                continue;
            }
            ProfileRequirements dependency = profileRequirementsMap.get(profile);
            if (inheritRequirements) {
                if (dependency == null) {
                    // Requirements missing, inherit them from the parent
                    dependency = new ProfileRequirements(profile, parent.getMinimumInstances(), parent.getMaximumInstances());
                } else if (!dependency.hasMinimumInstances()) {
                    // No instances for the dependency, inherit them from the parent
                    dependency.setMinimumInstances(parent.getMinimumInstances());
                    if (dependency.getMaximumInstances() != null && dependency.getMaximumInstances() < dependency.getMinimumInstances()) {
                        dependency.setMaximumInstances(parent.getMaximumInstances());
                    }
                }
            } else {
                if (dependency == null) {
                    // Requirements missing.
                    LOGGER.error("Profile dependency {} for profile {} is missing requirements.", profile, parent.getProfile());
                    continue;
                } else if (!dependency.hasMinimumInstances()) {
                    // No instances for the dependency.
                    LOGGER.error("Profile dependency {} for profile {} has no instances.", profile, parent.getProfile());
                    continue;
                }
            }
            pruneProfileRequirements(dependency, prunedProfileRequirements, profileRequirementsMap, profilePattern, inheritRequirements);
        }
        return prunedProfileRequirements;
    }

    // Return the preferred max profile instance count for a single container
    private static long calculateMaxInstancesPerContainer(int containers, int profileInstances, int averageInstancesPerContainer, double maxDeviation) {
        long average = averageInstancesPerContainer;
        if (averageInstancesPerContainer < 0 && containers > 0) {
            average = (profileInstances + containers - 1) / containers; // Ceiling of average
        } else if (averageInstancesPerContainer < 0) {
            average = 0;
        }
        return average + (int)Math.round(Math.abs(maxDeviation) * average);
    }

    @Override
    public void addProfile(ProfileRequirements profile) throws Exception {
        adjustWithMaxInstancesPerHost(profile);
        adjustWithMaxInstancesPerGroup(profile);
        if (profile.hasMinimumInstances() && profile.getMinimumInstances() > getProfileCount(profile)) {
            int delta = profile.getMinimumInstances() - getProfileCount(profile);
            Exception exception = null;
            count: for (int i = 0; i < delta; i++) {
                for (ProfileContainer container : getSortedGrandChildren()) {
                    try {
                        container.addProfile(profile);
                        continue count;
                    } catch (Exception e) {
                        exception = e;
                    }
                }
                if (exception != null) {
                    if (options.getIgnoreErrors()) {
                        LOGGER.error("Couldn't satisfy requirements for profile {}. This exception is ignored.", profile.getProfile(), exception);
                    } else {
                        throw new Exception("Couldn't satisfy requirements for profile " + profile.getProfile(), exception);
                    }
                }
            }
        }
    }

    private void adjustWithMaxInstancesPerContainer() {
        for (ProfileContainer container : getGrandChildren()) {
            removeProfiles(container.getProfileCount() - maxInstancesPerContainer);
        }
    }

    private void adjustWithMaxInstancesPerHost(ProfileRequirements profile) {
        if (profile.getMaximumInstancesPerHost() != null) {
            for (ProfileContainer host : getChildren()) {
                int maxInstancesPerHost = profile.getMaximumInstancesPerHost();
                if (host.getProfileCount(profile) > maxInstancesPerHost) {
                    host.removeProfile(profile, host.getProfileCount(profile) - maxInstancesPerHost);
                }
            }
        }
    }

    private void adjustWithMaxInstancesPerGroup(ProfileRequirements profile) {
        if (profile.getMaximumInstances() != null) {
            int maxInstances = profile.getMaximumInstances();
            int delta = getProfileCount(profile) - maxInstances;
            if (delta > 0) {
                removeProfile(profile, delta);
            }
        }
    }

    @Override
    public void removeProfile(String profile, int count) {
        for (int i = 0; i < count; i++) {
            List<ProfileContainer> containers = getSortedGrandChildren();
            Collections.reverse(containers);
            for (ProfileContainer container : containers) {
                if (container.hasProfile(profile)) {
                    container.removeProfile(profile);
                    break;
                }
            }
        }
    }

    public long getMaxInstancesPerContainer() {
        return maxInstancesPerContainer;
    }

    public void apply() {
        apply(0);
    }

    public void apply(long maxWaitInMillis) {
        Set<ProfileContainer> containers = new HashSet<>(getEveryGrandChild());
        if (containers.isEmpty()) {
            LOGGER.debug("No changes to apply");
            return;
        }
        ExecutorService taskExecutor = Executors.newFixedThreadPool(containers.size());
        for (ProfileContainer container : containers) {
            taskExecutor.execute((AutoScaledContainer) container);
        }
        taskExecutor.shutdown();
        if (maxWaitInMillis > 0) {
            try {
                taskExecutor.awaitTermination(maxWaitInMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace(); // Ignored
            }
        }
    }

    public AutoScaledGroupOptions getOptions() {
        return options;
    }

    public boolean hasRequirements(String profileId) {
        return profileRequirementsMap.containsKey(profileId);
    }

    public boolean matchesProfilePattern(String profileId) {
        return options.getProfilePattern().reset(profileId).matches();
    }
}
