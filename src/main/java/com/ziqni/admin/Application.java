package com.ziqni.admin;

import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.exceptions.GlobalExceptionHandler;
import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfigBuilder;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfiguration;
import com.ziqni.admin.sdk.context.WSClientConnected;
import com.ziqni.admin.sdk.context.WSClientConnecting;
import com.ziqni.admin.sdk.context.WSClientDisconnected;
import com.ziqni.admin.sdk.context.WSClientSevereFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main( String[] args ) throws Exception {

        logger.info("+++++ ZIQNI Build Version: {} +++++", "2023-02-14-00-00. Let's light this candle!");
        var configuration = AdminApiClientConfigBuilder.build();
        var ziqniAdminApiFactory = new ZiqniAdminApiFactory(configuration);
        logger.info("Launched compute engine app for project [{}] and user [{}]", configuration.getAdminClientIdentityProjectUrl(), configuration.getAdminClientIdentityUser());


        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        ziqniAdminApiFactory.initialise();

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

            new ZiqniAdmin(ziqniAdminApiFactory,configuration).Ignition(args);
        }
        else
            throw new RuntimeException("+++ Only socket based communications is used for this platform");
    }

    protected static void shutdownHook(ZiqniAdminApiFactory ziqniAdminApiFactory, AdminApiClientConfiguration configuration) {
        logger.info("+++ Shut down commenced for compute engine app.");
        logger.info("+++ Shut down tasks completed for engine app for project [{}] and user [{}]", configuration.getAdminClientIdentityProjectUrl(), configuration.getAdminClientIdentityUser());
        if(ziqniAdminApiFactory.getStreamingClient()!=null)
            ziqniAdminApiFactory.getStreamingClient().stop();
    }

}
