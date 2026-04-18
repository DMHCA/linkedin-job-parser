package com.romantrippel.linkedinjobparser.scheduler;

import com.romantrippel.linkedinjobparser.service.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);
    private final JobProcessor processor;

    public JobScheduler(JobProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "#{@parserProperties.interval}")
    public void runParser() {
        try {
            log.info("Starting parser cycle...");
            processor.process();
            log.info("Parser cycle finished successfully");
        }
        catch (Exception e) {
            log.error("Parser cycle failed", e);
        }
    }
}