package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.exceptions.TooManyRecordsException;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.Award;
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

public class AwardStore extends Store<@NonNull Award> {

    private static final Logger logger = LoggerFactory.getLogger(AwardStore.class);

    public final AsyncLoadingCache<@NonNull String, @NonNull Award> cache = Caffeine
            .newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .evictionListener(this)
            .executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
            .buildAsync(this);

    public AwardStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
        super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher, DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS, DEFAULT_CACHE_MAXIMUM_SIZE);
    }

    @Override
    public Class<@NonNull Award> getTypeClass() {
        return Award.class;
    }

    public CompletableFuture<Optional<Award>> getAward(String id){
        return cache.get(id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<? extends @NonNull Award> asyncLoad(@NonNull String key, Executor executor) throws Exception {
        return asyncLoadAll(Set.of(key), executor).thenApply(x->x.get(key));
    }

    @Override
    public CompletableFuture<? extends Map<? extends @NonNull String, ? extends @NonNull Award>> asyncLoadAll(Set<? extends @NonNull String> keys, Executor executor) throws Exception {
        TooManyRecordsException.Validate(20,0, keys.size());

        return getZiqniAdminApiFactory().getAwardsApi().getAwards(new ArrayList<>(keys), 1, 0)
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    Optional.ofNullable(response.getErrors()).ifPresent(e -> {
                        if(!e.isEmpty())
                            logger.error(e.toString());
                    });

                    if(response.getResults() != null) {
                        return response.getResults().stream().collect(Collectors.toMap(Award::getId, x->x));
                    }
                    else
                        return null;

                })
                .exceptionally(throwable -> {
                    logger.error("Exception occurred while attempting to get product", throwable);
                    return null;
                });
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable Award value, RemovalCause cause) {

    }
}
