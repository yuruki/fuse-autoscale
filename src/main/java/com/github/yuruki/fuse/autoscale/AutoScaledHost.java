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

class AutoScaledHost extends ProfileContainer {

    private final Container rootContainer;

    AutoScaledHost(String id, Container rootContainer) {
        this.id = id;
        this.rootContainer = rootContainer;
        this.childComparator = new SortByProfileCount();
    }

    boolean hasRootContainer() {
        return rootContainer != null;
    }

    Container getRootContainer() {
        return rootContainer;
    }
}
