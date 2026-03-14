package com.romantrippel.linkedinjobparser.scheduler;

import com.romantrippel.linkedinjobparser.service.JobProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobProcessor processor;

    public JobScheduler(JobProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${parser.interval}")
    public void runParser() throws Exception {
        System.out.println("Starting parser cycle...");
        processor.process();
    }
}