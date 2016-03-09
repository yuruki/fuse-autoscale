/**
 *  Copyright 2016 Jyrki Ruuskanen
 *
 *  Jyrki Ruuskanen licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.github.yuruki.fuse.autoscale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoScaledGroupOptions {

    private int averageInstancesPerContainer = -1;
    public static final String AVERAGE_INSTANCES_PER_CONTAINER_DEFAULT = "-1";
    private Matcher containerPattern = Pattern.compile("^auto.*").matcher("");
    public static final String CONTAINER_PATTERN_DEFAULT = "^auto.*";
    private String containerPrefix = "auto";
    public static final String CONTAINER_PREFIX_DEFAULT = "auto";
    private int defaultMaxInstancesPerHost = 1;
    public static final String DEFAULT_MAX_INSTANCES_PER_HOST_DEFAULT = "1";
    private boolean ignoreErrors = true;
    public static final String IGNORE_ERRORS_DEFAULT = "true";
    private boolean inheritRequirements = true;
    public static final String INHERIT_REQUIREMENTS_DEFAULT = "true";
    private int maxContainersPerHost = 3;
    public static final String MAX_CONTAINERS_PER_HOST_DEFAULT = "3";
    private double maxDeviation = 1.0;
    public static final String MAX_DEVIATION_DEFAULT = "1.0";
    private int minContainerCount = 0;
    public static final String MIN_CONTAINER_COUNT_DEFAULT = "0";
    private Matcher profilePattern = Pattern.compile("^.*-auto").matcher("");
    public static final String PROFILE_PATTERN_DEFAULT = "^.*-auto";
    private boolean scaleContainers = true;
    public static final String SCALE_CONTAINERS_DEFAULT = "true";
    private boolean dryRun = false;
    public static final String DRY_RUN_DEFAULT = "false";

    public AutoScaledGroupOptions() {}

    public AutoScaledGroupOptions(
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
        boolean dryRun) {
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
    }

    public AutoScaledGroupOptions containerPattern(Matcher containerPattern) {
        setContainerPattern(containerPattern);
        return this;
    }

    public AutoScaledGroupOptions profilePattern(Matcher profilePattern) {
        setProfilePattern(profilePattern);
        return this;
    }

    public AutoScaledGroupOptions scaleContainers(boolean scaleContainers) {
        setScaleContainers(scaleContainers);
        return this;
    }

    public AutoScaledGroupOptions inheritRequirements(boolean inheritRequirements) {
        setInheritRequirements(inheritRequirements);
        return this;
    }

    public AutoScaledGroupOptions maxDeviation(double maxDeviation) {
        setMaxDeviation(maxDeviation);
        return this;
    }

    public AutoScaledGroupOptions averageInstancesPerContainer(int averageInstancesPerContainer) {
        setAverageInstancesPerContainer(averageInstancesPerContainer);
        return this;
    }

    public AutoScaledGroupOptions containerPrefix(String containerPrefix) {
        setContainerPrefix(containerPrefix);
        return this;
    }

    public AutoScaledGroupOptions minContainerCount(int minContainerCount) {
        setMinContainerCount(minContainerCount);
        return this;
    }

    public AutoScaledGroupOptions defaultMaxInstancesPerHost(int defaultMaxInstancesPerHost) {
        setDefaultMaxInstancesPerHost(defaultMaxInstancesPerHost);
        return this;
    }

    public AutoScaledGroupOptions ignoreErrors(boolean ignoreErrors) {
        setIgnoreErrors(ignoreErrors);
        return this;
    }

    public AutoScaledGroupOptions maxContainersPerHost(int maxContainersPerHost) {
        setMaxContainersPerHost(maxContainersPerHost);
        return this;
    }

    public AutoScaledGroupOptions dryRun(boolean dryRun) {
        setDryRun(dryRun);
        return this;
    }

    public Matcher getContainerPattern() {
        return containerPattern;
    }

    public void setContainerPattern(Matcher containerPattern) {
        this.containerPattern = containerPattern;
    }

    public Matcher getProfilePattern() {
        return profilePattern;
    }

    public void setProfilePattern(Matcher profilePattern) {
        this.profilePattern = profilePattern;
    }

    public boolean isScaleContainers() {
        return scaleContainers;
    }

    public void setScaleContainers(boolean scaleContainers) {
        this.scaleContainers = scaleContainers;
    }

    public boolean isInheritRequirements() {
        return inheritRequirements;
    }

    public void setInheritRequirements(boolean inheritRequirements) {
        this.inheritRequirements = inheritRequirements;
    }

    public double getMaxDeviation() {
        return maxDeviation;
    }

    public void setMaxDeviation(double maxDeviation) {
        this.maxDeviation = maxDeviation;
    }

    public int getAverageInstancesPerContainer() {
        return averageInstancesPerContainer;
    }

    public void setAverageInstancesPerContainer(int averageInstancesPerContainer) {
        this.averageInstancesPerContainer = averageInstancesPerContainer;
    }

    public String getContainerPrefix() {
        return containerPrefix;
    }

    public void setContainerPrefix(String containerPrefix) {
        this.containerPrefix = containerPrefix;
    }

    public int getMinContainerCount() {
        return minContainerCount;
    }

    public void setMinContainerCount(int minContainerCount) {
        this.minContainerCount = minContainerCount;
    }

    public int getDefaultMaxInstancesPerHost() {
        return defaultMaxInstancesPerHost;
    }

    public void setDefaultMaxInstancesPerHost(int defaultMaxInstancesPerHost) {
        this.defaultMaxInstancesPerHost = defaultMaxInstancesPerHost;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public int getMaxContainersPerHost() {
        return maxContainersPerHost;
    }

    public void setMaxContainersPerHost(int maxContainersPerHost) {
        this.maxContainersPerHost = maxContainersPerHost;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
