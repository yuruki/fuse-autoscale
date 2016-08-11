package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.api.Profile;
import io.fabric8.api.Version;

class MockVersion implements Version {

    private final String id;
    private final Map<String, Profile> profileMap = new HashMap<>();

    MockVersion(String id) {
        this.id = id;
    }

    @Override
    public String getRevision() {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        return null;
    }

    void addProfile(Profile profile) {
        profileMap.put(profile.getId(), profile);
    }

    public void removeProfile(String profileId) {
        profileMap.remove(profileId);
    }

    @Override
    public List<String> getProfileIds() {
        return new ArrayList<>(profileMap.keySet());
    }

    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(profileMap.values());
    }

    @Override
    public Profile getProfile(String profileId) {
        return profileMap.get(profileId);
    }

    @Override
    public Profile getRequiredProfile(String profileId) {
        return null;
    }

    @Override
    public boolean hasProfile(String profileId) {
        return profileMap.containsKey(profileId);
    }

    @Override
    public int compareTo(Version version) {
        return 0;
    }

    @Override
    public String getId() {
        return id;
    }
}
