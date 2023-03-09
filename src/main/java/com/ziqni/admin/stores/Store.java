package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.EntityChanged;
import com.ziqni.admin.sdk.model.EntityStateChanged;
import com.ziqni.admin.sdk.model.ModelApiResponse;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class Store<T> implements AsyncCacheLoader<@NonNull String, @NonNull T>, RemovalListener<@NonNull String, @NonNull T> {

    public final static long DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS = 5L;
    public final static long DEFAULT_CACHE_MAXIMUM_SIZE = 10_000L;
    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher;

    protected final AsyncLoadingCache<@NonNull String, @NonNull T> cache;

    protected Store(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher, long expireMinutesAfterAccess, long maximumCacheSize) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
        this.ziqniSystemCallbackWatcher = ziqniSystemCallbackWatcher;
        this.cache = Caffeine
                .newBuilder()
                .maximumSize(maximumCacheSize)
                .expireAfterAccess(expireMinutesAfterAccess, TimeUnit.MINUTES)
                .evictionListener(this)
                .executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
                .buildAsync(this);
    }

    public void subscribeToEntityChanges(){
        this.ziqniSystemCallbackWatcher.subscribeToEntityChanges(getTypeClass());
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public abstract Class<T> getTypeClass();

    final public String getSimpleTypeClassName(){
        return getTypeClass().getSimpleName();
    }

    public ModelApiResponse handleCompletableFutureModelApiResponse(ModelApiResponse in){
        return in;
    }


}
