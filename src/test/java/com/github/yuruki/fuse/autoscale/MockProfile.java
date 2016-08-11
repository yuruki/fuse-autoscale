package com.github.yuruki.fuse.autoscale;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.api.Profile;

class MockProfile implements Profile {

    private final String id;

    MockProfile(String id) {
        this.id = id;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        return null;
    }

    @Override
    public List<String> getParentIds() {
        return null;
    }

    @Override
    public List<String> getLibraries() {
        return null;
    }

    @Override
    public List<String> getEndorsedLibraries() {
        return null;
    }

    @Override
    public List<String> getExtensionLibraries() {
        return null;
    }

    @Override
    public List<String> getBundles() {
        return null;
    }

    @Override
    public List<String> getFabs() {
        return null;
    }

    @Override
    public List<String> getFeatures() {
        return null;
    }

    @Override
    public List<String> getRepositories() {
        return null;
    }

    @Override
    public List<String> getOverrides() {
        return null;
    }

    @Override
    public List<String> getOptionals() {
        return null;
    }

    @Override
    public String getIconURL() {
        return null;
    }

    @Override
    public String getIconRelativePath() {
        return null;
    }

    @Override
    public String getSummaryMarkdown() {
        return null;
    }

    @Override
    public List<String> getTags() {
        return null;
    }

    @Override
    public Set<String> getConfigurationFileNames() {
        return null;
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        return null;
    }

    @Override
    public byte[] getFileConfiguration(String fileName) {
        return new byte[0];
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        return null;
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        return null;
    }

    @Override
    public boolean isOverlay() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String getProfileHash() {
        return null;
    }

    @Override
    public int compareTo(Profile profile) {
        return 0;
    }

    @Override
    public String getId() {
        return id;
    }
}
