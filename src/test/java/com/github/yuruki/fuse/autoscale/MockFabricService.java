package com.github.yuruki.fuse.autoscale;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.api.AutoScaleStatus;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerBasicMetadata;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.FabricStatus;
import io.fabric8.api.PatchService;
import io.fabric8.api.PortService;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.Version;

class MockFabricService implements FabricService {
    @Override
    public <T> T adapt(Class<T> type) {
        return null;
    }

    @Override
    public String getEnvironment() {
        return null;
    }

    @Override
    public Map<String, Map<String, String>> substituteConfigurations(Map<String, Map<String, String>> configurations) {
        return null;
    }

    @Override
    public void trackConfiguration(Runnable callback) {

    }

    @Override
    public void untrackConfiguration(Runnable callback) {

    }

    @Override
    public Container[] getContainers() {
        return new Container[0];
    }

    @Override
    public Container[] getAssociatedContainers(String versionId, String profileId) {
        return new Container[0];
    }

    @Override
    public Container getContainer(String name) {
        return null;
    }

    @Override
    public void startContainer(String containerId) {

    }

    @Override
    public void startContainer(String containerId, boolean force) {

    }

    @Override
    public void startContainer(Container container) {

    }

    @Override
    public void startContainer(Container container, boolean force) {

    }

    @Override
    public void stopContainer(String containerId) {

    }

    @Override
    public void stopContainer(String containerId, boolean force) {

    }

    @Override
    public void stopContainer(Container container) {

    }

    @Override
    public void stopContainer(Container container, boolean force) {

    }

    @Override
    public void destroyContainer(String containerId) {

    }

    @Override
    public void destroyContainer(String containerId, boolean force) {

    }

    @Override
    public void destroyContainer(Container container) {

    }

    @Override
    public void destroyContainer(Container container, boolean force) {

    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options) {
        return new CreateContainerMetadata[0];
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options, CreationStateListener listener) {
        return new CreateContainerMetadata[0];
    }

    @Override
    public Set<Class<? extends CreateContainerBasicOptions>> getSupportedCreateContainerOptionTypes() {
        return null;
    }

    @Override
    public Set<Class<? extends CreateContainerBasicMetadata>> getSupportedCreateContainerMetadataTypes() {
        return null;
    }

    @Override
    public String getDefaultVersionId() {
        return null;
    }

    @Override
    public void setDefaultVersionId(String versionId) {

    }

    @Override
    public Version getDefaultVersion() {
        return null;
    }

    @Override
    public Version getRequiredDefaultVersion() {
        return null;
    }

    @Override
    public ContainerProvider getProvider(String scheme) {
        return null;
    }

    @Override
    public Map<String, ContainerProvider> getValidProviders() {
        return null;
    }

    @Override
    public Map<String, ContainerProvider> getProviders() {
        return null;
    }

    @Override
    public URI getMavenRepoURI() {
        return null;
    }

    @Override
    public List<URI> getMavenRepoURIs() {
        return null;
    }

    @Override
    public URI getMavenRepoUploadURI() {
        return null;
    }

    @Override
    public String getRestAPI() {
        return null;
    }

    @Override
    public String getGitUrl() {
        return null;
    }

    @Override
    public String getWebConsoleUrl() {
        return null;
    }

    @Override
    public String getZookeeperUrl() {
        return "tcp://localhost:2181";
    }

    @Override
    public String getZooKeeperUser() {
        return "admin";
    }

    @Override
    public String getZookeeperPassword() {
        return "password";
    }

    @Override
    public Container getCurrentContainer() {
        return null;
    }

    @Override
    public String getCurrentContainerName() {
        return null;
    }

    @Override
    public FabricRequirements getRequirements() {
        return null;
    }

    @Override
    public AutoScaleStatus getAutoScaleStatus() {
        return null;
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {

    }

    @Override
    public FabricStatus getFabricStatus() {
        return null;
    }

    @Override
    public PatchService getPatchService() {
        return null;
    }

    @Override
    public PortService getPortService() {
        return null;
    }

    @Override
    public String getDefaultJvmOptions() {
        return null;
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {

    }

    @Override
    public String containerWebAppURL(String webAppId, String name) {
        return null;
    }

    @Override
    public String profileWebAppURL(String webAppId, String profileId, String versionId) {
        return null;
    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        return null;
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {

    }

    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        return false;
    }

    @Override
    public ContainerAutoScaler createContainerAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        return null;
    }
}
