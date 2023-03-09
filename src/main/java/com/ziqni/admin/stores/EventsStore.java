package com.ziqni.admin.stores;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ziqni.admin.concurrent.QueueJob;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.api.EventsApiWs;
import com.ziqni.admin.sdk.model.Event;
import com.ziqni.admin.sdk.model.ModelApiResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EventsStore extends Store implements CacheLoader<@NonNull String, EventsStore.EventTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(EventsStore.class);

    private final LoadingCache<@NonNull String, @NonNull EventTransaction> cache;

    private final EventsApiWs api;

    public EventsStore(ZiqniAdminApiFactory ziqniAdminApiFactory) {
        super(ziqniAdminApiFactory);
        this.api = ziqniAdminApiFactory.getEventsApi();
        cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build(this::cacheLoader);
    }

    public CompletableFuture<ModelApiResponse> pushEvent(Event basicEventModel) {
        return pushEvent(List.of(basicEventModel));
    }

    public CompletableFuture<ModelApiResponse> pushEvent(List<Event> events) {
        try {
            return api.createEvents(events)
                    .thenApply(modelApiResponse -> {
                        if(Objects.nonNull(modelApiResponse.getErrors())){
                            for(com.ziqni.admin.sdk.model.Error error: modelApiResponse.getErrors()){
                                logger.error(error.toString());
                            }
                        }
                        return modelApiResponse;
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to push event", throwable);
                        return null;
                    });

        } catch (Exception ex) {
            logger.error("Exception occurred while attempting to create events", ex);
            final var oops = new CompletableFuture<ModelApiResponse>();
            oops.completeExceptionally(ex);
            return oops;
        }
    }

    public CompletableFuture<ModelApiResponse> pushEventTransaction(Event basicEventModel) {
        return QueueJob.Submit(
                ZiqniExecutors.EventStoreSingleThreadedExecutor,
                () -> {
                    basicEventModel.batchId().map(v1 ->
                            Objects.requireNonNull(cache.get(v1)).addBasicEvent(basicEventModel)
                    );
                    return pushEvent(basicEventModel);
                }
        );
    }

    public CompletableFuture<List<Event>> findByBatchId(String batchId) {
        return QueueJob.Submit(
                ZiqniExecutors.EventStoreSingleThreadedExecutor,
                () -> {
                    final var out = new CompletableFuture<List<Event>>();
                    out.complete(Objects.requireNonNull(cache.get(batchId)).buffer);
                    return out;
                }
        );
    }




    private EventTransaction cacheLoader(String cacheKey) {
        return new EventTransaction();
    }

    @Override
    public @Nullable EventTransaction load(String key) throws Exception {
        return new EventTransaction();
    }


    public static class EventTransaction {
        private final List<Event> buffer = new ArrayList<>();

        public boolean addBasicEvent(Event e) {
            return buffer.add(e);
        }

        public List<Event> getEvents() {
            return buffer;
        }
    }
}
