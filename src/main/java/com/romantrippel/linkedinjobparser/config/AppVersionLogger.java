package com.romantrippel.linkedinjobparser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class AppVersionLogger {

    private static final Logger log = LoggerFactory.getLogger(AppVersionLogger.class);

    @Value("${info.app.version}")
    private String version;

    @PostConstruct
    public void logVersion() {
        log.info("Application version: {}", version);
    }

}