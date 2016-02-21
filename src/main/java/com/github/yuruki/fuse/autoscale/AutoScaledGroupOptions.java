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

    private Matcher containerPattern = Pattern.compile("^auto.*").matcher("");
    public static final String CONTAINER_PATTERN_DEFAULT = "^auto.*";
    private Matcher profilePattern = Pattern.compile("^.*-auto").matcher("");
    public static final String PROFILE_PATTERN_DEFAULT = "^.*-auto";
    private Boolean scaleContainers = true;
    public static final String SCALE_CONTAINERS_DEFAULT = "true";
    private Boolean inheritRequirements = true;
    public static final String INHERIT_REQUIREMENTS_DEFAULT = "true";
    private Double maxDeviation = 1.0;
    public static final String MAX_DEVIATION_DEFAULT = "1.0";
    private Integer averageAssignmentsPerContainer = -1;
    public static final String AVERAGE_ASSIGNMENTS_PER_CONTAINER_DEFAULT = "-1";
    private String containerPrefix = "auto";
    public static final String CONTAINER_PREFIX_DEFAULT = "auto";
    private Integer minContainerCount = 0;
    public static final String MIN_CONTAINER_COUNT_DEFAULT = "0";
    private Integer defaultMaximumInstancesPerHost = 1;
    public static final String DEFAULT_MAX_INSTANCES_PER_HOST_DEFAULT = "1";
    private Boolean ignoreErrors = true;
    public static final String IGNORE_ERRORS_DEFAULT = "true";
    private Integer maxContainersPerHost = 3;
    public static final String MAX_CONTAINERS_PER_HOST_DEFAULT = "3";

    public AutoScaledGroupOptions() {}

    public AutoScaledGroupOptions(
        Matcher containerPattern,
        Matcher profilePattern,
        Boolean scaleContainers,
        Boolean inheritRequirements,
        Double maxDeviation,
        Integer averageAssignmentsPerContainer,
        String containerPrefix,
        Integer minContainerCount,
        Integer defaultMaximumInstancesPerHost,
        Boolean ignoreErrors,
        Integer maxContainersPerHost) {
        this.containerPattern = containerPattern;
        this.profilePattern = profilePattern;
        this.scaleContainers = scaleContainers;
        this.inheritRequirements = inheritRequirements;
        this.maxDeviation = maxDeviation;
        this.averageAssignmentsPerContainer = averageAssignmentsPerContainer;
        this.containerPrefix = containerPrefix;
        this.minContainerCount = minContainerCount;
        this.defaultMaximumInstancesPerHost = defaultMaximumInstancesPerHost;
        this.ignoreErrors = ignoreErrors;
        this.maxContainersPerHost = maxContainersPerHost;
    }

    public AutoScaledGroupOptions containerPattern(Matcher containerPattern) {
        setContainerPattern(containerPattern);
        return this;
    }

    public AutoScaledGroupOptions profilePattern(Matcher profilePattern) {
        setProfilePattern(profilePattern);
        return this;
    }

    public AutoScaledGroupOptions scaleContainers(Boolean scaleContainers) {
        setScaleContainers(scaleContainers);
        return this;
    }

    public AutoScaledGroupOptions inheritRequirements(Boolean inheritRequirements) {
        setInheritRequirements(inheritRequirements);
        return this;
    }

    public AutoScaledGroupOptions maxDeviation(Double maxDeviation) {
        setMaxDeviation(maxDeviation);
        return this;
    }

    public AutoScaledGroupOptions averageAssignmentsPerContainer(Integer averageAssignmentsPerContainer) {
        setAverageAssignmentsPerContainer(averageAssignmentsPerContainer);
        return this;
    }

    public AutoScaledGroupOptions containerPrefix(String containerPrefix) {
        setContainerPrefix(containerPrefix);
        return this;
    }

    public AutoScaledGroupOptions minContainerCount(Integer minContainerCount) {
        setMinContainerCount(minContainerCount);
        return this;
    }

    public AutoScaledGroupOptions defaultMaximumInstancesPerHost(Integer defaultMaximumInstancesPerHost) {
        setDefaultMaximumInstancesPerHost(defaultMaximumInstancesPerHost);
        return this;
    }

    public AutoScaledGroupOptions ignoreErrors(Boolean ignoreErrors) {
        setIgnoreErrors(ignoreErrors);
        return this;
    }

    public AutoScaledGroupOptions maxContainersPerHost(Integer maxContainersPerHost) {
        setMaxContainersPerHost(maxContainersPerHost);
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

    public Boolean getScaleContainers() {
        return scaleContainers;
    }

    public void setScaleContainers(Boolean scaleContainers) {
        this.scaleContainers = scaleContainers;
    }

    public Boolean getInheritRequirements() {
        return inheritRequirements;
    }

    public void setInheritRequirements(Boolean inheritRequirements) {
        this.inheritRequirements = inheritRequirements;
    }

    public Double getMaxDeviation() {
        return maxDeviation;
    }

    public void setMaxDeviation(Double maxDeviation) {
        this.maxDeviation = maxDeviation;
    }

    public Integer getAverageAssignmentsPerContainer() {
        return averageAssignmentsPerContainer;
    }

    public void setAverageAssignmentsPerContainer(Integer averageAssignmentsPerContainer) {
        this.averageAssignmentsPerContainer = averageAssignmentsPerContainer;
    }

    public String getContainerPrefix() {
        return containerPrefix;
    }

    public void setContainerPrefix(String containerPrefix) {
        this.containerPrefix = containerPrefix;
    }

    public Integer getMinContainerCount() {
        return minContainerCount;
    }

    public void setMinContainerCount(Integer minContainerCount) {
        this.minContainerCount = minContainerCount;
    }

    public Integer getDefaultMaximumInstancesPerHost() {
        return defaultMaximumInstancesPerHost;
    }

    public void setDefaultMaximumInstancesPerHost(Integer defaultMaximumInstancesPerHost) {
        this.defaultMaximumInstancesPerHost = defaultMaximumInstancesPerHost;
    }

    public Boolean getIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(Boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public Integer getMaxContainersPerHost() {
        return maxContainersPerHost;
    }

    public void setMaxContainersPerHost(Integer maxContainersPerHost) {
        this.maxContainersPerHost = maxContainersPerHost;
    }
}
