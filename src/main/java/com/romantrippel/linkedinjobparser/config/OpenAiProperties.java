package com.romantrippel.linkedinjobparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="openai")
public class OpenAiProperties {
    private String apiKey;
    private String model;
    private boolean enabled;
    private int coverLetterThreshold;
    private int timeoutMs;
    private int maxTokens;
    private String url;
    private String promptPath;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCoverLetterThreshold() {
        return coverLetterThreshold;
    }

    public void setCoverLetterThreshold(int coverLetterThreshold) {
        this.coverLetterThreshold = coverLetterThreshold;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPromptPath() {
        return promptPath;
    }

    public void setPromptPath(String promptPath) {
        this.promptPath = promptPath;
    }
}

