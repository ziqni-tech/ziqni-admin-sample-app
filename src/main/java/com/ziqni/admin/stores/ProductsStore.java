package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.*;
import com.ziqni.admin.collections.AsyncConcurrentHashMap;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.api.ProductsApiWs;
import com.ziqni.admin.sdk.model.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ProductsStore extends Store implements AsyncCacheLoader<@NonNull String, @NonNull Product>, RemovalListener<@NonNull String, @NonNull Product> {

	private static final Logger logger = LoggerFactory.getLogger(ProductsStore.class);
	private  final ProductsApiWs api;
	private static final AsyncConcurrentHashMap<String, String> refIdCache = new AsyncConcurrentHashMap<>();
	public final AsyncLoadingCache<@NonNull String, @NonNull Product> cache;
	public ProductsStore(ZiqniAdminApiFactory ziqniAdminApiFactory) {
		super(ziqniAdminApiFactory);
		api = ziqniAdminApiFactory.getProductsApi();
		cache = Caffeine
				.newBuilder()
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.maximumSize(10_000)
				.evictionListener(this)
				.executor(ZiqniExecutors.GlobalZiqniCachesExecutor)
				.buildAsync(this);
	}


	private CompletableFuture<Product> cacheSetter(Product product){
		var fut = new CompletableFuture<Product>();
		fut.complete(product);
		return fut;
	}
	/**
	 * Get methods
	 **/
	public CompletableFuture<Optional<String>> getIdByReferenceId(String productRefId) {
		return refIdCache.computeIfAbsentAsync(
				productRefId,
				rid ->
						api.getProductsByRefId(List.of(productRefId), 1, 0)
								.orTimeout(5, TimeUnit.SECONDS)
								.thenApply(productResponse -> {
									Optional.ofNullable(productResponse.getErrors()).ifPresent(e -> {
										if(!e.isEmpty())
											logger.error(e.toString());
									});

									return   Optional.ofNullable(productResponse.getResults()).flatMap(members ->
											members.stream()
													.filter(x->x.getProductRefId().equals(productRefId))
													.findFirst().map(product -> {
														this.cache.put(product.getId(),cacheSetter(product));
														return product.getId();
													})
									).orElse(null);
								})
		).thenApply(Optional::ofNullable);
	}

	public CompletableFuture<String> getRefIdByProductId(String productId) {
		return cache.get(productId).thenApply(Product::getProductRefId);
	}

	public CompletableFuture<Optional<Product>> findProductModelById(String productId) {
		return cache.get(productId).thenApply(Optional::ofNullable);
	}

	public CompletableFuture<Product> findProductById(String productId) {
		return cache.get(productId);
	}

	/**
	 * Create
	 **/

//	public CompletableFuture<Optional<String>> create(String productRefId, String displayName, Seq<String> providers, String productType, Double defaultAdjustmentFactor, Option<scala.collection.Map<String, String>> metaData) {
//		return refIdCache.computeIfAbsentAsync(
//				productRefId,
//				ridtoScala -> {
//					var meta = setMetadata(metaData);
//					var tags = setTags(providers);
//					var productToCreate = new CreateProductRequest()
//							.productRefId(productRefId)
//							.name(displayName)
//							.adjustmentFactor(defaultAdjustmentFactor)
//							.tags(tags)
//							.metadata(meta);
//
//					return api.createProducts(List.of(productToCreate))
//							.orTimeout(5, TimeUnit.SECONDS)
//							.thenApply(modelApiResponse -> {
//
//								Optional.ofNullable(modelApiResponse.getErrors()).ifPresent(e -> {
//									if (!e.isEmpty())
//										logger.error(e.toString());
//								});
//
//								return Optional.ofNullable(modelApiResponse.getResults()).flatMap(results -> results
//										.stream()
//										.filter(x -> x.getExternalReference().equals(productRefId))
//										.findFirst()
//								);
//							}).thenApply(c->
//								c.map(Result::getId).orElse(null)
//							);
//				}
//		).thenApply(Optional::ofNullable);
//	}
//
//
//	/**
//	 * Update
//	 **/
//	public CompletableFuture<Optional<Result>> update(String productId, Option<String> productRefId, Option<String> displayName, Option<Seq<String>> providers, Option<String> productType, Option<Double> defaultAdjustmentFactor, Option<scala.collection.Map<String, String>> metaData) {
//
//		final var tags = setTags(providers.getOrElse(null));
//		final var meta = setMetadata(metaData);
//
//		return cache.get(productId)
//				.thenApply(Optional::ofNullable)
//				.thenApply(member -> member.map(m -> compareMember(m, productId,productRefId,displayName,tags,setMetadata(metaData))).orElse(false))
//				.thenCompose(same -> {
//					if(same){
//						final var nochange = new CompletableFuture<Optional<Result>>();
//						nochange.complete(Optional.empty());
//						return nochange;
//					}
//					else {
//
//						var toUpdate = new UpdateProductRequest()
//								.id(productId)
//								.name(displayName.getOrElse(null))
//								.tags(tags)
//								.adjustmentFactor(defaultAdjustmentFactor.getOrElse(null))
//								.metadata(meta);
//
//						return api.updateProducts(List.of(toUpdate))
//								.thenApply(modelApiResponse -> {
//									Optional.ofNullable(modelApiResponse.getErrors()).ifPresent(e -> {
//										if(!e.isEmpty())
//											logger.error(e.toString());
//									});
//									return Optional.ofNullable(modelApiResponse.getResults()).flatMap(gh -> gh.stream().filter(gh1->gh1.getId().equals(productId)).findFirst());
//								})
//								.exceptionally(throwable -> {
//									logger.error("Exception occurred while attempting to update product", throwable);
//									return null;
//								});
//					}
//				}).exceptionally(throwable -> {
//					logger.error("Exception occurred while attempting to update product", throwable);
//					final var out = new CompletableFuture<ModelApiResponse>();
//					out.completeExceptionally(throwable);
//					return null;
//				});
//	}


	/**
	 * Delete
	 **/
	public CompletableFuture<Optional<Result>> delete(String productId) {
		return api.deleteProducts(List.of(productId))
				.orTimeout(5, TimeUnit.SECONDS)
				.thenApply(modelApiResponse ->
					Optional.ofNullable(modelApiResponse.getResults()).flatMap(results -> results.stream()
							.filter(x->x.getId().equalsIgnoreCase(productId))
							.findFirst().map(result -> {
								Optional.ofNullable(result.getExternalReference()).map(refIdCache::remove);
								return result;
							})));

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
	public CompletableFuture<? extends Product> asyncLoad(@NonNull String key, Executor executor) throws Exception {
		return asyncLoadAll(Set.of(key),executor).thenApply(x->x.get(key));
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
	public CompletableFuture<? extends Map<? extends String, ? extends Product>> asyncLoadAll(Set<? extends String> keys, Executor executor) throws Exception {
			return api.getProducts(new ArrayList<>(keys), 1, 0)
					.orTimeout(5, TimeUnit.SECONDS)
					.thenApply(response -> {
						Optional.ofNullable(response.getErrors()).ifPresent(e -> {
							if(!e.isEmpty())
								logger.error(e.toString());
						});

						if(response.getResults() != null) {
							response.getResults().forEach(item ->
									refIdCache.put(item.getProductRefId(), item.getId())
							);

							return response.getResults().stream().collect(Collectors.toMap(Product::getId,x->x));
						}
						else
							return null;

					})
					.exceptionally(throwable -> {
						logger.error("Exception occurred while attempting to get product", throwable);
						return null;
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
	public void onRemoval(@Nullable String key, @Nullable Product value, RemovalCause cause) {
		if(value != null) {
			refIdCache.computeIfPresent(value.getProductRefId(), (s, s2) -> null);
			logger.debug("Removing product {} [ ID:{} | REF:{} ]", value.getName(), value.getId(), value.getProductRefId());
		}
	}
}
