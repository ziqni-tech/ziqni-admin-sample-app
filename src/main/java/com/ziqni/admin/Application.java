package com.ziqni.admin;

import com.ziqni.admin.exceptions.GlobalExceptionHandler;
import com.ziqni.admin.sdk.configuration.AdminApiClientConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main( String[] args ) throws Exception {

        logger.info("+++++ ZIQNI Build Version: {} +++++", "2023-02-09-00-00. Let's light this candle!");
        var configuration = AdminApiClientConfigBuilder.build();
        new ZiqniAdmin(configuration).launch(ziqniAdmin -> {
            // Do something
        });
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
    }
}
