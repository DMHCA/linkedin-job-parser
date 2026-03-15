package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.TelegramProperties;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramService {

    private final TelegramProperties telegramProperties;
    private final LinkedInJobParser linkedInJobParser;
    private final RestClient restClient;

    public TelegramService(TelegramProperties telegramProperties,
                           LinkedInJobParser linkedInJobParser) {
        this.telegramProperties = telegramProperties;
        this.linkedInJobParser = linkedInJobParser;
        this.restClient = RestClient.builder().build();
    }

    public void sendJob(Job job) {
        if (!telegramProperties.isEnabled()) return;

        String logoUrl = linkedInJobParser.fetchLogoUrlByJobId(job.getJobId());

        if (logoUrl != null && !logoUrl.isBlank()) {
            sendPhoto(job, logoUrl);
        } else {
            sendMessage(job);
        }
    }

    private void sendPhoto(Job job, String logoUrl) {
        String url = "https://api.telegram.org/bot"
                + telegramProperties.getBotToken()
                + "/sendPhoto";

        Map<String, Object> requestBody = Map.of(
                "chat_id", telegramProperties.getChatId(),
                "photo", logoUrl,
                "caption", buildMessage(job)
        );

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            sendMessage(job);
        }
    }

    private void sendMessage(Job job) {
        String url = "https://api.telegram.org/bot"
                + telegramProperties.getBotToken()
                + "/sendMessage";

        Map<String, Object> requestBody = Map.of(
                "chat_id", telegramProperties.getChatId(),
                "text", buildMessage(job),
                "disable_web_page_preview", true
        );

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    private String buildMessage(Job job) {
        StringBuilder message = new StringBuilder();

        // Заголовок
        message.append("🔥🔥🔥 HOT MATCH 🔥🔥🔥\n")
                .append("━━━━━━━━━━━━━━━━━━━━\n\n");

        // Основная информация
        message.append("🏢 ").append(safe(job.getCompany())).append("\n");
        message.append("💼 ").append(safe(job.getTitle())).append("\n");
        message.append("📍 ").append(safe(job.getLocation())).append("\n\n");

        // Score + Level
        if (job.getFitScore() != null) {
            message.append("📊 Score: ").append(job.getFitScore()).append("\n");
        }
        if (job.getSeniority() != null && !job.getSeniority().isBlank()) {
            message.append("📈 Level: ").append(job.getSeniority()).append("\n");
        }

        // Stack
        if (job.getStack() != null && !job.getStack().isBlank()) {
            message.append("🛠 Stack: ").append(job.getStack()).append("\n\n");
        }

        // Responsibilities
        if (job.getResponsibilities() != null && !job.getResponsibilities().isBlank()) {
            message.append("🧠 Чем заниматься:\n")
                    .append(job.getResponsibilities())
                    .append("\n\n");
        }

        // Match reason
        if (job.getMatchReason() != null && !job.getMatchReason().isBlank()) {
            message.append("✅ Почему подходит:\n")
                    .append(job.getMatchReason())
                    .append("\n\n");
        }

        // Link
        message.append("🔗 LinkedIn\n")
                .append(safe(job.getLink()));

        return message.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}