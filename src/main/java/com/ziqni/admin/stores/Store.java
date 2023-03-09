package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

public abstract class Store<TKey,T> implements AsyncCacheLoader<@NonNull TKey, @NonNull T>, RemovalListener<@NonNull TKey, @NonNull T> {

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher;

    public final AsyncLoadingCache<@NonNull TKey, @NonNull T> cache;

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

    protected Store(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
        this(ziqniAdminApiFactory,ziqniSystemCallbackWatcher,15, 10_000);
    }

    public void init(){
        this.ziqniSystemCallbackWatcher.subscribeToEntityChanges(getTypeClass());
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    public abstract Class<T> getTypeClass();

    final public String getSimpleTypeClassName(){
        return getTypeClass().getSimpleName();
    }
}
