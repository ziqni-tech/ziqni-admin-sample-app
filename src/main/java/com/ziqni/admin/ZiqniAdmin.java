package com.ziqni.admin;

import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.bus.ZiqniEventBus;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.handlers.ZiqniSystemCallbackHandler;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfiguration;
import com.ziqni.admin.sdk.context.WSClientConnected;
import com.ziqni.admin.sdk.context.WSClientConnecting;
import com.ziqni.admin.sdk.context.WSClientDisconnected;
import com.ziqni.admin.sdk.context.WSClientSevereFailure;
import com.ziqni.admin.stores.Store;
import com.ziqni.admin.stores.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZiqniAdmin {

    private static final Logger logger = LoggerFactory.getLogger(ZiqniAdmin.class);

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final AdminApiClientConfiguration configuration;
    private final AtomicBoolean waitingForReconnect = new AtomicBoolean(false);
    private final ZiqniSystemCallbackHandler ziqniSystemCallbackHandler;
    private final Stores ziqniStores;

    public ZiqniAdmin(ZiqniAdminApiFactory ziqniAdminApiFactory, AdminApiClientConfiguration configuration) {
        this.ziqniAdminApiFactory = ziqniAdminApiFactory;
        this.configuration = configuration;
        this.ziqniStores = new Stores(ziqniAdminApiFactory);
        this.ziqniSystemCallbackHandler = new ZiqniSystemCallbackHandler(new ZiqniEventBus(),ziqniAdminApiFactory);
    }

    public void Ignition(String[] args) throws Exception {
        logger.info("*** Ignition sequence started ***");
        try {
            ziqniStores
                    .start()
                    .exceptionally(throwable -> {
                        logger.error("Failed to load the cache stores", throwable);
                        return null;
                    });

            // implement shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> Application.shutdownHook(ziqniAdminApiFactory, configuration)));

        } catch (Exception e) {
            logger.error("Failed to launch", e);
            throw e;
        }
    }

    public Stores getZiqniStores() {
        return ziqniStores;
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    //////// ADMIN API CLIENT EVENTBUS ////////
    @Subscribe
    public void onWSClientConnected(WSClientConnected change) {
        this.ziqniSystemCallbackHandler.load();
    }

    @Subscribe
    public void onWSClientConnecting(WSClientConnecting change) {
    }

    @Subscribe
    public void onWSClientDisconnected(WSClientDisconnected change){
        if(!waitingForReconnect.get()){ // Have we been told to reconnect?
            waitingForReconnect.set(true); // Make a note to say we are already trying to reconnect
            scheduleReconnectWatcher();
        }
    }

    @Subscribe
    public void onWSClientSevereFailure(WSClientSevereFailure change){
        logger.info("WSClientSevereFailure");
    }

    private void scheduleReconnectWatcher(){
        waitingForReconnect.set(true);
        ZiqniExecutors.ReconnectScheduledExecutor.schedule( () -> {
            if(this.ziqniAdminApiFactory.getStreamingClient().isConnected()){
                waitingForReconnect.set(false);
            }
            else {
                try {
                    this.ziqniAdminApiFactory.getStreamingClient().start();
                } catch (Throwable throwable) {
                    logger.error("Failed to start the streaming client. scheduling a retry at {} [{}]", OffsetDateTime.now().plusSeconds(30), throwable.getMessage());
                }
                scheduleReconnectWatcher();
            }
        }, 30, TimeUnit.SECONDS);
    }
}
