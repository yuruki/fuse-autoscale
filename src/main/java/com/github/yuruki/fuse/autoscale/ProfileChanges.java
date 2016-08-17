package com.github.yuruki.fuse.autoscale;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ProfileChanges {

    private final List<String> resultProfiles;
    private final List<String> addedProfiles;
    private final List<String> removedProfiles;

    private ProfileChanges(List<String> resultProfiles, List<String> addedProfiles, List<String> removedProfiles) {
        this.resultProfiles = resultProfiles;
        this.addedProfiles = addedProfiles;
        this.removedProfiles = removedProfiles;
    }

    static ProfileChanges getProfileChanges(AutoScaledContainer autoScaledContainer) {
        Set<String> resultProfiles = new HashSet<>();
        Set<String> addedProfiles = new HashSet<>();
        Set<String> removedProfiles = new HashSet<>();

        // Get current profiles for the container
        if (autoScaledContainer.getContainer() != null && autoScaledContainer.getContainer().getProfileIds() != null) {
            resultProfiles.addAll(autoScaledContainer.getContainer().getProfileIds());
        }

        // Find the changes
        for (Map.Entry<String, Boolean> entry : autoScaledContainer.getProfileMap().entrySet()) {
            final String profile = entry.getKey();
            if (entry.getValue()) {
                if (!resultProfiles.contains(profile)) {
                    addedProfiles.add(profile);
                }
                resultProfiles.add(profile);
            } else {
                if (resultProfiles.contains(profile)) {
                    removedProfiles.add(profile);
                }
                resultProfiles.remove(profile);
            }
        }

        // Sort results
        List<String> sortedResultProfiles = new LinkedList<>(resultProfiles);
        Collections.sort(sortedResultProfiles);
        List<String> sortedAddedProfiles = new LinkedList<>(addedProfiles);
        Collections.sort(sortedAddedProfiles);
        List<String> sortedRemovedProfiles = new LinkedList<>(removedProfiles);
        Collections.sort(sortedRemovedProfiles);

        return new ProfileChanges(sortedResultProfiles, sortedAddedProfiles, sortedRemovedProfiles);
    }

    List<String> getResultProfiles() {
        return resultProfiles;
    }

    List<String> getAddedProfiles() {
        return addedProfiles;
    }

    List<String> getRemovedProfiles() {
        return removedProfiles;
    }

    int getProfileChangeCount() {
        return addedProfiles.size() + removedProfiles.size();
    }
}
