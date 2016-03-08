package com.github.yuruki.fuse.autoscale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRequirements;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AutoScaledGroupTest {

    private final MockFabricService fabricService = new MockFabricService();

    private TestAppender appender;
    private Logger logger = Logger.getLogger(AutoScaledGroup.class);

    @Before
    public void setUp() throws Exception {
        appender = new TestAppender();
        appender.setThreshold(Level.WARN);
        logger.addAppender(appender);
    }

    @Test
    public void testApply() throws Exception {
        List<ProfileRequirements> profileRequirements;

        // Set up profiles and versions
        MockProfile oneProfile = new MockProfile("one-profile");
        MockProfile otherProfile = new MockProfile("other-profile");
        MockProfile noreqProfile = new MockProfile("no-requirements-auto");
        MockProfile min1Profile = new MockProfile("min1-auto");
        MockVersion version = new MockVersion("1.0");
        version.addProfile(oneProfile);
        version.addProfile(otherProfile);
        version.addProfile(noreqProfile);
        version.addProfile(min1Profile);

        // Set up initial containers
        List<Container> containerList = new ArrayList<>();
        MockContainer oneContainer = new MockContainer("auto1", true, "1");
        oneContainer.setVersion(version);
        oneContainer.addProfiles(oneProfile);
        containerList.add(oneContainer);
        MockContainer otherContainer = new MockContainer("other1", true, "1");
        otherContainer.setVersion(version);
        otherContainer.addProfiles(otherProfile);
        containerList.add(otherContainer);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min1Profile.getId()).minimumInstances(1)); // Minimum instances

        // Set up options
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .scaleContainers(false)
            .inheritRequirements(true)
            .containerPrefix("auto")
            .minContainerCount(1)
            .defaultMaxInstancesPerHost(1)
            .verbose(true);

        // Set up auto-scaled group
        AutoScaledGroup autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);

        // Non-matching parts should remain untouched
        List<Profile> otherContainerProfiles = Arrays.asList(otherContainer.getProfiles());
        assertTrue("otherContainer doesn't have otherProfile", otherContainerProfiles.contains(otherProfile));
        assertEquals("otherContainer doesn't have exactly one profile", 1, otherContainerProfiles.size());

        // Matching container should now have two profiles
        List<Profile> oneContainerProfiles = Arrays.asList(oneContainer.getProfiles());
        assertTrue("oneContainer doesn't have oneProfile", oneContainerProfiles.contains(oneProfile));
        assertTrue("oneContainer doesn't have min1Profile", oneContainerProfiles.contains(min1Profile));
        assertEquals("oneContainer doesn't have exactly two profiles", 2, oneContainerProfiles.size());
    }

    @Test
    public void testApplyTooManyPerHost() throws Exception {
        List<ProfileRequirements> profileRequirements;

        // Set up profiles and versions
        MockProfile oneProfile = new MockProfile("one-profile");
        MockProfile otherProfile = new MockProfile("other-profile");
        MockProfile noreqProfile = new MockProfile("no-requirements-auto");
        MockProfile min2Profile = new MockProfile("min2-auto");
        MockVersion version = new MockVersion("1.0");
        version.addProfile(oneProfile);
        version.addProfile(otherProfile);
        version.addProfile(noreqProfile);
        version.addProfile(min2Profile);

        // Set up initial containers
        List<Container> containerList = new ArrayList<>();
        MockContainer oneContainer = new MockContainer("auto1", true, "1");
        oneContainer.setVersion(version);
        oneContainer.addProfiles(oneProfile);
        containerList.add(oneContainer);
        MockContainer otherContainer = new MockContainer("other1", true, "1");
        otherContainer.setVersion(version);
        otherContainer.addProfiles(otherProfile);
        containerList.add(otherContainer);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min2Profile.getId()).minimumInstances(2)); // Minimum instances

        // Set up options
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .scaleContainers(false)
            .inheritRequirements(true)
            .containerPrefix("auto")
            .minContainerCount(1)
            .defaultMaxInstancesPerHost(1)
            .verbose(true);

        // Set up auto-scaled group
        assertEquals("Warnings or errors logged too early", 0, appender.getLog().size());
        AutoScaledGroup autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);

        // Non-matching parts should remain untouched
        List<Profile> otherContainerProfiles = Arrays.asList(otherContainer.getProfiles());
        assertEquals("otherContainer doesn't have exactly one profile", 1, otherContainerProfiles.size());
        assertTrue("otherContainer doesn't have otherProfile", otherContainerProfiles.contains(otherProfile));

        // Matching container should now have two profiles, and we should have logged an error for the third
        List<Profile> oneContainerProfiles = Arrays.asList(oneContainer.getProfiles());
        assertEquals("oneContainer doesn't have exactly two profiles", 2, oneContainerProfiles.size());
        assertTrue("oneContainer doesn't have oneProfile", oneContainerProfiles.contains(oneProfile));
        assertTrue("oneContainer doesn't have min2Profile", oneContainerProfiles.contains(min2Profile));
        assertTrue("No warnings or errors were logged", appender.getLog().size() > 0);
    }

    @Test
    public void testApplyScaleContainers() throws Exception {
        List<ProfileRequirements> profileRequirements;

        // Set up profiles and versions
        MockProfile oneProfile = new MockProfile("one-profile");
        MockProfile otherProfile = new MockProfile("other-profile");
        MockProfile noreqProfile = new MockProfile("no-requirements-auto");
        MockProfile min2Profile = new MockProfile("min2-auto");
        MockVersion version = new MockVersion("1.0");
        version.addProfile(oneProfile);
        version.addProfile(otherProfile);
        version.addProfile(noreqProfile);
        version.addProfile(min2Profile);

        // Set up initial containers
        List<Container> containerList = new ArrayList<>();
        MockContainer oneContainer = new MockContainer("auto1", true, "1");
        oneContainer.setVersion(version);
        oneContainer.addProfiles(oneProfile);
        containerList.add(oneContainer);
        MockContainer otherContainer = new MockContainer("other1", true, "1");
        otherContainer.setVersion(version);
        otherContainer.addProfiles(otherProfile);
        containerList.add(otherContainer);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min2Profile.getId()).minimumInstances(2)); // Minimum instances

        // Set up options
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .scaleContainers(true)
            .inheritRequirements(true)
            .containerPrefix("auto")
            .defaultMaxInstancesPerHost(1)
            .averageInstancesPerContainer(10)
            .verbose(true);

        // Set up auto-scaled group
        assertEquals("Warnings or errors logged too early", 0, appender.getLog().size());
        AutoScaledGroup autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);

        // We should have a new container to accommodate the third profile
        assertEquals("Too few containers", 2, autoScaledGroup.getGrandChildren().size());
        assertEquals("autoScaledGroup doesn't have exactly two instances of min2Profile", 2, autoScaledGroup.getProfileCount(min2Profile));

        // Non-matching parts should remain untouched
        List<Profile> otherContainerProfiles = Arrays.asList(otherContainer.getProfiles());
        assertEquals("otherContainer doesn't have exactly one profile", 1, otherContainerProfiles.size());
        assertTrue("otherContainer doesn't have otherProfile", otherContainerProfiles.contains(otherProfile));

        // Matching initial container should now have two profiles
        List<Profile> oneContainerProfiles = Arrays.asList(oneContainer.getProfiles());
        assertEquals("oneContainer doesn't have exactly two profiles", 2, oneContainerProfiles.size());
        assertTrue("oneContainer doesn't have oneProfile", oneContainerProfiles.contains(oneProfile));
        assertTrue("oneContainer doesn't have min2Profile", oneContainerProfiles.contains(min2Profile));
    }

    @Test
    public void testTooManyContainers() throws Exception {
        List<ProfileRequirements> profileRequirements;

        // Set up profiles and versions
        MockProfile oneProfile = new MockProfile("one-profile");
        MockProfile otherProfile = new MockProfile("other-profile");
        MockProfile noreqProfile = new MockProfile("no-requirements-auto");
        MockProfile min1Profile = new MockProfile("min1-auto");
        MockVersion version = new MockVersion("1.0");
        version.addProfile(oneProfile);
        version.addProfile(otherProfile);
        version.addProfile(noreqProfile);
        version.addProfile(min1Profile);

        // Set up initial containers
        List<Container> containerList = new ArrayList<>();
        MockContainer oneContainer = new MockContainer("auto1", true, "1");
        oneContainer.setVersion(version);
        oneContainer.addProfiles(oneProfile);
        containerList.add(oneContainer);
        MockContainer otherContainer = new MockContainer("auto2", true, "1");
        otherContainer.setVersion(version);
        otherContainer.addProfiles(otherProfile);
        containerList.add(otherContainer);
        MockContainer thirdContainer = new MockContainer("auto3", true, "1");
        otherContainer.setVersion(version);
        containerList.add(otherContainer);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min1Profile.getId()).minimumInstances(1)); // Minimum instances

        // Set up options
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .scaleContainers(true)
            .inheritRequirements(true)
            .containerPrefix("auto")
            .defaultMaxInstancesPerHost(1)
            .averageInstancesPerContainer(10)
            .verbose(true);

        // Set up auto-scaled group
        assertEquals("Warnings or errors logged too early", 0, appender.getLog().size());
        AutoScaledGroup autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);

        // We should have two containers left
        assertEquals("Wrong number of auto-scaled containers", 2, autoScaledGroup.getGrandChildren().size());
        assertEquals("autoScaledGroup has wrong number of min1Profile instances", 1, autoScaledGroup.getProfileCount(min1Profile));

        // Non-matching parts should remain untouched
        List<Profile> otherContainerProfiles = Arrays.asList(otherContainer.getProfiles());
        assertTrue("otherContainer doesn't have otherProfile", otherContainerProfiles.contains(otherProfile));
        List<Profile> oneContainerProfiles = Arrays.asList(oneContainer.getProfiles());
        assertTrue("oneContainer doesn't have oneProfile", oneContainerProfiles.contains(oneProfile));
    }

    @Test
    public void testTooManyInitialProfiles() throws Exception {
        List<ProfileRequirements> profileRequirements;

        // Set up profiles and versions
        MockProfile oneProfile = new MockProfile("one-auto");
        MockProfile otherProfile = new MockProfile("other-auto");
        MockProfile noreqProfile = new MockProfile("no-requirements-auto");
        MockProfile min1Profile = new MockProfile("min1-auto");
        MockVersion version = new MockVersion("1.0");
        version.addProfile(oneProfile);
        version.addProfile(otherProfile);
        version.addProfile(noreqProfile);
        version.addProfile(min1Profile);

        // Set up initial containers
        List<Container> containerList = new ArrayList<>();
        MockContainer oneContainer = new MockContainer("auto1", true, "1");
        oneContainer.setVersion(version);
        oneContainer.addProfiles(oneProfile, otherProfile, noreqProfile, min1Profile);
        containerList.add(oneContainer);

        // Set up profile requirements
        profileRequirements = new ArrayList<>();
        profileRequirements.add(new ProfileRequirements(noreqProfile.getId())); // No requirements
        profileRequirements.add(new ProfileRequirements(min1Profile.getId()).minimumInstances(1)); // Minimum instances

        // Set up options
        AutoScaledGroupOptions options = new AutoScaledGroupOptions()
            .containerPattern(Pattern.compile("^auto.*$").matcher(""))
            .profilePattern(Pattern.compile("^.*-auto$").matcher(""))
            .scaleContainers(true)
            .inheritRequirements(true)
            .containerPrefix("auto")
            .defaultMaxInstancesPerHost(1)
            .maxDeviation(0.0)
            .averageInstancesPerContainer(1)
            .verbose(true);

        // Set up auto-scaled group
        assertEquals("Warnings or errors logged too early", 0, appender.getLog().size());
        AutoScaledGroup autoScaledGroup = new AutoScaledGroup("test", options, containerList.toArray(new Container[containerList.size()]), profileRequirements.toArray(new ProfileRequirements[profileRequirements.size()]), new ContainerFactory(fabricService));
        autoScaledGroup.apply(5000);

        // We should have only min1Profile left
        List<Profile> oneContainerProfiles = Arrays.asList(oneContainer.getProfiles());
        assertTrue("Profile missing", oneContainerProfiles.contains(min1Profile));
        assertEquals("Wrong profile count", 1, oneContainerProfiles.size());
    }

    private class TestAppender extends AppenderSkeleton {
        private final List<LoggingEvent> log = new ArrayList<>();

        @Override
        protected void append(LoggingEvent loggingEvent) {
            log.add(loggingEvent);
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        public List<LoggingEvent> getLog() {
            return new ArrayList<>(log);
        }
    }
}