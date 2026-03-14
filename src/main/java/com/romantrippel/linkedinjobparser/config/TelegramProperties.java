package com.romantrippel.linkedinjobparser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramProperties {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    @Value("${telegram.enabled}")
    private boolean enabled;

    public String getBotToken() {
        return botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}