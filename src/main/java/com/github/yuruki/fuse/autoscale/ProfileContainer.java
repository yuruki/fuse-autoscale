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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ProfileContainer {

    final Logger LOGGER = LoggerFactory.getLogger(getClass());
    final Map<String, ProfileContainer> childMap = new HashMap<>();

    protected String id = "default";
    Boolean removable = true;
    Boolean removed = false;
    Comparator<ProfileContainer> childComparator = new SortByContainerCount();

    final boolean hasChild(String id) {
        return childMap.containsKey(id);
    }

    final ProfileContainer getChild(String id) {
        return childMap.get(id);
    }

    final boolean hasProfile(Profile profile) {
        return hasProfile(profile.getId());
    }

    final boolean hasProfile(ProfileRequirements profile) {
        return hasProfile(profile.getProfile());
    }

    final void addChild(ProfileContainer child) {
        childMap.put(child.getId(), child);
    }

    public boolean hasProfile(String profileId) {
        for (ProfileContainer child : getChildren()) {
            if (child.hasProfile(profileId)) {
                return true;
            }
        }
        return false;
    }

    public void addProfile(ProfileRequirements profile) throws Exception {
        if (profile.hasMinimumInstances()) {
            int count = profile.getMinimumInstances();
            for (int i = 0; i < count; i++) {
                Exception exception = null;
                for (ProfileContainer child : getSortedChildren()) {
                    try {
                        child.addProfile(profile);
                        break;
                    } catch (Exception e) {
                        exception = e;
                    }
                }
                if (exception != null) {
                    throw new Exception("Couldn't add profile " + profile.getProfile() + " to " + id, exception);
                }
            }
        }
    }

    final void removeProfile(Profile profile) throws Exception {
        removeProfile(profile.getId());
    }

    final void removeProfile(ProfileRequirements profile) throws Exception {
        removeProfile(profile.getProfile());
    }

    public void removeProfile(String profile) throws Exception {
        for (ProfileContainer child : getChildren()) {
            if (child.hasProfile(profile)) {
                child.removeProfile(profile);
            }
        }
    }

    final void removeProfile(ProfileRequirements profile, int count) throws Exception {
        removeProfile(profile.getProfile(), count);
    }

    public void removeProfile(String profile, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            List<ProfileContainer> children = new LinkedList<>(getSortedChildren());
            Collections.reverse(children);
            for (ProfileContainer child : children) {
                if (child.hasProfile(profile)) {
                    child.removeProfile(profile);
                    break;
                }
            }
        }
    }

    int getProfileCount() {
        int count = 0;
        for (ProfileContainer child : getChildren()) {
            count += child.getProfileCount();
        }
        return count;
    }

    final int getProfileCount(Profile profile) {
        return getProfileCount(profile.getId());
    }

    final int getProfileCount(ProfileRequirements profile) {
        return getProfileCount(profile.getProfile());
    }

    public int getProfileCount(String profileId) {
        int count = 0;
        for (ProfileContainer child : getChildren()) {
            count += child.getProfileCount(profileId);
        }
        return count;
    }

    final public String getId() {
        return id;
    }

    void remove() {
        removed = true;
    }

    private boolean isRemovable() {
        return removable && !removed;
    }

    private boolean isRemoved() {
        return removed;
    }

    final List<ProfileContainer> getChildren() {
        List<ProfileContainer> result = new ArrayList<>();
        for (ProfileContainer child : childMap.values()) {
            if (!child.isRemoved()) {
                result.add(child);
            }
        }
        return result;
    }

    final List<ProfileContainer> getEveryChild() {
        return new ArrayList<>(childMap.values());
    }

    final List<ProfileContainer> getSortedChildren() {
        List<ProfileContainer> result = new LinkedList<>(getChildren());
        Collections.sort(result, childComparator);
        return result;
    }

    final List<ProfileContainer> getRemovableChildren() {
        List<ProfileContainer> result = new ArrayList<>();
        for (ProfileContainer child : getChildren()) {
            if (child.isRemovable()) {
                result.add(child);
            }
        }
        return result;
    }

    final void removeChild(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            List<ProfileContainer> removables = new LinkedList<>(getRemovableChildren());
            if (!removables.isEmpty()) {
                Collections.sort(removables, childComparator);
                removables.get(0).remove();
                LOGGER.debug("Marked container {} for removal", removables.get(0).getId());
            } else {
                throw new Exception("No more removable children available for " + id + " (removal of " + (count - i) + " requested");
            }
        }
    }

    public void removeProfiles(long count) {
        for (int i = 0; i < count; i++) {
            List<ProfileContainer> children = new LinkedList<>(getSortedChildren());
            children.get(children.size() - 1).removeProfiles(1); // Remove from last
        }
    }

    final List<ProfileContainer> getGrandChildren() {
        List<ProfileContainer> result = new ArrayList<>();
        for (ProfileContainer child : getChildren()) {
            result.addAll(child.getChildren());
        }
        return result;
    }

    final List<ProfileContainer> getSortedGrandChildren() {
        List<ProfileContainer> result = new LinkedList<>();
        Comparator<ProfileContainer> grandChildComparator = null;
        for (ProfileContainer child : getChildren()) {
            if (grandChildComparator == null) {
                grandChildComparator = child.childComparator;
            }
            List<ProfileContainer> grandChildren = new ArrayList<>(child.getChildren());
            result.addAll(grandChildren);
        }
        Collections.sort(result, grandChildComparator);
        return result;
    }

    final List<ProfileContainer> getEveryGrandChild() {
        List<ProfileContainer> result = new ArrayList<>();
        for (ProfileContainer child : getChildren()) {
            result.addAll(child.getEveryChild());
        }
        return result;
    }

    static class SortByProfileCount implements Comparator<ProfileContainer> {
        @Override
        public int compare(ProfileContainer container, ProfileContainer t1) {
            return container.getProfileCount() - t1.getProfileCount();
        }
    }

    static class SortByContainerCount implements Comparator<ProfileContainer> {
        @Override
        public int compare(ProfileContainer container, ProfileContainer t1) {
            return container.getChildren().size() - t1.getChildren().size();
        }
    }
}
