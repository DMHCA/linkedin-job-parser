package com.romantrippel.linkedinjobparser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramProperties {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    @Value("${telegram.chat-amazon-id}")
    private String chatAmazonId;

    @Value("${telegram.chat-germany-id}")
    private String chatGermanyId;

    @Value("${telegram.enabled}")
    private boolean enabled;

    public String getChatAmazonId() {
        return chatAmazonId;
    }

    public void setChatAmazonId(String chatAmazonId) {
        this.chatAmazonId = chatAmazonId;
    }

    public String getChatGermanyId() {
        return chatGermanyId;
    }

    public void setChatGermanyId(String chatGermanyId) {
        this.chatGermanyId = chatGermanyId;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}