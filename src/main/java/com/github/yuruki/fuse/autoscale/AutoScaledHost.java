package com.github.yuruki.fuse.autoscale;

import io.fabric8.api.Container;

public class AutoScaledHost extends ProfileContainer {

    private final Container rootContainer;

    public AutoScaledHost(String id, Container rootContainer) {
        this.id = id;
        this.rootContainer = rootContainer;
        this.childComparator = new SortByProfileCount();
    }

    public boolean hasRootContainer() {
        return rootContainer != null;
    }

    public Container getRootContainer() {
        return rootContainer;
    }
}
