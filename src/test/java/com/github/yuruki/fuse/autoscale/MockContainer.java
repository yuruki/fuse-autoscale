package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

public class MockContainer implements Container {

    private static AtomicInteger count = new AtomicInteger(0);

    private final String id;

    private List<Profile> profileList = new ArrayList<>();
    private Boolean alive;
    private String ipAddress;
    private Boolean root;
    private String versionId;
    private Version version;
    private Boolean destroyed;

    public MockContainer(String id, boolean alive, String ipAddress, boolean root) {
        this.id = id;
        this.alive = alive;
        this.ipAddress = ipAddress;
        this.root = root;
    }

    public MockContainer(String id, boolean alive, String ipAddress) {
        this(id, alive, ipAddress, false);
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public void setType(String type) {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Container getParent() {
        return null;
    }

    @Override
    public FabricService getFabricService() {
        return null;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void setAlive(boolean flag) {
        this.alive = flag;
    }

    @Override
    public boolean isEnsembleServer() {
        return false;
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public String getSshUrl() {
        return null;
    }

    @Override
    public String getJmxUrl() {
        return null;
    }

    @Override
    public String getHttpUrl() {
        return null;
    }

    @Override
    public String getJolokiaUrl() {
        return null;
    }

    @Override
    public void setJolokiaUrl(String location) {

    }

    @Override
    public String getDebugPort() {
        return null;
    }

    @Override
    public void setHttpUrl(String location) {

    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    public String getVersionId() {
        return versionId;
    }

    @Override
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public Long getProcessId() {
        return null;
    }

    @Override
    public Profile[] getProfiles() {
        return profileList.toArray(new Profile[profileList.size()]);
    }

    @Override
    public List<String> getProfileIds() {
        return null;
    }

    @Override
    public void setProfiles(Profile[] profiles) {
        this.profileList = new ArrayList<>(Arrays.asList(profiles));
    }

    @Override
    public void addProfiles(Profile... profiles) {
        profileList.addAll(Arrays.asList(profiles));
    }

    @Override
    public void removeProfiles(String... profileIds) {

    }

    @Override
    public Profile getOverlayProfile() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public void setLocation(String location) {

    }

    @Override
    public String getGeoLocation() {
        return null;
    }

    @Override
    public void setGeoLocation(String geoLocation) {

    }

    @Override
    public String getResolver() {
        return null;
    }

    @Override
    public void setResolver(String resolver) {

    }

    @Override
    public String getIp() {
        return ipAddress;
    }

    @Override
    public String getLocalIp() {
        return null;
    }

    @Override
    public void setLocalIp(String localIp) {

    }

    @Override
    public String getLocalHostname() {
        return null;
    }

    @Override
    public void setLocalHostname(String localHostname) {

    }

    @Override
    public String getPublicIp() {
        return null;
    }

    @Override
    public void setPublicIp(String publicIp) {

    }

    @Override
    public String getPublicHostname() {
        return null;
    }

    @Override
    public void setPublicHostname(String publicHostname) {

    }

    @Override
    public String getManualIp() {
        return null;
    }

    @Override
    public void setManualIp(String manualIp) {

    }

    @Override
    public int getMinimumPort() {
        return 0;
    }

    @Override
    public void setMinimumPort(int port) {

    }

    @Override
    public int getMaximumPort() {
        return 0;
    }

    @Override
    public void setMaximumPort(int port) {

    }

    @Override
    public void start() {

    }

    @Override
    public void start(boolean force) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void stop(boolean force) {

    }

    @Override
    public void destroy() {
        this.destroyed = true;
    }

    @Override
    public void destroy(boolean force) {
        destroy();
    }

    @Override
    public Container[] getChildren() {
        return new Container[0];
    }

    @Override
    public List<String> getJmxDomains() {
        return null;
    }

    @Override
    public void setJmxDomains(List<String> jmxDomains) {

    }

    @Override
    public boolean isProvisioningComplete() {
        return false;
    }

    @Override
    public boolean isProvisioningPending() {
        return false;
    }

    @Override
    public String getProvisionResult() {
        return null;
    }

    @Override
    public void setProvisionResult(String result) {

    }

    @Override
    public String getProvisionException() {
        return null;
    }

    @Override
    public void setProvisionException(String exception) {

    }

    @Override
    public List<String> getProvisionList() {
        return null;
    }

    @Override
    public void setProvisionList(List<String> bundles) {

    }

    @Override
    public Properties getProvisionChecksums() {
        return null;
    }

    @Override
    public void setProvisionChecksums(Properties checksums) {

    }

    @Override
    public String getProvisionStatus() {
        return null;
    }

    @Override
    public Map<String, String> getProvisionStatusMap() {
        return null;
    }

    @Override
    public CreateContainerMetadata<?> getMetadata() {
        return null;
    }

    @Override
    public boolean isAliveAndOK() {
        return alive;
    }
}
