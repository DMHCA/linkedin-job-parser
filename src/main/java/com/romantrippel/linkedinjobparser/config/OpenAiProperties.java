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

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled;
    }
}