package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.ziqni.admin.collections.AsyncConcurrentHashMap;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.Member;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemberStore extends Store<@NonNull String, @NonNull Member> {

    private static final Logger logger = LoggerFactory.getLogger(MemberStore.class);
    private static final AsyncConcurrentHashMap<String, String> refIdCache = new AsyncConcurrentHashMap<>();


    public MemberStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
        super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher);
    }

    @Override
    public Class<@NonNull Member> getTypeClass() {
        return Member.class;
    }


    @Override
    public CompletableFuture<? extends Member> asyncLoad(@NonNull String key, Executor executor) throws Exception {
        return asyncLoadAll(Set.of(key),executor).thenApply(map -> map.get(key));
    }

    @Override
    public CompletableFuture<? extends Map<? extends String, ? extends Member>> asyncLoadAll(Set<? extends String> keys, Executor executor) throws Exception {

        return getZiqniAdminApiFactory().getMembersApi().getMembers(new ArrayList<>(keys), keys.size(), 0)
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {

                    Optional.ofNullable(response.getErrors()).ifPresent(e -> {
                        if(!e.isEmpty())
                            logger.error(e.toString());
                    });

                    return Optional.ofNullable(response.getResults()).map(results ->
                            results.stream().map(member -> {
                                refIdCache.compute(member.getMemberRefId(),(s, s2) ->  member.getId());
                                return member;

                            }).collect(Collectors.toMap(Member::getId, x->x))
                    ).orElse(Map.of());
                });
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable Member value, RemovalCause cause) {
        if(value != null) {
            refIdCache.computeIfPresent(value.getMemberRefId(), (s, s2) -> null);
            logger.debug("Removing member {} [ ID:{} | REF:{} ]", value.getName(), value.getId(), value.getMemberRefId());
        }
    }
}
