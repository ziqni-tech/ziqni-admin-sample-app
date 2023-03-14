package com.ziqni.admin;

import com.google.common.eventbus.Subscribe;
import com.ziqni.admin.concurrent.ZiqniExecutors;
import com.ziqni.admin.exceptions.GlobalExceptionHandler;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfigBuilder;
import com.ziqni.admin.sdk.context.WSClientConnected;
import com.ziqni.admin.sdk.context.WSClientConnecting;
import com.ziqni.admin.sdk.context.WSClientDisconnected;
import com.ziqni.admin.sdk.context.WSClientSevereFailure;
import com.ziqni.admin.sdk.model.EntityChanged;
import com.ziqni.admin.sdk.model.EntityStateChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SampleApplication implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SampleApplication.class);

    private final ScheduledThreadPoolExecutor ticker;

    public static void main( String[] args ) throws Exception {

        ZiqniAdmin.launch(
                AdminApiClientConfigBuilder.build(),
                SampleApplication::new
        );

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

        this.ticker = ZiqniExecutors.newSingleThreadScheduledExecutor("simulate-data-feed");
        this.ticker.schedule(this, 5, TimeUnit.SECONDS);
    }

    @Subscribe
    public void onZiqniAdminConnected(WSClientConnected change) {
        logger.info("+++++ ZIQNI connected, {}", change);
    }

    @Subscribe
    public void onZiqniAdminConnecting(WSClientConnecting change) {
        logger.info("+++++ ZIQNI admin api connecting, {}", change);
    }

    @Subscribe
    public void onZiqniAdminDisconnected(WSClientDisconnected change){
        logger.info("+++++ ZIQNI admin api disconnected, {}", change);
    }

    @Subscribe
    public void onZiqniAdminSevereFailure(WSClientSevereFailure change){
        logger.info("+++++  ZIQNI admin api experienced a severe failure, {}", change);
    }

    @Subscribe
    public void onZiqniAdminEntityChanged(EntityChanged entityChanged){
        logger.info("+++++  ZIQNI entity changed, {}", entityChanged);
    }

    @Subscribe
    public void onZiqniAdminEntityStateChanged(EntityStateChanged entityStateChanged){
        logger.info("+++++  ZIQNI entity state changed, {}", entityStateChanged);
    }

    @Override
    public void run() {

//        this.ziqniAdmin.getZiqniStores().getMembersStore().getMemberByRefId("3596684600255879")
//                .thenAccept(member ->
//                        logger.info(member.toString())
//                )
//                .exceptionally(throwable -> {
//                    logger.error("Failed to get the member response",throwable);
//                    return null;
//                })
//                .join();


//        Create/update a member [player]
//        this.ziqniAdmin.getZiqniStores().getMembersStore().createMember(new CreateMemberRequest()
//                .name()
//                .memberRefId()
//                .tags() <- User groups or other classification of your population
//        );
//
//         Create an action type like, buy, return, win, stake etc
//        this.ziqniAdmin.getZiqniStores().getActionTypesStore().create()

        this.ticker.schedule(this, 5, TimeUnit.SECONDS);
    }
}
