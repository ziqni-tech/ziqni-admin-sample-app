package com.ziqni.admin.collections;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncConcurrentHashMap <K,V> extends ConcurrentHashMap<K,V> {

    private static <TOut> CompletableFuture<TOut> completableFutureWrapper(Supplier<TOut> supplier){
        final var out = new CompletableFuture<TOut>();
        try{
            out.complete(supplier.get());
        }catch (Throwable t){
            out.completeExceptionally(t);
        }
        return out;
    }

    @Override
    public V replace(K key, V value) {
        return super.replace(key, value);
    }

    public CompletableFuture<V> replaceAsync(K key, V value) {
        final var out = new CompletableFuture<V>();
        out.complete(super.replace(key, value));
        return out;
    }

    ////////

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return super.computeIfAbsent(key, mappingFunction);
    }

    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<K,CompletableFuture<V>> mappingFunction) {
        final var out = new CompletableFuture<V>();
        final var v = super.computeIfAbsent(key, k ->
                mappingFunction
                        .apply(k)
                        .orTimeout(50, TimeUnit.SECONDS)
                        .join()
        );
        out.complete(v);
        return out;
    }

    ////////

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.computeIfPresent(key, remappingFunction);
    }

    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<K,V,CompletableFuture<V>> mappingFunction) {
        final var out = new CompletableFuture<V>();
        final var v = super.computeIfPresent(key,(k, v1) -> mappingFunction
                .apply(k,v1)
                .orTimeout(50, TimeUnit.SECONDS)
                .join());
        out.complete(v);
        return out;
    }

    ////////

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.compute(key, remappingFunction);
    }

    public CompletableFuture<V> computeAsync(K key, BiFunction<K,V,CompletableFuture<V>> mappingFunction) {
        final var out = new CompletableFuture<V>();
        final var v = super.computeIfPresent(key,(k, v1) -> mappingFunction
                .apply(k,v1)
                .orTimeout(50, TimeUnit.SECONDS)
                .join());
        out.complete(v);
        return out;
    }
}