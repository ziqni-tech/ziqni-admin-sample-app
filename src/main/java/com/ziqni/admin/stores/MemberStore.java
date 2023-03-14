package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.ziqni.admin.collections.AsyncConcurrentHashMap;
import com.ziqni.admin.exceptions.TooManyRecordsException;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.*;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemberStore extends Store<@NonNull Member> {

    private static final Logger logger = LoggerFactory.getLogger(MemberStore.class);
    private static final AsyncConcurrentHashMap<String, String> idCache = new AsyncConcurrentHashMap<>();

    public MemberStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
        super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher, DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS, DEFAULT_CACHE_MAXIMUM_SIZE);
    }

    public CompletableFuture<ModelApiResponse> createMember(CreateMemberRequest member){
        return this.getZiqniAdminApiFactory().getMembersApi()
                .createMembers(List.of(member))
                .thenApply(super::handleCompletableFutureModelApiResponse);
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
        TooManyRecordsException.Validate(20,0, keys.size());

        final var query = new QueryRequest()
                .addShouldItem(new QueryMultiple().queryField(Member.JSON_PROPERTY_MEMBER_REF_ID).queryValues(new ArrayList<>(keys)))
                .shouldMatch(1)
                .skip(0)
                .limit(keys.size()
                );

        return getZiqniAdminApiFactory().getMembersApi().getMembersByQuery(query)
                .thenApply(response -> {

                    Optional.ofNullable(response.getErrors()).ifPresent(e -> {
                        if(!e.isEmpty())
                            logger.error(e.toString());
                    });

                    var out =  Optional.ofNullable(response.getResults()).map(results ->
                            results.stream().peek(member -> idCache.compute(member.getId(),(s, s2) ->  member.getMemberRefId())).collect(Collectors.toMap(Member::getMemberRefId, x->x))
                    ).orElse(Map.of());

                    return out;
                });
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable Member value, RemovalCause cause) {
        if(value != null) {
            idCache.computeIfPresent(value.getId(), (s, s2) -> null);
            logger.debug("Removing member {} [ ID:{} | REF:{} ]", value.getName(), value.getId(), value.getMemberRefId());
        }
    }

    public CompletableFuture<@NonNull Member> getMemberByRefId(String memberRefId) {
        return this.cache.get(memberRefId);
    }
}
