# Autoscaler for Red Hat JBoss Fuse 6.2 (fabric8 1.2)

## Background

We are running Red Hat JBoss Fuse 6.2.1 (fabric8 1.2) with Apache Karaf on 3 and 5 node Fabric ensembles. We have one CamelContext per bundle and one Fabric profile per bundle. This adds up to over 100 of CamelContexts, bundles and profiles each.

The problem we are facing is scaling. The bundles keep piling up but they are still running on one Karaf container per host. The JVM heap size is getting a bit unwieldy.

The autoscale feature on fabric8 uses an approach of one container per profile, which practically means that we would have to create a smaller number of high level profiles where we add our profiles as parents, by hand, while making sure each profile is added exactly once (static port allocations in our bundles dictate this). Also, we don't want our container layout to change automatically.

Not using autoscale we might create more containers with smaller heaps and spread the profiles on them, but assigning the profiles is tedious and error-prone as explained above.

## Solution

Instead of fabric8's current autoscale concept focusing on the containers and applying one profile per container we should focus on the profiles. Profile requirements define the desired running state with the instance limits and dependencies. With the combination of maxDeviation and averageAssignmentsPerContainer parameters we can essentially define the shape of an elastic Fabric. Fuse-autoscaler component implements this profile-centric approach.

Example config:

* scaleContainers = true
* averageAssignmentsPerContainer = 50
* profilePattern = ^.*-auto$
* containerPattern = ^auto.*$
* containerPrefix = auto
* maxContainersPerHost = 5

Scenario:

User has a thousand profiles with their requirements defined. User has the static part of the Fabric set up with the Zookeeper ensemble, maybe the brokers and whatever we don't want to scale. Then we add the autoscaler to the mix with the above configuration.

The autoscaler will start apply the assignments to containers matching the container pattern, adding them if there's not enough, starting existing ones if they are not started and removing ones that are can be removed safely and are not neede.

## Configuration

Fuse-autoscaler uses the following parameters in io.fabric8.autoscale PID:

* **pollTime (long: 15000)**: The number of milliseconds between polls to check if the system still has its requirements satisfied.
* **autoscalerGroupId ("default")**: (NOT TESTED YET) The group ID for this autoscaler. You can run multiple autoscalers concurrently as long as they have unique group IDs. If you do, take care that the profilePatterns don't overlap or things might get crazy.
* **scaleContainers (bool: true)**: Allow autoscaler to create, start and remove containers.
* **profilePattern (regex: `^.*-auto`)**: Only matching profile names are considered for autoscaling.
* **containerPattern (regex: `^auto.*`)**: Only matching containers are used for profile assignment autoscaling.
* **containerPrefix ("auto")**: Container name prefix used for new containers created by autoscaler. The prefix must match containerPattern. 
* **defaultMaxInstancesPerHost (int: 1)**: Default value for maximum profile instances per host.
* **minContainerCount (int: 0)**: Minimum number of applicable containers on which autoscaling can be performed.
* **maxDeviation (double: 1.0, >= 0)**: If a container has more than x + maximumDeviation * x profiles assigned, the excess profiles will be reassigned. x = matched profile count / applicable container count, rounded up.
* **inheritRequirements (bool: true)**: Profile dependencies will inherit their requirements from the parent if their requirements are not set. Inherited requirements are transient and won't change your configured requirements.
* **averageAssignmentsPerContainer (int: -1)**: The desired average number of profile assignments per container when scaling with containers.
* **maxContainersPerHost (int: 3)**: Maximum allowed number of autoscaled containers per host. Set this to match the resources of your hosts.
* **ignoreErrors (bool: true)**: Perform autoscaling even when all the requirements couldn't be satisfied.

## Usage

Add com.github.yuruki/fuse-autoscale bundle to *fabric* profile, or create a new profile with the bundle and assign it on one or more Fabric root containers. Add io.fabric8.autoscale.properties with the autoscale configuration to the profile.

### Maintenance mode

When *scaleContainers* is set to *true*, deleting containers or shutting them down in order to reboot the host cleanly can be difficult because the autoscaler keeps restarting and recreating the containers.

To enter so-called Maintenance mode which allows you to work with container lifecycle manually, set *scaleContainers* to *false* and increase *maxDeviation* to *1* (depends on your setup) to allow profiles to migrate to other containers. You can perform the change with for example *fabric:profile-edit --pid io.fabric8.autoscale/scaleContainers=false --pid io.fabric8.autoscale/maxDeviation=1 your-profile* command. Set *scaleContainers* and *maxDeviation* back to their original values when you are done and the host is ready.

## Caveats

Autoscaler can only create child containers for now. Feel free to add the other container providers to the component.

## Example 1

io.fabric8.autoscale/
* scaleContainers = false
* profilePattern = ^.*-dev$
* containerPattern = ^camel.*$
* defaultMaxInstancesPerHost = 1

With this configuration the autoscaler will not create, start or remove any containers. Instead it will try to assign all matched profiles according to their requirements on applicable containers. The autoscaler will consider profiles that end with "-dev" and containers whose name starts with "camel". By default the maximum profile instances per host are limited to 1.

The autoscaler will make an effort to spread the profiles evenly across the applicable containers. If the requirements change, the autoscaler will adjust the assignments accordingly.

Using a different profilePattern for test and production environments you can control what is running where by adjusting the profile requirements. In this kind of setup the autoscaled profiles for dev, test and prod would point to the same profile with the actual implementation in it.

## Example 2

io.fabric8.autoscale/
* scaleContainers = true
* profilePattern = `^(my-template-.*)|(my-bundle-.*-test)$`
* containerPattern = ^camel.*$
* containerPrefix = camel
* maxContainersPerHost = 3
* averageInstancesPerContainer = 50
* defaultMaxInstancesPerHost = 1
* maxDeviation = 1.0
* inheritRequirements = true

This is a configuration we will probably use in our UAT environment. The autoscaler will consider profiles that match "my-template-" or "my-bundle-*-test" and assign them on containers starting with "camel".

We don't configure maximum instances per host in the profile requirements so the autoscaler will use the default value of 1.

We will only define the requirements for the profiles that we explicitly want to assign. Profile dependencies defined via dependsOn mechanism will inherit their requirements from their parents. This will keep the profile requirements DRY.
