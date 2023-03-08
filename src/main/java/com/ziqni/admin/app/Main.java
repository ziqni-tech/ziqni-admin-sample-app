package com.ziqni.admin.app;

import com.ziqni.admin.sdk.ZiqniAdminApiFactory;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main( String[] args ) throws Exception {

        logger.info("+++++ ZIQNI Build Version: {} +++++", "2023-02-14-00-00");
        var configuration = AdminApiClientConfigBuilder.build();
        var ziqniAdminApiFactory = new ZiqniAdminApiFactory(configuration);
        logger.info("Launched compute engine app for project [{}] and user [{}]", configuration.getAdminClientIdentityProjectUrl(), configuration.getAdminClientIdentityUser());
    }
}
