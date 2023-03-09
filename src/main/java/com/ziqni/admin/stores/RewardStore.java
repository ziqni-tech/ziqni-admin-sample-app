package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.Reward;
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

public class RewardStore extends Store implements AsyncCacheLoader<@NonNull String, @NonNull Reward>, RemovalListener<@NonNull String, @NonNull Reward> {

    private static final Logger logger = LoggerFactory.getLogger(RewardStore.class);

    public final AsyncLoadingCache<@NonNull String, @NonNull Reward> cache = Caffeine
            .newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .evictionListener(this)
            .executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
            .buildAsync(this);

    public RewardStore(ZiqniAdminApiFactory ziqniAdminApiFactory) {
        super(ziqniAdminApiFactory);
    }

    public CompletableFuture<Optional<Reward>> getBasicReward(String id) {
        return getReward(id);
    }
    public CompletableFuture<Optional<Reward>> getReward(String id) {
        return cache.get(id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<? extends @NonNull Reward> asyncLoad(@NonNull String key, Executor executor) throws Exception {
        return asyncLoadAll(Set.of(key), executor).thenApply(x->x.get(key));
    }

    @Override
    public CompletableFuture<? extends Map<? extends @NonNull String, ? extends @NonNull Reward>> asyncLoadAll(Set<? extends @NonNull String> keys, Executor executor) throws Exception {
        return getZiqniAdminApiFactory().getRewardsApi().getRewards(new ArrayList<>(keys), 1, 0)
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    Optional.ofNullable(response.getErrors()).ifPresent(e -> {
                        if(!e.isEmpty())
                            logger.error(e.toString());
                    });

                    if(response.getResults() != null) {
                        return response.getResults().stream().collect(Collectors.toMap(Reward::getId, x->x));
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
    public void onRemoval(@Nullable @NonNull String key, @Nullable @NonNull Reward value, RemovalCause cause) {

    }
}
