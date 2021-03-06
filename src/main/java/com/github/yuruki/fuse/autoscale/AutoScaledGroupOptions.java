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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AutoScaledGroupOptions {

    private int averageInstancesPerContainer = -1;
    static final String AVERAGE_INSTANCES_PER_CONTAINER_DEFAULT = "-1";
    private Matcher containerPattern = Pattern.compile("^auto.*").matcher("");
    static final String CONTAINER_PATTERN_DEFAULT = "^auto.*";
    private String containerPrefix = "auto";
    static final String CONTAINER_PREFIX_DEFAULT = "auto";
    private int defaultMaxInstancesPerHost = 1;
    static final String DEFAULT_MAX_INSTANCES_PER_HOST_DEFAULT = "1";
    private boolean ignoreErrors = true;
    static final String IGNORE_ERRORS_DEFAULT = "true";
    private boolean inheritRequirements = true;
    static final String INHERIT_REQUIREMENTS_DEFAULT = "true";
    private int maxContainersPerHost = 3;
    static final String MAX_CONTAINERS_PER_HOST_DEFAULT = "3";
    private double maxDeviation = 1.0;
    static final String MAX_DEVIATION_DEFAULT = "1.0";
    private int minContainerCount = 1;
    static final String MIN_CONTAINER_COUNT_DEFAULT = "1";
    private Matcher profilePattern = Pattern.compile("^.*-auto").matcher("");
    static final String PROFILE_PATTERN_DEFAULT = "^.*-auto";
    private boolean scaleContainers = true;
    static final String SCALE_CONTAINERS_DEFAULT = "true";
    private boolean dryRun = false;
    static final String DRY_RUN_DEFAULT = "false";
    private Matcher rootContainerPattern = Pattern.compile(".*").matcher("");
    static final String ROOT_CONTAINER_PATTERN_DEFAULT = ".*";
    private int changesPerPoll = 0;
    static final String CHANGES_PER_POLL_DEFAULT = "0";

    AutoScaledGroupOptions() {}

    AutoScaledGroupOptions(
        Matcher containerPattern,
        Matcher profilePattern,
        boolean scaleContainers,
        boolean inheritRequirements,
        double maxDeviation,
        int averageInstancesPerContainer,
        String containerPrefix,
        int minContainerCount,
        int defaultMaxInstancesPerHost,
        boolean ignoreErrors,
        int maxContainersPerHost,
        boolean dryRun,
        Matcher rootContainerPattern,
        int changesPerPoll) {
        this.containerPattern = containerPattern;
        this.profilePattern = profilePattern;
        this.scaleContainers = scaleContainers;
        this.inheritRequirements = inheritRequirements;
        this.maxDeviation = maxDeviation;
        this.averageInstancesPerContainer = averageInstancesPerContainer;
        this.containerPrefix = containerPrefix;
        this.minContainerCount = minContainerCount;
        this.defaultMaxInstancesPerHost = defaultMaxInstancesPerHost;
        this.ignoreErrors = ignoreErrors;
        this.maxContainersPerHost = maxContainersPerHost;
        this.dryRun = dryRun;
        this.rootContainerPattern = rootContainerPattern;
        this.changesPerPoll = changesPerPoll;
    }

    AutoScaledGroupOptions containerPattern(Matcher containerPattern) {
        setContainerPattern(containerPattern);
        return this;
    }

    AutoScaledGroupOptions profilePattern(Matcher profilePattern) {
        setProfilePattern(profilePattern);
        return this;
    }

    AutoScaledGroupOptions scaleContainers(boolean scaleContainers) {
        setScaleContainers(scaleContainers);
        return this;
    }

    AutoScaledGroupOptions inheritRequirements(boolean inheritRequirements) {
        setInheritRequirements(inheritRequirements);
        return this;
    }

    AutoScaledGroupOptions maxDeviation(double maxDeviation) {
        setMaxDeviation(maxDeviation);
        return this;
    }

    AutoScaledGroupOptions averageInstancesPerContainer(int averageInstancesPerContainer) {
        setAverageInstancesPerContainer(averageInstancesPerContainer);
        return this;
    }

    AutoScaledGroupOptions containerPrefix(String containerPrefix) {
        setContainerPrefix(containerPrefix);
        return this;
    }

    AutoScaledGroupOptions minContainerCount(int minContainerCount) {
        setMinContainerCount(minContainerCount);
        return this;
    }

    AutoScaledGroupOptions defaultMaxInstancesPerHost(int defaultMaxInstancesPerHost) {
        setDefaultMaxInstancesPerHost(defaultMaxInstancesPerHost);
        return this;
    }

    AutoScaledGroupOptions ignoreErrors(boolean ignoreErrors) {
        setIgnoreErrors(ignoreErrors);
        return this;
    }

    AutoScaledGroupOptions maxContainersPerHost(int maxContainersPerHost) {
        setMaxContainersPerHost(maxContainersPerHost);
        return this;
    }

    AutoScaledGroupOptions dryRun(boolean dryRun) {
        setDryRun(dryRun);
        return this;
    }

    AutoScaledGroupOptions rootContainerPattern(Matcher rootContainerPattern) {
        setRootContainerPattern(rootContainerPattern);
        return this;
    }

    AutoScaledGroupOptions changesPerPoll(int changesPerPoll) {
        setChangesPerPoll(changesPerPoll);
        return this;
    }

    Matcher getContainerPattern() {
        return containerPattern;
    }

    void setContainerPattern(Matcher containerPattern) {
        this.containerPattern = containerPattern;
    }

    Matcher getProfilePattern() {
        return profilePattern;
    }

    void setProfilePattern(Matcher profilePattern) {
        this.profilePattern = profilePattern;
    }

    boolean isScaleContainers() {
        return scaleContainers;
    }

    void setScaleContainers(boolean scaleContainers) {
        this.scaleContainers = scaleContainers;
    }

    boolean isInheritRequirements() {
        return inheritRequirements;
    }

    void setInheritRequirements(boolean inheritRequirements) {
        this.inheritRequirements = inheritRequirements;
    }

    double getMaxDeviation() {
        return maxDeviation;
    }

    void setMaxDeviation(double maxDeviation) {
        this.maxDeviation = maxDeviation;
    }

    int getAverageInstancesPerContainer() {
        return averageInstancesPerContainer;
    }

    void setAverageInstancesPerContainer(int averageInstancesPerContainer) {
        this.averageInstancesPerContainer = averageInstancesPerContainer;
    }

    String getContainerPrefix() {
        return containerPrefix;
    }

    void setContainerPrefix(String containerPrefix) {
        this.containerPrefix = containerPrefix;
    }

    int getMinContainerCount() {
        return minContainerCount;
    }

    void setMinContainerCount(int minContainerCount) {
        this.minContainerCount = minContainerCount;
    }

    int getDefaultMaxInstancesPerHost() {
        return defaultMaxInstancesPerHost;
    }

    void setDefaultMaxInstancesPerHost(int defaultMaxInstancesPerHost) {
        this.defaultMaxInstancesPerHost = defaultMaxInstancesPerHost;
    }

    boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    int getMaxContainersPerHost() {
        return maxContainersPerHost;
    }

    void setMaxContainersPerHost(int maxContainersPerHost) {
        this.maxContainersPerHost = maxContainersPerHost;
    }

    boolean isDryRun() {
        return dryRun;
    }

    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    Matcher getRootContainerPattern() {
        return rootContainerPattern;
    }

    void setRootContainerPattern(Matcher rootContainerPattern) {
        this.rootContainerPattern = rootContainerPattern;
    }

    int getChangesPerPoll() {
        return changesPerPoll;
    }

    void setChangesPerPoll(int changesPerPoll) {
        this.changesPerPoll = changesPerPoll;
    }
}
