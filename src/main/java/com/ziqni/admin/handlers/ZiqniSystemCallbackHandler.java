package com.ziqni.admin.handlers;

import com.ziqni.admin.bus.ZiqniEventBus;
import com.ziqni.admin.sdk.ApiException;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.api.EntityChangesApiWs;
import com.ziqni.admin.sdk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.util.List;

public class ZiqniSystemCallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZiqniSystemCallbackHandler.class);

    private final EntityChangesApiWs entityChangesApi;
    private final ZiqniEventBus ziqniEventBus;


    public ZiqniSystemCallbackHandler(ZiqniEventBus ziqniEventBus, ZiqniAdminApiFactory ziqniAdminApiFactory) {
        this.ziqniEventBus = ziqniEventBus;
        this.entityChangesApi = ziqniAdminApiFactory.getEntityChangesApi();
    }

    public void load(){

        this.entityChangesApi.entityChangedHandler(this::onEntityChanged, this::onEntityChangedException);
        this.entityChangesApi.entityStateChangedHandler(this::onEntityStateChanged, this::onEntityStateChangedException);

        subscribeToEntityChanges(Transformer.class);
        subscribeToEntityChanges(Connection.class);

        subscribeToEntityChanges(Member.class);
        subscribeToEntityChanges(Reward.class);
        subscribeToEntityChanges(Achievement.class);
        subscribeToEntityChanges(Award.class);
        subscribeToEntityChanges(Product.class);
    }

    private void subscribeToEntityChanges(Class<?> clazz){
        this.entityChangesApi.manageEntityChangeSubscription(new EntityChangeSubscriptionRequest()
                .entityType(clazz.getSimpleName())
                .action(EntityChangeSubscriptionRequest.ActionEnum.SUBSCRIBE)
                .constraints(List.of())
        ).exceptionally(throwable -> {
            logger.error("Failed to subscribe to {}", clazz.getSimpleName(), throwable);
            return null;
        });
    }

    private void onEntityChanged(StompHeaders stompHeaders, EntityChanged entityChanged){
        this.ziqniEventBus.postEntityChangedEventBus(entityChanged);
    }
    private void onEntityChangedException(StompHeaders stompHeaders, ApiException apiException){
        logger.error("Failed to process onEntityChanged", apiException.getCause());
    }

    private void onEntityStateChanged(StompHeaders stompHeaders, EntityStateChanged entityStateChanged){
        this.ziqniEventBus.postEntityChangedEventBus(entityStateChanged);
    }
    private void onEntityStateChangedException(StompHeaders stompHeaders, ApiException apiException){
        logger.error("Failed to process onEntityStateChanged", apiException.getCause());
    }
}
