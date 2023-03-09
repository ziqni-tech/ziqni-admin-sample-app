package com.ziqni.admin.cache;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Caches {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final MemberCache memberCache;

    public Caches(@NonNull ZiqniAdminApiFactory ziqniAdminApiFactory) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;

        this.memberCache = new MemberCache(this.ziqniAdminApiFactory);
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public MemberCache getMemberCache() {
        return memberCache;
    }
}
