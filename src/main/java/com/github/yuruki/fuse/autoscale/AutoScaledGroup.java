/*
   Copyright 2016 Jyrki Ruuskanen

   Jyrki Ruuskanen licenses this file to you under the Apache License, version
   2.0 (the "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   implied.  See the License for the specific language governing
   permissions and limitations under the License.
 */
package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

class AutoScaledGroup extends ProfileContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaledGroup.class);

    private final AutoScaledGroupOptions options;
    private final ContainerFactory containerFactory;

    private Map<String, ProfileRequirements> profileRequirementsMap = new HashMap<>();
    private Map<String, ProfileRequirements> prunedProfileRequirementsMap = new HashMap<>();
    private int profileInstances;
    private int requiredHosts;
    private Long maxInstancesPerContainer;

    AutoScaledGroup(final String groupId, final AutoScaledGroupOptions options, final Container[] containers, final ProfileRequirements[] profiles, ContainerFactory containerFactory) throws Exception {
        this.id = groupId;
        this.options = options;
        this.containerFactory = containerFactory;
        updateGroup(profiles, containers);
        applyProfileRequirements();
    }

    private void updateGroup(ProfileRequirements[] profiles, Container[] containers) throws Exception {
        setProfileRequirements(profiles);
        setContainers(containers);
        scaleContainers(profileInstances, requiredHosts, options.getAverageInstancesPerContainer());
    }

    private void updateGroup(Container[] containers) throws Exception {
        setContainers(containers);
        scaleContainers(profileInstances, requiredHosts, options.getAverageInstancesPerContainer());
    }

    private void updateGroup(ProfileRequirements[] profiles) throws Exception {
        setProfileRequirements(profiles);
        scaleContainers(profileInstances, requiredHosts, options.getAverageInstancesPerContainer());
    }

    private static ProfileRequirementsProcessingResult processProfileRequirements(final AutoScaledGroupOptions options, final ProfileRequirements... profiles) {
        Map<String, ProfileRequirements> prunedProfileRequirementsMap = new HashMap<>();
        int profileInstances = 0;
        int requiredHosts = 0;

        for (ProfileRequirements profile : pruneProfileRequirements(options.getProfilePattern(), options.isInheritRequirements(), profiles)) {
            if (profile.getMaximumInstancesPerHost() == null) {
                profile.setMaximumInstancesPerHost(options.getDefaultMaxInstancesPerHost());
            }
            prunedProfileRequirementsMap.put(profile.getProfile(), profile);
            if (profile.hasMinimumInstances() && profile.getMaximumInstancesPerHost() > 0) {
                requiredHosts = Math.max(requiredHosts, (profile.getMinimumInstances() + profile.getMaximumInstancesPerHost() - 1) / profile.getMaximumInstancesPerHost());
            }
            if (profile.hasMinimumInstances()) {
                profileInstances += profile.getMinimumInstances();
            } else {
                profileInstances++; // These are dependencies without minimum instances set
            }
        }
        return new ProfileRequirementsProcessingResult(prunedProfileRequirementsMap, profileInstances, requiredHosts);
    }

    private void processContainers(final AutoScaledGroupOptions options, final Container... containers) throws Exception {
        // Collect all applicable containers
        for (Container container : containers) {
            Container rootContainer = container.isRoot() ? container : container.getParent();
            if (matchesRootContainerPattern(rootContainer.getId())) {
                if (!hasChild(rootContainer.getIp())) {
                    addChild(new AutoScaledHost(rootContainer.getIp(), rootContainer));
                }
                if (options.isScaleContainers() && matchesContainerPattern(container.getId())
                    || !options.isScaleContainers() && matchesContainerPattern(container.getId()) && container.isAlive()) {
                    AutoScaledContainer.createAutoScaledContainer(this, container, containerFactory);
                }
            }
        }
        if (!options.isScaleContainers() && getGrandChildren().size() < options.getMinContainerCount()) {
            throw new Exception("Not enough containers available (" + getGrandChildren().size() + "), " + options.getMinContainerCount() + " required");
        }
    }

    private boolean matchesContainerPattern(String containerId) {
        return options.getContainerPattern().reset(containerId).matches();
    }

    private static int calculateRequiredContainers(int profileInstances, int requiredHosts, int desiredAverageInstancesPerContainer) {
        int requiredContainers = 0;
        if (profileInstances > 0 && desiredAverageInstancesPerContainer > 0) {
            requiredContainers = (profileInstances + desiredAverageInstancesPerContainer - 1) / desiredAverageInstancesPerContainer;
        }
        if (requiredContainers < requiredHosts) {
            requiredContainers = requiredHosts;
        }
        return requiredContainers;
    }

    private void applyProfileRequirements() throws Exception {
        maxInstancesPerContainer = calculateMaxInstancesPerContainer(getGrandChildren().size(), profileInstances, options.getAverageInstancesPerContainer(), options.getMaxDeviation());
        adjustWithMaxInstancesPerContainer();
        for (ProfileRequirements profile : prunedProfileRequirementsMap.values()) {
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
                        if (options.isIgnoreErrors()) {
                            LOGGER.error("Couldn't satisfy requirements for profile {}. This exception is ignored.", profile.getProfile(), exception);
                        } else {
                            throw new Exception("Couldn't satisfy requirements for profile " + profile.getProfile(), exception);
                        }
                    }
                }
            }
        }
    }

    private void scaleContainers(int profileInstances, int requiredHosts, int desiredAverageInstancesPerContainer) throws Exception {
        if (!options.isScaleContainers()) {
            return;
        }
        if (options.getAverageInstancesPerContainer() < 1) {
            throw new Exception("averageInstancesPerContainer < 1");
        }
        int requiredContainers = calculateRequiredContainers(profileInstances, requiredHosts, desiredAverageInstancesPerContainer);
        int containerDelta = requiredContainers - getGrandChildren().size();
        int hostDelta = requiredHosts - getChildren().size();
        LOGGER.debug("Scaling containers with container delta: {}, host delta {}", containerDelta, hostDelta);
        if (containerDelta > 0) {
            // Add containers
            for (int i = 0; i < containerDelta; i++) {
                try {
                    String containerId = createContainerId();
                    AutoScaledContainer.createAutoScaledContainer(this, containerId, i < hostDelta, containerFactory);
                } catch (Exception e) {
                    if (options.isIgnoreErrors()) {
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
        if (matchesContainerPattern(options.getContainerPrefix())) {
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
    private static List<ProfileRequirements> pruneProfileRequirements(final Matcher profilePattern, final Boolean inheritRequirements, final ProfileRequirements... profileRequirements) {
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
        profileRequirementsMap.put(profile.getProfile(), profile);
        updateGroup(profileRequirementsMap.values().toArray(new ProfileRequirements[profileRequirementsMap.size()]));
        applyProfileRequirements();
    }

    private void adjustWithMaxInstancesPerContainer() {
        for (ProfileContainer container : getGrandChildren()) {
            removeProfiles(container.getProfileCount() - maxInstancesPerContainer);
        }
    }

    private void adjustWithMaxInstancesPerHost(ProfileRequirements profile) throws Exception {
        if (profile.getMaximumInstancesPerHost() != null) {
            for (ProfileContainer host : getChildren()) {
                int maxInstancesPerHost = profile.getMaximumInstancesPerHost();
                if (host.getProfileCount(profile) > maxInstancesPerHost) {
                    host.removeProfile(profile, host.getProfileCount(profile) - maxInstancesPerHost);
                }
            }
        }
    }

    private void adjustWithMaxInstancesPerGroup(ProfileRequirements profile) throws Exception {
        if (profile.getMaximumInstances() != null) {
            int maxInstances = profile.getMaximumInstances();
            int delta = getProfileCount(profile) - maxInstances;
            if (delta > 0) {
                for (int i = 0; i < delta; i++) {
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
        }
    }

    @Override
    public void removeProfile(String profile, int count) throws Exception {
        if (profileRequirementsMap.containsKey(profile)
            && profileRequirementsMap.get(profile).hasMinimumInstances()
            && profileRequirementsMap.get(profile).getMinimumInstances() > count) {
            addProfile(new ProfileRequirements(profile, profileRequirementsMap.get(profile).getMinimumInstances() - count));
        } else {
            removeProfile(profile);
        }
    }

    @Override
    public void removeProfile(String profile) throws Exception {
        profileRequirementsMap.remove(profile);
        updateGroup(profileRequirementsMap.values().toArray(new ProfileRequirements[profileRequirementsMap.size()]));
        applyProfileRequirements();
    }

    long getMaxInstancesPerContainer() {
        return maxInstancesPerContainer;
    }

    void apply() {
        apply(0);
    }

    void apply(long maxWaitInMillis) {
        List<ProfileContainer> containers = getEveryGrandChildWithChanges();
        if (containers.isEmpty()) {
            LOGGER.debug("No changes to apply");
            return;
        } else {
            LOGGER.info("{} container(s) have pending changes", containers.size());
        }
        int maxContainerCount = options.getChangesPerPoll() > 0 && options.getChangesPerPoll() < containers.size() ? options.getChangesPerPoll() : containers.size();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(maxContainerCount);
        Iterator<ProfileContainer> iterator = containers.iterator();
        int containerCount = 0;
        for (; containerCount < maxContainerCount && iterator.hasNext(); containerCount++) {
            taskExecutor.execute((AutoScaledContainer) iterator.next());
        }
        taskExecutor.shutdown();
        if (maxWaitInMillis > 0) {
            try {
                taskExecutor.awaitTermination(maxWaitInMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace(); // Ignored
            }
        }
        LOGGER.info("Applied changes to {} container(s). {} container(s) remaining.", containerCount, containers.size() - containerCount);
    }

    private List<ProfileContainer> getEveryGrandChildWithChanges() {
        List<ProfileContainer> containersWithChanges = new ArrayList<>();
        for (ProfileContainer container : getEveryGrandChild()) {
            if (((AutoScaledContainer) container).hasChanges()) {
                containersWithChanges.add(container);
            }
        }
        return containersWithChanges;
    }

    AutoScaledGroupOptions getOptions() {
        return options;
    }

    boolean hasRequirements(String profileId) {
        return prunedProfileRequirementsMap.containsKey(profileId);
    }

    boolean matchesProfilePattern(String profileId) {
        return options.getProfilePattern().reset(profileId).matches();
    }

    private boolean matchesRootContainerPattern(String containerId) {
        return options.getRootContainerPattern().reset(containerId).matches();
    }

    private static final class ProfileRequirementsProcessingResult {
        final Map<String, ProfileRequirements> profileRequirementsMap;
        final int profileInstances;
        final int requiredHosts;

        ProfileRequirementsProcessingResult(Map<String, ProfileRequirements> profileRequirementsMap, int profileInstances, int requiredHosts) {
            this.profileRequirementsMap = new HashMap<>(profileRequirementsMap);
            this.profileInstances = profileInstances;
            this.requiredHosts = requiredHosts;
        }
    }

    private void setProfileRequirements(ProfileRequirements[] profileRequirements) {
        Map<String, ProfileRequirements> newProfileRequirementsMap = new HashMap<>();
        for (ProfileRequirements profile : profileRequirements) {
            newProfileRequirementsMap.put(profile.getProfile(), profile);
        }
        profileRequirementsMap = newProfileRequirementsMap;
        ProfileRequirementsProcessingResult result = processProfileRequirements(options, profileRequirements);
        prunedProfileRequirementsMap = result.profileRequirementsMap;
        profileInstances = result.profileInstances;
        requiredHosts = result.requiredHosts;
    }

    private void setContainers(Container[] containers) throws Exception {
        childMap.clear(); // Reset hosts
        processContainers(options, containers);
    }
}
