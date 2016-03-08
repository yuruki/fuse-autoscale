package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.fabric8.api.Container;
import io.fabric8.api.ProfileRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ProfileContainerTest {

    private final MockFabricService fabricService = new MockFabricService();

    private List<ProfileContainer> profileContainers;
    private AutoScaledGroup autoScaledGroup;
    private MockProfile noreqProfile;
    private MockProfile min1Profile;
    private MockProfile profile3;
    private MockProfile profile4;
    private MockProfile profile5;
    private List<ProfileRequirements> profileRequirements;

    @org.junit.Before
    public void setUp() throws Exception {
        // Set up profiles
        noreqProfile = new MockProfile("noreq-auto");
        min1Profile = new MockProfile("min1-auto");
        profile3 = new MockProfile("third-auto");
        profile4 = new MockProfile("fourth-auto");
        profile5 = new MockProfile("fifth-auto");

        // Set up versions
        MockVersion version = new MockVersion("1.0");
        version.addProfile(noreqProfile);
        version.addProfile(min1Profile);
        version.addProfile(profile3);
        version.addProfile(profile4);
        version.addProfile(profile5);

        // Set up containers
        MockContainer container = new MockContainer("root", true, "host1", true);
        container.setVersion(version);
        container.addProfiles(profile3, profile4, profile5);
        List<Container> containerList = new ArrayList<>();
        containerList.add(container);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min1Profile.getId()).minimumInstances(1)); // Minimum instances

        // Set up parameters
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .scaleContainers(true)
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .containerPrefix("auto")
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .inheritRequirements(true)
            .defaultMaxInstancesPerHost(1)
            .averageInstancesPerContainer(10)
            .verbose(true);

        // Set up testables
        autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        profileContainers = new ArrayList<>();
        profileContainers.add(autoScaledGroup);
        profileContainers.addAll(autoScaledGroup.getChildren());
        profileContainers.addAll(autoScaledGroup.getGrandChildren());
    }

    @org.junit.Test
    public void testHasProfile() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(min1Profile.getId()));
        }
    }

    @org.junit.Test
    public void testHasProfile1() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(profileRequirements.get(1)));
        }
    }

    @org.junit.Test
    public void testHasProfile2() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(min1Profile));
        }
    }

    @org.junit.Test
    public void testAddProfile() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertFalse(container.getId() + " shouldn't have the profile yet", container.hasProfile(noreqProfile));
        }
        autoScaledGroup.addProfile(new ProfileRequirements(noreqProfile.getId()).minimumInstances(1));
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(noreqProfile));
        }
    }

    @org.junit.Test
    public void testAddProfile1() throws Exception {
        assertTrue("Profile missing", autoScaledGroup.hasProfile(min1Profile));
        autoScaledGroup.addProfile(new ProfileRequirements(min1Profile.getId()).minimumInstances(2).maximumInstancesPerHost(1));
        assertEquals("Wrong container count", 2, autoScaledGroup.getGrandChildren().size());
    }

    @org.junit.Test
    public void testAddManyProfiles() throws Exception {
        assertTrue("Profile missing", autoScaledGroup.hasProfile(min1Profile));
        autoScaledGroup.addProfile(new ProfileRequirements(min1Profile.getId()).minimumInstances(1000).maximumInstancesPerHost(1));
        assertEquals("Wrong container count", 1000, autoScaledGroup.getGrandChildren().size());
    }

    @org.junit.Test
    public void testAddManyProfilesOneByOne() throws Exception {
        assertTrue("Profile missing", autoScaledGroup.hasProfile(min1Profile));
        for (int i = 1; i <= 1000; i++) {
            autoScaledGroup.addProfile(new ProfileRequirements(min1Profile.getId()).minimumInstances(i).maximumInstancesPerHost(1));
        }
        assertEquals("Wrong profile count", 1000, autoScaledGroup.getProfileCount());
    }

    @org.junit.Test
    public void testRemoveProfile() throws Exception {
        assertTrue("Profile missing", autoScaledGroup.hasProfile(min1Profile));
        autoScaledGroup.removeProfile(min1Profile.getId());
        assertEquals("Wrong profile count", 0, autoScaledGroup.getProfileCount());
        assertEquals("Wrong container count", 0, autoScaledGroup.getGrandChildren().size());
    }
    @org.junit.Test

    public void testRemoveProfile1() throws Exception {
        assertTrue("Profile missing", autoScaledGroup.hasProfile(min1Profile));
        autoScaledGroup.addProfile(new ProfileRequirements(min1Profile.getId()).minimumInstances(1000).maximumInstancesPerHost(1));
        assertEquals("Wrong container count", 1000, autoScaledGroup.getGrandChildren().size());
        autoScaledGroup.removeProfile(min1Profile.getId(), 500);
        assertEquals("Wrong container count", 500, autoScaledGroup.getGrandChildren().size());
    }
}