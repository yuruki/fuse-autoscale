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

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;

class ContainerFactory {

    private final FabricService service;

    ContainerFactory(FabricService service) {
        this.service = service;
    }

    void createChildContainer(String name, String[] profiles, Container rootContainer) throws Exception {
        CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder();
        if (rootContainer == null) {
            throw new Exception("No root container available");
        }
        builder.parent(rootContainer.getId());
        builder.jmxUser(service.getZooKeeperUser()).jmxPassword(service.getZookeeperPassword()).zookeeperUrl(service.getZookeeperUrl()).zookeeperPassword(service.getZookeeperPassword());
        builder.number(1).version(rootContainer.getVersionId()).profiles(profiles);
        builder.name(name);
        if (service.getDefaultJvmOptions() != null && !service.getDefaultJvmOptions().isEmpty()) {
            builder.jvmOpts(service.getDefaultJvmOptions());
        }
        CreateChildContainerOptions options = builder.build();
        service.createContainers(options);
    }
}
