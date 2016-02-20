package com.github.yuruki.fuse.autoscale;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;

public class ContainerFactory {

    private final FabricService service;

    public ContainerFactory(FabricService service) {
        this.service = service;
    }

    public void createChildContainer(String name, String[] profiles, Container rootContainer) throws Exception {
        CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder();
        if (rootContainer == null) {
            throw new Exception("No root container available");
        }
        builder.parent(rootContainer.getId());
        builder.jmxUser(service.getZooKeeperUser()).jmxPassword(service.getZookeeperPassword()).zookeeperUrl(service.getZookeeperUrl()).zookeeperPassword(service.getZookeeperPassword());
        builder.number(1).version(rootContainer.getVersionId()).profiles(profiles);
        builder.name(name);
        CreateChildContainerOptions options = builder.build();
        service.createContainers(options);
    }
}
