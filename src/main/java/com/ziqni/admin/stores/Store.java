package com.ziqni.admin.stores;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;

public abstract class Store {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;

    protected Store(ZiqniAdminApiFactory ziqniAdminApiFactory) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }
}
