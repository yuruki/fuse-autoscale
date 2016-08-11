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
import java.util.UUID;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.common.util.Arrays;

class AutoScaledContainer extends ProfileContainer implements Runnable {

    private final Container container;
    private final Map<String, Boolean> profiles = new HashMap<>();
    private final AutoScaledGroup group;
    private final ContainerFactory containerFactory;

    private ProfileContainer host;

    private AutoScaledContainer(Container container, String id, AutoScaledGroup group, boolean newHost, ContainerFactory containerFactory) throws Exception {
        this.container = container;
        this.id = id;
        this.group = group;
        this.containerFactory = containerFactory;

        if (container != null) {
            // Existing container
            if (container.isRoot()) {
                removable = false; // Let's mark root containers non-removable
                setHost(container.getIp(), container);
            } else {
                setHost(container.getIp(), container.getParent());
            }
            LOGGER.debug("Included an existing container {} from host {}", id, host.getId());
        } else if (newHost) {
            // New container on a new host
            setHost(UUID.randomUUID().toString()); // Any unique value goes
            LOGGER.debug("Requested a new container {} on a new host {}", id, host.getId());
        } else {
            // New (child) container on an existing host
            ProfileContainer rootHost = null;
            for (ProfileContainer host : group.getSortedChildren()) {
                if (((AutoScaledHost) host).hasRootContainer()) {
                    rootHost = host;
                    break;
                }
            }
            if (rootHost != null) {
                setHost(rootHost);
                LOGGER.debug("Requested a new container {} on an existing host {}", id, host.getId());
            } else {
                throw new Exception("Can't add a child container. No applicable root containers available.");
            }
        }

        // Collect current profiles
        if (container != null && container.getProfileIds() != null) {
            for (String profileId : container.getProfileIds()) {
                if (group.hasRequirements(profileId)) {
                    profiles.put(profileId, true); // Profile with requirements. Marked as already assigned.
                } else if (group.matchesProfilePattern(profileId)) {
                    profiles.put(profileId, false); // Matched profile with no requirements. Marked as not assigned.
                } else if (!profileId.equals("default") && removable) {
                    removable = false; // Having any unmatched profiles on the container means we can't remove it
                }
            }
        }

        if (removable && group.getOptions().getMaxContainersPerHost() > 0 && host.getChildren().size() > group.getOptions().getMaxContainersPerHost()) {
            LOGGER.debug("MaxContainersPerHost ({}) exceeded on host {} with container {}. Marking container for removal", group.getOptions().getMaxContainersPerHost(), host.getId(), id);
            remove(); // Schedule the container to be removed
        }
    }

    static AutoScaledContainer createAutoScaledContainer(AutoScaledGroup group, Container container, ContainerFactory containerFactory) throws Exception {
        return new AutoScaledContainer(container, container.getId(), group, false, containerFactory);
    }

    static AutoScaledContainer createAutoScaledContainer(AutoScaledGroup group, String id, boolean newHost, ContainerFactory containerFactory) throws Exception {
        return new AutoScaledContainer(null, id, group, newHost, containerFactory);
    }

    private void setHost(ProfileContainer host) {
        this.host = host;
        host.addChild(this);
        group.addChild(host);
    }

    private void setHost(String hostId, Container rootContainer) {
        if (group.hasChild(hostId)) {
            setHost(group.getChild(hostId));
        } else {
            setHost(new AutoScaledHost(hostId, rootContainer));
        }
    }

    private void setHost(String hostId) {
        setHost(hostId, null);
    }

    @Override
    public int getProfileCount() {
        int count = 0;
        for (Boolean assigned : profiles.values()) {
            if (assigned) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void removeProfile(String profile) {
        profiles.put(profile, false);
    }

    @Override
    public void removeProfile(String profile, int count) {
        removeProfile(profile); // Ignore count
    }

    public void removeProfiles(long count) {
        Iterator<String> iterator = profiles.keySet().iterator();
        for (int i = 0; i < count && iterator.hasNext(); i++) {
            iterator.next();
            iterator.remove();
        }
    }

    @Override
    public boolean hasProfile(String profileId) {
        return profiles.containsKey(profileId) && profiles.get(profileId);
    }

    @Override
    public void addProfile(ProfileRequirements profile) throws Exception {
        if (removed){
            throw new Exception("Can't assign " + profile.getProfile() + " to container marked as removed (" + id + ").");
        } else if (getProfileCount() >= group.getMaxInstancesPerContainer()) {
            throw new Exception("Can't assign " + profile.getProfile() + " to container " + id + ", due to maxInstancesPerContainer (" + group.getMaxInstancesPerContainer() + ").");
        } else if (profile.getMaximumInstancesPerHost() != null && host.getProfileCount(profile) >= profile.getMaximumInstancesPerHost()) {
            throw new Exception("Can't assign " + profile.getProfile() + " to container " + id + ", due to maxInstancesPerHost (" + profile.getMaximumInstancesPerHost() + ").");
        } else if (profile.getMaximumInstances() != null && group.getProfileCount(profile) >= profile.getMaximumInstances()) {
            throw new Exception("Can't assign " + profile.getProfile() + " to container " + id + ", due to maxInstances (" + profile.getMaximumInstances() + ").");
        } else {
            profiles.put(profile.getProfile(), true);
        }
    }

    @Override
    public int getProfileCount(String profileId) {
        if (profiles.containsKey(profileId) && profiles.get(profileId)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void run() {
        if (container != null && removed) {
            // Remove container
            container.destroy(true);
            LOGGER.info("Container {} removed", id);
            return;
        }

        // Get current profiles for the container
        Set<String> currentProfiles = new HashSet<>();
        if (container != null && container.getProfileIds() != null) {
            for (String profileId : container.getProfileIds()) {
                currentProfiles.add(profileId);
            }
        }

        // Find the changes
        Set<String> resultSet = new HashSet<>(currentProfiles);
        Set<String> additions = new HashSet<>();
        Set<String> removals = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : profiles.entrySet()) {
            final String profile = entry.getKey();
            if (entry.getValue()) {
                resultSet.add(profile);
                if (!currentProfiles.contains(profile)) {
                    additions.add(profile);
                }
            } else {
                resultSet.remove(profile);
                if (currentProfiles.contains(profile)) {
                    removals.add(profile);
                }
            }
        }

        // Apply possible changes
        if (!resultSet.equals(currentProfiles) || (container != null && !container.isAlive() && getProfileCount() > 0)) {
            List<String> sortedResult = new LinkedList<>(resultSet);
            Collections.sort(sortedResult);
            if (container != null) {
                List<Profile> profiles = new ArrayList<>();
                for (String profileId : sortedResult) {
                    Profile profile = container.getVersion().getProfile(profileId);
                    if (profile != null) {
                        profiles.add(container.getVersion().getProfile(profileId));
                    } else {
                        LOGGER.error("Profile {} doesn't exist in version {}, can't assign to container {}. This exception is ignored.", profileId, container.getVersionId(), id);
                    }
                }
                // Update existing container
                if (group.getOptions().isDryRun()) {
                    LOGGER.info("Would have updated container {}: added: {} removed: {}", id, Arrays.join(", ", additions), Arrays.join(", ", removals));
                } else {
                    LOGGER.info("Updating container {}: added: {} removed: {}", id, Arrays.join(", ", additions), Arrays.join(", ", removals));
                    container.setProfiles(profiles.toArray(new Profile[profiles.size()]));
                    if (!container.isAlive() && getProfileCount() > 0) { // Only auto-start containers with managed profiles
                        LOGGER.info("Starting container {}", id);
                        container.start();
                    }
                }
            } else {
                if (group.getOptions().isDryRun()) {
                    LOGGER.info("Would have created container {} with profiles: {}", id, Arrays.join(", ", sortedResult));
                } else {
                    // TODO: generalize for any provider
                    // Create container
                    try {
                        containerFactory.createChildContainer(id, sortedResult.toArray(new String[sortedResult.size()]), ((AutoScaledHost) host).getRootContainer());
                        LOGGER.info("Created container {} with profiles: {}", id, Arrays.join(", ", sortedResult));
                    } catch (Exception e) {
                        LOGGER.error("Couldn't create child container {} with profiles: {}. This exception is ignored.", id, Arrays.join(", ", sortedResult), e);
                    }
                }
            }
        }
    }
}
