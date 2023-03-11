package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.exceptions.TooManyRecordsException;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.Achievement;
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


public class AchievementsStore extends Store<@NonNull Achievement>{


	private static final Logger logger = LoggerFactory.getLogger(AchievementsStore.class);

	public final AsyncLoadingCache<@NonNull String, @NonNull Achievement> cache = Caffeine
			.newBuilder()
			.expireAfterAccess(15, TimeUnit.MINUTES)
			.maximumSize(500)
			.evictionListener(this)
			.executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
			.buildAsync(this);

	public AchievementsStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
		super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher, DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS, DEFAULT_CACHE_MAXIMUM_SIZE);
	}

	@Override
	public Class<@NonNull Achievement> getTypeClass() {
		return Achievement.class;
	}


	/**
	 * Get methods
	 **/
	public CompletableFuture<Optional<Achievement>> getAchievement(String id) {
		return findAchievementById(id);
	}

	public CompletableFuture<Optional<Achievement>> findAchievementById(String id) {
		return cache.get(id).thenApply(Optional::ofNullable);
	}

	@Override
	public CompletableFuture<? extends Achievement> asyncLoad(@NonNull String key, Executor executor) throws Exception {
		return asyncLoadAll(Set.of(key), executor).thenApply(x->x.get(key));
	}

	@Override
	public CompletableFuture<? extends Map<? extends String, ? extends Achievement>> asyncLoadAll(@NonNull Set<? extends String> keys, Executor executor) throws Exception {
		TooManyRecordsException.Validate(20,0, keys.size());

		return getZiqniAdminApiFactory().getAchievementsApi().getAchievements(new ArrayList<>(keys), keys.size(), 0)
				.orTimeout(5, TimeUnit.SECONDS)
				.thenApply(response -> {
					Optional.ofNullable(response.getErrors()).ifPresent(e -> {
						if(!e.isEmpty())
							logger.error(e.toString());
					});

					if(response.getResults() != null) {
						return response.getResults().stream().collect(Collectors.toMap(Achievement::getId, x->x));
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
	public void onRemoval(@Nullable String key, @Nullable Achievement value, RemovalCause cause) {

	}
}
