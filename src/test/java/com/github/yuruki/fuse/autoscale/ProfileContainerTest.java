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

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileContainerTest.class);

    private final MockFabricService fabricService = new MockFabricService();

    private List<ProfileContainer> profileContainers;
    private AutoScaledGroup autoScaledGroup;
    private MockProfile profile1;
    private MockProfile profile2;
    private MockProfile profile3;
    private MockProfile profile4;
    private MockProfile profile5;
    private List<ProfileRequirements> profileRequirements;

    @org.junit.Before
    public void setUp() throws Exception {
        // Set up profiles
        profile1 = new MockProfile("first-auto");
        profile2 = new MockProfile("second-auto");
        profile3 = new MockProfile("third-auto");
        profile4 = new MockProfile("fourth-auto");
        profile5 = new MockProfile("fifth-auto");

        // Set up versions
        MockVersion version = new MockVersion("1.0");
        version.addProfile(profile1);
        version.addProfile(profile2);
        version.addProfile(profile3);
        version.addProfile(profile4);
        version.addProfile(profile5);

        // Set up containers
        MockContainer container = new MockContainer("root", true, "host1", true);
        container.setVersion(version);
        List<Container> containerList = new ArrayList<>();
        containerList.add(container);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(profile1.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(profile2.getId()).minimumInstances(1)); // Minimum instances

        // Set up parameters
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .scaleContainers(true)
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .containerPrefix("auto")
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .inheritRequirements(true)
            .defaultMaxInstancesPerHost(1)
            .averageInstancesPerContainer(10);

        // Set up testables
        autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);
        profileContainers = new ArrayList<>();
        profileContainers.add(autoScaledGroup);
        profileContainers.addAll(autoScaledGroup.getChildren());
        profileContainers.addAll(autoScaledGroup.getGrandChildren());
    }

    @org.junit.Test
    public void testHasProfile() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(profile2.getId()));
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
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(profile2));
        }
    }

    @org.junit.Test
    public void testAddProfile() throws Exception {
        for (ProfileContainer container : profileContainers) {
            assertFalse(container.getId() + " shouldn't have the profile yet", container.hasProfile(profile1));
        }
        autoScaledGroup.addProfile(new ProfileRequirements(profile1.getId()).minimumInstances(1));
        for (ProfileContainer container : profileContainers) {
            assertTrue("Profile missing in " + container.getId(), container.hasProfile(profile1));
        }
    }

    @org.junit.Test
    public void testAddProfile1() throws Exception {

    }

    @org.junit.Test
    public void testRemoveProfile() throws Exception {

    }

    @org.junit.Test
    public void testRemoveProfile1() throws Exception {

    }

    @org.junit.Test
    public void testRemoveProfile2() throws Exception {

    }

    @org.junit.Test
    public void testRemoveProfile3() throws Exception {

    }

    @org.junit.Test
    public void testRemoveProfile4() throws Exception {

    }

    @org.junit.Test
    public void testGetProfileCount() throws Exception {

    }

    @org.junit.Test
    public void testGetProfileCount1() throws Exception {

    }

    @org.junit.Test
    public void testGetProfileCount2() throws Exception {

    }

    @org.junit.Test
    public void testGetProfileCount3() throws Exception {

    }

    @org.junit.Test
    public void testGetId() throws Exception {

    }
}