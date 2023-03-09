package com.ziqni.admin.stores;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Stores {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final MemberStore memberStore;

    public Stores(@NonNull ZiqniAdminApiFactory ziqniAdminApiFactory) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
        this.memberStore = new MemberStore(this.ziqniAdminApiFactory);
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public MemberStore getMemberCache() {
        return memberStore;
    }
}
