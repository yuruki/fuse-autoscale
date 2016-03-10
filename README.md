# Autoscaler for Red Hat JBoss Fuse 6.2 (fabric8 1.2) on Karaf

## Background

We are running Red Hat JBoss Fuse 6.2.1 (fabric8 1.2) on Karaf with 3 to 5 node Fabric ensembles. We have one CamelContext per bundle and one Fabric profile per bundle. This adds up to over 100 of CamelContexts, bundles and profiles each.

The problem we are facing is scaling. The bundles keep piling up but they are still running on one Karaf container per host. The JVM heap size is getting a bit unwieldy.

The autoscale feature on fabric8 (fabric-autoscale) uses an approach of one container per profile. In practice that means we would have to create a smaller number of high level profiles and add our profiles as parents in them, by hand, while making sure each profile is assigned exactly once per host (static port allocations in our bundles dictate this). Also, we don't want our container layout to change automatically and without limits.

Not using fabric-autoscale we would have to create more containers with smaller heaps and spread the profiles on them, but assigning the profiles is tedious and error-prone as explained above.

## Solution

Instead of fabric-autoscale's concept of focusing on the containers and applying one profile per container we should focus on the profiles. Profile requirements define the desired running state with the instance limits and dependencies. Adding maxDeviation and averageInstancesPerContainer parameters to autoscaler we can essentially define the shape of an elastic Fabric. Fuse-autoscaler component implements this profile-centric approach.

Example config:

* scaleContainers = true
* averageInstancesPerContainer = 50
* profilePattern = ^.*-auto$
* containerPattern = ^auto.*$
* containerPrefix = auto
* maxContainersPerHost = 5

Scenario:

User has a thousand profiles with their requirements defined. User has the static part of the Fabric set up with the Zookeeper ensemble, maybe the brokers and whatever we don't want to scale. Then we add the autoscaler to the mix with the above configuration.

The autoscaler will adjust matching profile assignments on containers matching the container pattern, adding containers if there's not enough, starting existing containers if they are not started and removing containers that are can be removed safely and are not needed.

## Configuration

Fuse-autoscaler uses the following parameters in io.fabric8.autoscale PID:

* **pollTime (long: 15000)**: The number of milliseconds between polls to check if the system still has its requirements satisfied.
* **autoscalerGroupId ("default")**: The group ID for this autoscaler. You can run multiple autoscalers concurrently as long as they have unique group IDs. If you do, take care that the profilePatterns don't overlap or things might get crazy. See Example 2 below.
* **scaleContainers (bool: true)**: Allow autoscaler to create, start and remove containers.
* **profilePattern (regex: `^.*-auto`)**: Only matching profile names are considered for autoscaling.
* **containerPattern (regex: `^auto.*`)**: Only matching containers are used for profile assignment autoscaling.
* **containerPrefix ("auto")**: Container name prefix used for new containers created by autoscaler. The prefix must match containerPattern. 
* **defaultMaxInstancesPerHost (int: 1)**: Default value for maximum profile instances per host.
* **minContainerCount (int: 0)**: Minimum number of applicable containers on which autoscaling can be performed.
* **maxDeviation (double: 1.0, >= 0)**: If a container has more than x + maximumDeviation * x profiles assigned, the excess profiles will be reassigned. x = matched profile count / applicable container count, rounded up.
* **inheritRequirements (bool: true)**: Profile dependencies will inherit their requirements from the parent if their requirements are not set. Inherited requirements are transient and won't change your configured requirements.
* **averageInstancesPerContainer (int: -1)**: The desired average number of profile instances per container when scaling with containers.
* **maxContainersPerHost (int: 3)**: Maximum allowed number of autoscaled containers per host. Set this to match the resources of your hosts.
* **ignoreErrors (bool: true)**: Perform autoscaling even when all the requirements couldn't be satisfied.
* **dryRun (bool: false)**: Do not apply any changes, only log them. Useful for safe testing on a live system.

## Usage

Create a new profile, add com.github.yuruki/fuse-autoscale bundle to it and add *fabric* profile as a parent. Then assign your profile on one or more Fabric root containers. Add io.fabric8.autoscale.properties with the autoscale configuration to the profile.

### Maintenance mode

When `scaleContainers = true`, deleting containers or shutting them down in order to reboot the host cleanly can be difficult because the autoscaler keeps restarting and recreating the containers.

To enter so-called Maintenance mode which allows you to control container lifecycle manually, set `scaleContainers = false` and increase `maxDeviation` to allow orphaned profiles to migrate to the remaining containers (the value depends on your setup). You can perform the change with for example `fabric:profile-edit --pid io.fabric8.autoscale/scaleContainers=false --pid io.fabric8.autoscale/maxDeviation=1 your-autoscale-profile` command. The PID to use is the properties file name without the extension.

Set `scaleContainers` and `maxDeviation` back to their original values when you are done and the root container is up.

## Caveats

Autoscaler can only create child containers for now. Feel free to add the other container providers to the component.

## Example 1

io.fabric8.autoscale.properties:

* scaleContainers = false
* profilePattern = `^.*-dev$`
* containerPattern = `^camel.*$`
* defaultMaxInstancesPerHost = 1

With this configuration the autoscaler will not create, start or remove any containers. Instead it will try to assign matched profiles according to their requirements on applicable running containers. The autoscaler will consider profiles that end with "-dev" and containers whose name starts with "camel". By default the maximum profile instances per host are limited to 1.

The autoscaler will make an effort to spread the profiles evenly across the containers. If the requirements change, the autoscaler will adjust the profile assignments accordingly.

Using a different profilePattern for test and production environments you can control what is running where solely by adjusting the profile requirements. In this kind of setup the autoscaled profiles for dev, test and prod would point to the same profile with the actual implementation in it.

## Example 2

This is a real-world configuration from our UAT environment. We create one fuse-autoscale instance for the brokers and one for the workers by using a "dash suffix" in the properties file name (OSGi Factory Configuration).

io.fabric8.autoscale-broker.properties:

* autoscalerGroupId = broker
* profilePattern = `^mq-broker-.*-devbroker$`
* scaleContainers = true
* containerPattern = `^broker.*$`
* containerPrefix = broker
* maxContainersPerHost = 1
* minContainerCount = 2
* defaultMaxInstancesPerHost = 1
* inheritRequirements = true
* averageInstancesPerContainer = 1
* maxDeviation = 0

io.fabric8.autoscale-worker.properties:

* autoscalerGroupId = worker
* profilePattern = `^(company-template-.*-test)|(company-bundle-.*-test)$`
* scaleContainers = true
* containerPattern = `^worker.*$`
* containerPrefix = worker
* defaultMaxInstancesPerHost = 1
* inheritRequirements = true
* maxContainersPerHost = 3
* minContainerCount = 6
* averageInstancesPerContainer = 30
* maxDeviation = 0.5

We won't configure maximum instances per host in the profile requirements; autoscaler will use the default value of 1. We will only define the requirements for the profiles that we explicitly want to assign. Profile dependencies defined via dependsOn mechanism will inherit their requirements from their parents. This will keep the profile requirements DRY.

The broker autoscaler will consider profiles that match "mq-broker-\*-testbroker" and assign them on containers starting with "broker" according to profile requirements. We want at most one container per host and one broker profile per container. When in maintenance mode (scaleContainers=false) we want the autoscaler to stay passive if there are less than two broker containers available.

The worker autoscaler will consider profiles that match "company-template-\*-test" or "company-bundle-\*-test" and assign them on containers starting with "worker" according to profile requirements. We want at most two containers per host and on average 40 worker profiles per container (at most 60 per container). When in maintenance mode we want the autoscaler to stay passive if there are less than four worker containers available.

Broker and worker container lifecycles will be controlled by their associated autoscalers.
