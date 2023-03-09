package com.ziqni.admin;

import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.exceptions.GlobalExceptionHandler;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfigBuilder;
import com.ziqni.admin.sdk.context.WSClientConnected;
import com.ziqni.admin.sdk.context.WSClientConnecting;
import com.ziqni.admin.sdk.context.WSClientSevereFailure;
import com.ziqni.admin.sdk.model.EntityChanged;
import com.ziqni.admin.sdk.model.EntityStateChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(SampleApplication.class);

    public static void main( String[] args ) throws Exception {

        logger.info("+++++ ZIQNI Build Version: {} +++++", "2023-02-09-00-00. Let's light this candle!");
        var configuration = AdminApiClientConfigBuilder.build();
        new ZiqniAdmin(configuration).launch(SampleApplication::new);
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
    }

    public SampleApplication(ZiqniAdmin ziqniAdmin) {
        ziqniAdmin.registerToReceiveEvents(this);

        ziqniAdmin.getZiqniStores().getMembersStore().subscribeToEntityChanges();
        ziqniAdmin.getZiqniStores().getProductsStore().subscribeToEntityChanges();

        ziqniAdmin.getZiqniStores().getRewardStore().subscribeToEntityChanges();
        ziqniAdmin.getZiqniStores().getAwardStore().subscribeToEntityChanges();

        ziqniAdmin.getZiqniStores().getCompetitionsStore().subscribeToEntityChanges();
        ziqniAdmin.getZiqniStores().getContestsStore().subscribeToEntityChanges();

    }

    @Subscribe
    public void onWSClientConnected(WSClientConnected change) {
        logger.info("+++++ ZIQNI connected, {}", change);
    }

    @Subscribe
    public void onWSClientConnecting(WSClientConnecting change) {
        logger.info("+++++ ZIQNI connecting, {}", change);
    }

    @Subscribe
    public void onWSClientSevereFailure(WSClientSevereFailure change){
        logger.info("+++++  ZIQNI websocket client experienced a severe failure, {}", change);
    }
    @Subscribe
    public void onEntityChanged(EntityChanged entityChanged){
        logger.info("+++++  ZIQNI entity changed, {}", entityChanged);
    }

    @Subscribe
    public void onEntityStateChanged(EntityStateChanged entityStateChanged){
        logger.info("+++++  ZIQNI entity state changed, {}", entityStateChanged);
    }
}
