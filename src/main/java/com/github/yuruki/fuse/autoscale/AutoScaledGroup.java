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

    private final Map<String, ProfileRequirements> profileRequirementsMap = new HashMap<>();
    private final AutoScaledGroupOptions options;
    private final Long maxAssignmentsPerContainer;

    public AutoScaledGroup(final String groupId, final AutoScaledGroupOptions options, final Container[] containers, final ProfileRequirements[] profileRequirements, ContainerFactory containerFactory) throws Exception {
        this.id = groupId;
        this.options = options;

        // Collect all applicable profile requirements
        int profileInstancesTotal = 0; // Total number of required profile instances
        int requiredHosts = 0; // Number of hosts needed to satisfy the requirements
        for (ProfileRequirements profile : pruneProfileRequirements(profileRequirements, options.getProfilePattern(), options.getInheritRequirements())) {
            if (profile.getMaximumInstancesPerHost() == null) {
                profile.setMaximumInstancesPerHost(options.getDefaultMaximumInstancesPerHost());
            }
            profileRequirementsMap.put(profile.getProfile(), profile);
            if (profile.hasMinimumInstances() && profile.getMaximumInstancesPerHost() > 0) {
                requiredHosts = Math.max(requiredHosts, (profile.getMinimumInstances() + profile.getMaximumInstancesPerHost() - 1) / profile.getMaximumInstancesPerHost());
            }
            if (profile.hasMinimumInstances()) {
                profileInstancesTotal += profile.getMinimumInstances();
            } else {
                profileInstancesTotal++; // These are dependencies without minimum instances set
            }
        }

        // Collect root container hosts
        for (Container container : containers) {
            if (container.isRoot()) {
                addChild(new AutoScaledHost(container.getIp(), container));
            }
        }

        // Collect all applicable containers
        if (options.getScaleContainers()) {
            Set<String> collectedHosts = new HashSet<>();
            for (Container container : containers) {
                if (options.getContainerPattern().reset(container.getId()).matches()) {
                    AutoScaledContainer.newAutoScaledContainer(this, container);
                    collectedHosts.add(container.getIp());
                }
            }
            // Scale containers
            if (options.getAverageAssignmentsPerContainer() < 1) {
                throw new Exception("averageAssignmentsPerContainer < 1");
            }
            int requiredContainers = (profileInstancesTotal + options.getAverageAssignmentsPerContainer() - 1) / options.getAverageAssignmentsPerContainer(); // Ceiling
            if (requiredContainers < requiredHosts) {
                requiredContainers = requiredHosts;
            }
            adjustContainerCount(requiredContainers - getGrandChildren().size(), requiredHosts - collectedHosts.size(), containerFactory);
        } else {
            for (Container container : containers) {
                if (options.getContainerPattern().reset(container.getId()).matches() && container.isAlive()) {
                    AutoScaledContainer.newAutoScaledContainer(this, container);
                }
            }
            if (getGrandChildren().size() < options.getMinContainerCount()) {
                throw new Exception("Not enough containers (" + getGrandChildren().size() + "), " + options.getMinContainerCount() + " required");
            }
        }

        // Calculate max profile instances per container
        this.maxAssignmentsPerContainer = calculateMaxAssignmentsPerContainer(getGrandChildren().size(), profileInstancesTotal, options.getAverageAssignmentsPerContainer(), options.getMaxDeviation());

        // Apply profile requirements on the containers
        applyProfileRequirements();
    }

    private void adjustContainerCount(int containerDelta, int hostDelta, ContainerFactory containerFactory) throws Exception {
        LOGGER.debug("Adjusting container count with container delta: {}, host delta {}", containerDelta, hostDelta);
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

    private void applyProfileRequirements() throws Exception {
        adjustWithMaxInstancesPerContainer();
        adjustWithMaxInstancesPerHost();
        adjustWithMaxInstances();
        adjustWithMinInstances();
    }

    private void adjustWithMaxInstancesPerContainer() {
        for (ProfileContainer container : getGrandChildren()) {
            long delta = container.getProfileCount() - maxAssignmentsPerContainer;
            container.removeProfiles(delta);
        }
    }

    private void adjustWithMaxInstancesPerHost() {
        for (ProfileRequirements profile : profileRequirementsMap.values()) {
            int maxInstancesPerHost = profile.getMaximumInstancesPerHost();
            for (ProfileContainer host : getChildren()) {
                if (host.getProfileCount(profile) > maxInstancesPerHost) {
                    host.removeProfile(profile, host.getProfileCount(profile) - maxInstancesPerHost);
                }
            }
        }
    }

    private void adjustWithMaxInstances() {
        for (ProfileRequirements profile : profileRequirementsMap.values()) {
            if (profile.getMaximumInstances() != null) {
                int maxInstances = profile.getMaximumInstances();
                int delta = getProfileCount(profile) - maxInstances;
                if (delta > 0) {
                    removeProfile(profile, delta);
                }
            }
        }
    }

    private void adjustWithMinInstances() throws Exception {
        for (ProfileRequirements profile : profileRequirementsMap.values()) {
            if (profile.hasMinimumInstances()) {
                int delta = profile.getMinimumInstances() - getProfileCount(profile);
                if (delta > 0) {
                    try {
                        addProfile(profile, delta);
                    } catch (Exception e) {
                        LOGGER.error("Couldn't assign {} instances for profile {}", delta, profile.getProfile(), e);
                        if (!options.getIgnoreErrors()) {
                            throw e;
                        }
                    }
                }
            }
        }
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
        if (parent.getDependentProfiles() == null) {
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

    // Return the preferred maximum profile assignment count for a single container
    private static long calculateMaxAssignmentsPerContainer(int containers, int profileInstances, int desiredAveragePerContainer, double maxDeviation) {
        long average = desiredAveragePerContainer;
        if (desiredAveragePerContainer < 0 && containers > 0) {
            average = (profileInstances + containers - 1) / containers; // Ceiling of average
        } else if (desiredAveragePerContainer < 0) {
            average = 0;
        }
        return average + (int)Math.round(Math.abs(maxDeviation) * average);
    }

    @Override
    public void addProfile(ProfileRequirements profile, int count) throws Exception {
        Exception exception = null;
        count: for (int i = 0; i < count; i++) {
            for (ProfileContainer container : getSortedGrandChildren()) {
                try {
                    container.addProfile(profile);
                    continue count;
                } catch (Exception e) {
                    exception = e;
                }
            }
            if (exception != null) {
                throw new Exception("Couldn't add profile " + profile.getProfile() + " to group " + id, exception);
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

    public Matcher getProfilePattern() {
        return options.getProfilePattern();
    }

    public long getMaxAssignmentsPerContainer() {
        return maxAssignmentsPerContainer;
    }

    public void apply() {
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
    }

    public void apply(long maxWaitInMillis) {
        Set<ProfileContainer> containers = new HashSet<>(getEveryGrandChild());
        ExecutorService taskExecutor = Executors.newFixedThreadPool(containers.size());
        for (ProfileContainer container : containers) {
            taskExecutor.execute((AutoScaledContainer) container);
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(maxWaitInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(); // ignored
        }
    }

    public AutoScaledGroupOptions getOptions() {
        return options;
    }
}
