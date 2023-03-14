package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
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

public class UnitsOfMeasureStore extends Store<@NonNull UnitOfMeasure> {

	private static final Logger logger = LoggerFactory.getLogger(UnitsOfMeasureStore.class);

	public final AsyncLoadingCache<@NonNull String, @NonNull UnitOfMeasure> cache = Caffeine
			.newBuilder()
			.maximumSize(1_000)
			.expireAfterAccess(1, TimeUnit.DAYS)
			.evictionListener(this)
			.executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
			.buildAsync(this);

	public UnitsOfMeasureStore(ZiqniAdminApiFactory ziqniAdminApiFactory, ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher) {
		super(ziqniAdminApiFactory,ziqniSystemCallbackWatcher, DEFAULT_CACHE_EXPIRE_MINUTES_AFTER_ACCESS, DEFAULT_CACHE_MAXIMUM_SIZE);
	}

	/**
	 * Get methods
	 **/
	public CompletableFuture<Boolean> unitOfMeasureExists(String uom) {
		return getUnitOfMeasure(uom).thenApply(Optional::isPresent);
	}

	public CompletableFuture<Optional<UnitOfMeasure>> getUnitOfMeasure(String uom) {
		return cache.get(uom).thenApply(Optional::ofNullable);
	}

	public CompletableFuture<Optional<Double>> getUnitOfMeasureMultiplier(String uom) {
		return cache.get(uom).thenApply(Optional::ofNullable).thenApply(unitOfMeasure -> unitOfMeasure.map(UnitOfMeasure::getMultiplier));
	}

	public CompletableFuture<Integer> start(){
		return loadUnitsOfMeasure(0).thenApply(x->x.getMeta().getTotalRecords());
	}

	private CompletableFuture<UnitOfMeasureResponse> loadUnitsOfMeasure(Integer skip) {
		var request = new QueryRequest().skip(skip).limit(100);
		return getZiqniAdminApiFactory().getUnitsOfMeasureApi().getUnitsOfMeasureByQuery(request)
				.thenCompose(unitOfMeasureResponse -> {
					if(unitOfMeasureResponse.getResults() != null && unitOfMeasureResponse.getResults().size() > 0 &&  unitOfMeasureResponse.getMeta().getTotalRecords() > unitOfMeasureResponse.getResults().size())
						return loadUnitsOfMeasure(skip+20);
					else {
						final var out = new CompletableFuture<UnitOfMeasureResponse>();
						out.complete(unitOfMeasureResponse);
						return out;
					}
				});
	}

	/**
	 * Create
	 **/
	public CompletableFuture<Optional<Result>> create(@NonNull final String key, String name, String isoCode, Double multiplier, UnitOfMeasureType unitOfMeasureType) {
		return QueueJob.Submit(
				ZiqniExecutors.StoresSingleThreadedExecutor,
				() -> {
					var toCreate=  new CreateUnitOfMeasureRequest()
							.key(key)
							.name(Objects.nonNull(name)?name:key)
							.description("")
							.multiplier(multiplier)
							.unitOfMeasureType(unitOfMeasureType);

					return getZiqniAdminApiFactory().getUnitsOfMeasureApi().createUnitsOfMeasure(List.of(toCreate))
							
							.thenApply(modelApiResponse -> {

								Optional.ofNullable(modelApiResponse.getErrors()).ifPresent(e -> {
									if (!e.isEmpty())
										logger.error(e.toString());
								});

								return Optional.ofNullable(modelApiResponse.getResults()).flatMap(results -> results
										.stream()
										.filter(x -> x.getExternalReference().equals(key))
										.findFirst().map(result -> {
											put(new UnitOfMeasure()
													.id(result.getId())
													.key(result.getExternalReference())
													.name(Objects.nonNull(name)?name:key)
													.description("")
													.multiplier(multiplier)
													.unitOfMeasureType(unitOfMeasureType)
											);
											return result;
										})
								);
							});
				});
	}

	public void put(UnitOfMeasure unitOfMeasure){
		final var fut = new CompletableFuture<UnitOfMeasure>();
		fut.complete(unitOfMeasure);
		cache.put(unitOfMeasure.getKey(), fut);
	}

	@Override
	public Class<@NonNull UnitOfMeasure> getTypeClass() {
		return UnitOfMeasure.class;
	}

	@Override
	public CompletableFuture<? extends UnitOfMeasure> asyncLoad(String key, Executor executor) throws Exception {
		return asyncLoadAll(Set.of(key),executor).thenApply(map -> map.get(key));
	}

	@Override
	public CompletableFuture<? extends Map<? extends String, ? extends UnitOfMeasure>> asyncLoadAll(Set<? extends String> keys, Executor executor) throws Exception {
		TooManyRecordsException.Validate(20,0, keys.size());

		final var query = new QueryRequest()
				.addShouldItem(new QueryMultiple().queryField(ActionType.JSON_PROPERTY_KEY).queryValues(new ArrayList<>(keys)))
				.shouldMatch(1)
				.skip(0)
				.limit(keys.size()
				);

		return getZiqniAdminApiFactory().getUnitsOfMeasureApi().getUnitsOfMeasureByQuery(query)
				
				.thenApply(unitOfMeasureResponse -> {

					Optional.ofNullable(unitOfMeasureResponse.getErrors()).ifPresent(e -> {
						if(!e.isEmpty())
							logger.error(e.toString());
					});

					return Optional.ofNullable(unitOfMeasureResponse.getResults())
							.map(a-> a.stream().collect(Collectors.toMap(UnitOfMeasure::getKey, x->x)))
							.orElse(null);
				});

	}

	@Override
	public void onRemoval(@Nullable String key, UnitOfMeasure value, RemovalCause cause) {

	}
}
