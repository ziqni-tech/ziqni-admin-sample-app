package com.ziqni.admin;

import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.sdk.model.EntityChanged;
import com.ziqni.admin.sdk.model.EntityStateChanged;
import com.ziqni.admin.watchers.ZiqniSystemCallbackWatcher;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfiguration;
import com.ziqni.admin.sdk.context.WSClientDisconnected;
import com.ziqni.admin.sdk.context.WSClientSevereFailure;
import com.ziqni.admin.stores.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ZiqniAdmin {

    private static final Logger logger = LoggerFactory.getLogger(ZiqniAdmin.class);

    private final ZiqniAdminApiFactory ziqniAdminApiFactory;
    private final AdminApiClientConfiguration configuration;
    private final AtomicBoolean waitingForReconnect = new AtomicBoolean(false);
    private final ZiqniSystemCallbackWatcher ziqniSystemCallbackWatcher;
    private final Stores ziqniStores;

    public ZiqniAdmin(AdminApiClientConfiguration configuration) {
        this.configuration = configuration;
        this.ziqniAdminApiFactory = new ZiqniAdminApiFactory(configuration);
        this.ziqniSystemCallbackWatcher = new ZiqniSystemCallbackWatcher(ziqniAdminApiFactory);
        this.ziqniStores = new Stores(ziqniAdminApiFactory,ziqniSystemCallbackWatcher);
        this.ziqniAdminApiFactory.getZiqniAdminEventBus().register(this);
    }

    public ZiqniAdmin launch(Consumer<ZiqniAdmin> onLaunched) throws Exception {
        logger.info("*** Ignition sequence started, Let's light up this candle! ***");
        this.ziqniAdminApiFactory.initialise();

        if(configuration.isWebsocket()) {
            while (ziqniAdminApiFactory.getStreamingClient() == null) {
                Thread.sleep(500);
                logger.info("+++ Initializing the streaming client");
            }

            final AtomicInteger counter = new AtomicInteger(0);
            final var started = ziqniAdminApiFactory.getStreamingClient().start();
            while (!ziqniAdminApiFactory.getStreamingClient().isConnected()) {
                Thread.sleep(500);
                logger.info("+++ Waiting for the streaming client to start [{}]",counter.incrementAndGet());
            }
            logger.info("+++ Started the streaming client");
        }
        else
            throw new RuntimeException("+++ Only socket based communications is used for this platform");

        try {
            ziqniStores
                    .start()
                    .exceptionally(throwable -> {
                        logger.error("Failed to load the cache stores", throwable);
                        return null;
                    })
                    .thenAccept(stores -> {
                        this.ziqniSystemCallbackWatcher.register();
                    });

            // implement shutdown hook
            Runtime.getRuntime().addShutdownHook( new Thread(() ->
                    ZiqniAdmin.shutdownHook(ziqniAdminApiFactory, configuration))
            );

        } catch (Exception e) {
            logger.error("Failed to launch", e);
            throw e;
        }

        onLaunched.accept(this);
        return this;
    }

    public Stores getZiqniStores() {
        return ziqniStores;
    }

    public ZiqniAdminApiFactory getZiqniAdminApiFactory() {
        return ziqniAdminApiFactory;
    }

    /**
     * Register to receive events
     * WSClientConnected, WSClientConnecting, WSClientSevereFailure
     * EntityChanged, EntityStateChanged
     */
    public void registerToReceiveEvents(Object registerMe){
        this.ziqniAdminApiFactory.getZiqniAdminEventBus().register(registerMe);
        this.ziqniSystemCallbackWatcher.registerEntityChangedEventBus(registerMe);
        this.ziqniSystemCallbackWatcher.registerEntityStateChangedEventBus(registerMe);
    }

    //////// ADMIN API CLIENT EVENTBUS ////////

    @Subscribe
    public void onWSClientDisconnected(WSClientDisconnected change){
        if(!waitingForReconnect.get()){ // Have we been told to reconnect?
            waitingForReconnect.set(true); // Make a note to say we are already trying to reconnect
            scheduleReconnectWatcher();
        }
    }

    @Subscribe
    public void onWSClientSevereFailure(WSClientSevereFailure change){
        logger.info("ZIQNI WS client experienced a severe failure");
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

    protected static void shutdownHook(ZiqniAdminApiFactory ziqniAdminApiFactory, AdminApiClientConfiguration configuration) {
        logger.info("+++ Shut down commenced for compute engine app.");
        logger.info("+++ Shut down tasks completed for engine app for project [{}] and user [{}]", configuration.getAdminClientIdentityProjectUrl(), configuration.getAdminClientIdentityUser());
        if(ziqniAdminApiFactory.getStreamingClient()!=null)
            ziqniAdminApiFactory.getStreamingClient().stop();
    }

    @Subscribe
    public void onEntityChanged(EntityChanged entityChanged){
        logger.info(entityChanged.toString());
    }

    @Subscribe
    public void onEntityStateChanged(EntityStateChanged entityStateChanged){
        logger.info(entityStateChanged.toString());
    }
}
