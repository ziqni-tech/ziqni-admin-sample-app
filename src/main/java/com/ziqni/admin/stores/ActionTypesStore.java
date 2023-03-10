package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.collections.Tuple;
import com.ziqni.admin.concurrent.QueueJob;
import com.ziqni.admin.concurrent.ZiqniExecutors;
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

public class ActionTypesStore extends Store<ActionType> {

	private static final Logger logger = LoggerFactory.getLogger(ActionTypesStore.class);

	public final AsyncLoadingCache<String, ActionType> cache = Caffeine
			.newBuilder()
			.maximumSize(10_000)
			.expireAfterAccess(1, TimeUnit.DAYS)
			.executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
			.evictionListener(this).buildAsync(this);

	public ActionTypesStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
		super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher, DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS, DEFAULT_CACHE_MAXIMUM_SIZE);
	}



	@Override
	public Class<@NonNull ActionType> getTypeClass() {
		return ActionType.class;
	}

	/**
	 * Get methods
	 **/
	public CompletableFuture<Boolean> actionTypeExists(String action) {
		return getAction(action).thenApply(Objects::nonNull);
	}

	public CompletableFuture<ActionType> getAction(String action) {
		return cache.get(action);
	}

	public CompletableFuture<Integer> start(){
		return loadActions(0).thenApply(x->x.getMeta().getTotalRecords());
	}

	private CompletableFuture<ActionTypeResponse> loadActions(Integer skip) {
		var request = new QueryRequest().skip(skip).limit(100);
		return getZiqniAdminApiFactory().getActionTypesApi().getActionTypesByQuery(request)
				.thenCompose(actionTypeResponse -> {
					if(actionTypeResponse.getResults() != null && actionTypeResponse.getResults().size() > 0 &&  actionTypeResponse.getMeta().getTotalRecords() > actionTypeResponse.getResults().size())
						return loadActions(skip+20);
					else {
						final var out = new CompletableFuture<ActionTypeResponse>();
						out.complete(actionTypeResponse);
						return out;
					}
				});
	}

	/**
	 * Create
	 **/
	public CompletableFuture<Optional<Result>> create(final String actionKey, String name, String unitOfMeasureKey) {
		return QueueJob.Submit(ZiqniExecutors.StoresSingleThreadedExecutor, () -> {
//			var meta = setMetadata(metaData);
			var toCreate=  new CreateActionTypeRequest()
					.key(actionKey)
					.name(Objects.nonNull(name)?name:actionKey)
//					.metadata(meta)
					.unitOfMeasure(unitOfMeasureKey);

			return getZiqniAdminApiFactory().getActionTypesApi().createActionTypes(List.of(toCreate))
					.orTimeout(5, TimeUnit.SECONDS)
					.thenApply(modelApiResponse -> {

						Optional.ofNullable(modelApiResponse.getErrors()).ifPresent(e -> {
							if (!e.isEmpty())
								logger.debug(e.toString());
						});

						return Optional.ofNullable(modelApiResponse.getResults()).flatMap(results -> results
								.stream()
								.filter(x -> x.getExternalReference().equals(actionKey))
								.findFirst().map(result -> {
									put( new ActionType()
											.key(actionKey)
											.id(result.getId())
											.name(toCreate.getName())
									);
									return result;
								})
						);
					});
		});
	}

	public void put(ActionType actionType){
		final var fut = new CompletableFuture<ActionType>();
		fut.complete(actionType);
		cache.put(actionType.getKey(), fut);
	}

	/**
	 * Update
	 **/
	public CompletableFuture<ModelApiResponse> update(String action, String name, String unitOfMeasureType) {

		final CompletableFuture<Optional<UpdateAction>> search = cache.get(action)
				.thenApply(Optional::ofNullable)
				.thenApply(actionType -> actionType.map( found -> {

//					var meta = setMetadata(metaData);
					return new UpdateAction(found, new UpdateActionTypeRequest()
							.id(found.getId())
							.name(Objects.nonNull(name) ? name : action )
							.unitOfMeasure(Objects.nonNull(unitOfMeasureType)
									? UnitOfMeasureType.valueOf(unitOfMeasureType).getValue()
									: UnitOfMeasureType.OTHER.getValue()
							)
//							.metadata(meta)
					);

				})).handle((updateAction, throwable) -> {
					if(throwable != null){
						logger.error("Exception occurred while attempting to update action type", throwable);
						return Optional.empty();
					}
					else {
						return updateAction;
					}
				});

		return search.thenCompose(updateAction -> {
			if(updateAction.isPresent()){
				return getZiqniAdminApiFactory().getActionTypesApi().updateActionTypes(List.of(updateAction.get().two)).orTimeout(5, TimeUnit.SECONDS)
						.thenApply(modelApiResponse -> {

							var r1 = Optional.ofNullable(modelApiResponse.getResults()).flatMap(results -> {
								final var out = results.stream()
										.filter(x -> x.getExternalReference() != null && x.getExternalReference().equals(action))
										.map(result -> {
											put(updateAction.get().one
													.name(updateAction.get().two.getName())
													.id(result.getId())
											);
											return result;
										}).findFirst();
								return out;
							});

							return modelApiResponse;
						});
			}
			else {
				final var oops = new CompletableFuture<ModelApiResponse>();
				oops.completeExceptionally(new Throwable("Not found"));
				return oops;
			}
		});
	}

	/**
	 * Asynchronously computes or retrieves the value corresponding to {@code key}.
	 *
	 * @param key      the non-null key whose value should be loaded
	 * @param executor the executor with which the entry is asynchronously loaded
	 * @return the future value associated with {@code key}
	 * @throws Exception            or Error, in which case the mapping is unchanged
	 * @throws InterruptedException if this method is interrupted. {@code InterruptedException} is
	 *                              treated like any other {@code Exception} in all respects except that, when it is
	 *                              caught, the thread's interrupt status is set
	 */
	@Override
	public CompletableFuture<? extends ActionType> asyncLoad(@NonNull String key, Executor executor) throws Exception {
		return asyncLoadAll(Set.of(key),executor).thenApply(map -> map.get(key));
	}

	/**
	 * Asynchronously computes or retrieves the values corresponding to {@code keys}. This method is
	 * called by {@link AsyncLoadingCache#getAll}.
	 * <p>
	 * If the returned map doesn't contain all requested {@code keys} then the entries it does contain
	 * will be cached and {@code getAll} will return the partial results. If the returned map contains
	 * extra keys not present in {@code keys} then all returned entries will be cached, but only the
	 * entries for {@code keys} will be returned from {@code getAll}.
	 * <p>
	 * This method should be overridden when bulk retrieval is significantly more efficient than many
	 * individual lookups. Note that {@link AsyncLoadingCache#getAll} will defer to individual calls
	 * to {@link AsyncLoadingCache#get} if this method is not overridden.
	 *
	 * @param keys     the unique, non-null keys whose values should be loaded
	 * @param executor the executor with which the entries are asynchronously loaded
	 * @return a future containing the map from each key in {@code keys} to the value associated with
	 * that key; <b>may not contain null values</b>
	 * @throws Exception            or Error, in which case the mappings are unchanged
	 * @throws InterruptedException if this method is interrupted. {@code InterruptedException} is
	 *                              treated like any other {@code Exception} in all respects except that, when it is
	 *                              caught, the thread's interrupt status is set
	 */
	@Override
	public CompletableFuture<? extends Map<? extends @NonNull String, ? extends ActionType>> asyncLoadAll(Set<? extends @NonNull String> keys, Executor executor) throws Exception {
		TooManyRecordsException.Validate(20,0, keys.size());

		final var query = new QueryRequest()
				.addShouldItem(new QueryMultiple().queryField(ActionType.JSON_PROPERTY_KEY).queryValues(new ArrayList<>(keys)))
				.shouldMatch(1)
				.skip(0)
				.limit(keys.size()
				);

		return getZiqniAdminApiFactory().getActionTypesApi().getActionTypesByQuery(query)
				.orTimeout(5, TimeUnit.SECONDS)
				.thenApply(actionTypeResponse -> {

					Optional.ofNullable(actionTypeResponse.getErrors()).ifPresent(e -> {
						if(!e.isEmpty())
							logger.error(e.toString());
					});

					return Optional.ofNullable(actionTypeResponse.getResults())
							.map(a-> a.stream().collect(Collectors.toMap(ActionType::getKey, x->x)))
							.orElse(null);
				});

	}

	/**
	 * Notifies the listener that a removal occurred at some point in the past.
	 * <p>
	 * This does not always signify that the key is now absent from the cache, as it may have already
	 * been re-added.
	 *
	 * @param key   the key represented by this entry, or {@code null} if collected
	 * @param value the value represented by this entry, or {@code null} if collected
	 * @param cause the reason for which the entry was removed
	 */
	@Override
	public void onRemoval(@Nullable String key, ActionType value, RemovalCause cause) {

	}

	private static class UpdateAction extends Tuple<ActionType,UpdateActionTypeRequest> {

		public UpdateAction(ActionType one, UpdateActionTypeRequest two) {
			super(one, two);
		}
	}
}
