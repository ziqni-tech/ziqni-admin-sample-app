package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.model.Contest;
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

public class ContestsStore extends Store implements AsyncCacheLoader<@NonNull String, @NonNull Contest>, RemovalListener<@NonNull String, @NonNull Contest> {

	private static final Logger logger = LoggerFactory.getLogger(ContestsStore.class);

	public final AsyncLoadingCache<@NonNull String, @NonNull Contest> cache = Caffeine
			.newBuilder()
			.maximumSize(1_000)
			.expireAfterAccess(15, TimeUnit.MINUTES)
			.evictionListener(this)
			.executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
			.buildAsync(this);

	public ContestsStore(ZiqniAdminApiFactory ziqniAdminApiFactory) {
		super(ziqniAdminApiFactory);
	}


	/** Get methods **/
	public CompletableFuture<Optional<Contest>> getBasicContestModel(String id) {
		return getContest(id);
	}

	/** Get methods **/
	public CompletableFuture<Optional<Contest>> getContest(String id) {
		return cache.get(id).thenApply(Optional::ofNullable);
	}
	@Override
	public CompletableFuture<? extends Contest> asyncLoad(@NonNull String key, Executor executor) throws Exception {
		return asyncLoadAll(Set.of(key), executor).thenApply(x->x.get(key));
	}

	@Override
	public CompletableFuture<? extends Map<? extends @NonNull String, ? extends @NonNull Contest>> asyncLoadAll(Set<? extends @NonNull String> keys, Executor executor) throws Exception {
		return getZiqniAdminApiFactory().getContestsApi().getContests(new ArrayList<>(keys), 1, 0)
				.orTimeout(5, TimeUnit.SECONDS)
				.thenApply(response -> {
					Optional.ofNullable(response.getErrors()).ifPresent(e -> {
						if(!e.isEmpty())
							logger.error(e.toString());
					});

					if(response.getResults() != null) {
						return response.getResults().stream().collect(Collectors.toMap(Contest::getId, x->x));
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
	public void onRemoval(@Nullable String key, @Nullable Contest value, RemovalCause cause) {

	}
}
