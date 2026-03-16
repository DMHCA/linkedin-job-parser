package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.config.TelegramProperties;
import com.romantrippel.linkedinjobparser.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final TelegramProperties telegramProperties;
    private final OpenAiProperties openAiProperties;
    private final RestTemplate restTemplate;

    public TelegramService(TelegramProperties telegramProperties,
                           OpenAiProperties openAiProperties,
                           RestTemplate restTemplate) {
        this.telegramProperties = telegramProperties;
        this.openAiProperties = openAiProperties;
        this.restTemplate = restTemplate;
    }

    public void sendJob(Job job) {
        Objects.requireNonNull(job, "job must not be null");

        if (!telegramProperties.isEnabled()) return;

        try {
            sendMessage(job);
        } catch (Exception e) {
            log.error("Telegram send failed for jobId={}", job.getJobId(), e);
        }
    }

    private void sendMessage(Job job) {
        String url = "https://api.telegram.org/bot" + telegramProperties.getBotToken() + "/sendMessage";

        Map<String, Object> requestBody = Map.of(
                "chat_id", telegramProperties.getChatId(),
                "text", buildMessage(job),
                "disable_web_page_preview", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        restTemplate.postForObject(url, request, String.class);
    }

    public String buildMessage(Job job) {
        StringBuilder message = new StringBuilder();

        message.append(buildHeader(job));
        appendJobMainInfo(message, job);
        appendScoreAndLevel(message, job);
        appendStack(message, job);
        appendResponsibilities(message, job);
        appendMatchReason(message, job);
        appendLink(message, job);

        return message.toString();
    }

    // --- private helpers ---
    private String buildHeader(Job job) {
        Integer score = job.getFitScore();
        if (score != null && score >= openAiProperties.getCoverLetterThreshold()) {
            return "🔥🔥🔥 HOT MATCH 🔥🔥🔥\n━━━━━━━━━━━━━━━━━━━━\n\n";
        }
        return "💼 New opportunity\n━━━━━━━━━━━━━━━━━━━━\n\n";
    }

    private void appendJobMainInfo(StringBuilder msg, Job job) {
        msg.append("🏢 ").append(safe(job.getCompany())).append("\n");
        msg.append("💼 ").append(safe(job.getTitle())).append("\n");
        msg.append("📍 ").append(safe(job.getLocation())).append("\n\n");
    }

    private void appendScoreAndLevel(StringBuilder msg, Job job) {
        if (job.getFitScore() != null) msg.append("📊 Score: ").append(job.getFitScore()).append("\n");
        if (job.getSeniority() != null && !job.getSeniority().isBlank())
            msg.append("📈 Level: ").append(job.getSeniority()).append("\n");
        if (job.getFitScore() != null || (job.getSeniority() != null && !job.getSeniority().isBlank()))
            msg.append("\n");
    }

    private void appendStack(StringBuilder msg, Job job) {
        if (job.getStack() != null && !job.getStack().isBlank()) {
            msg.append("🛠 Stack: ").append(job.getStack()).append("\n\n");
        }
    }

    private void appendResponsibilities(StringBuilder msg, Job job) {
        if (job.getResponsibilities() != null && !job.getResponsibilities().isBlank()) {
            msg.append("🧠 Чем заниматься:\n").append(job.getResponsibilities()).append("\n\n");
        }
    }

    private void appendMatchReason(StringBuilder msg, Job job) {
        if (job.getMatchReason() != null && !job.getMatchReason().isBlank()) {
            msg.append("✅ Почему подходит:\n").append(job.getMatchReason()).append("\n\n");
        }
    }

    private void appendLink(StringBuilder msg, Job job) {
        msg.append("🔗 LinkedIn\n").append(safe(job.getLink()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}