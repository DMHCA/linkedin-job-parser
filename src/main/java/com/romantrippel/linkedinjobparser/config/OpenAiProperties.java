package com.romantrippel.linkedinjobparser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiProperties {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.enabled}")
    private boolean enabled;

    @Value("${openai.cover-letter-threshold}")
    private int coverLetterThreshold;

    @Value("${openai.timeout-ms}")
    private int timeoutMs;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCoverLetterThreshold() {
        return coverLetterThreshold;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}