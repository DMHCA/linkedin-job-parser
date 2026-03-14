package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
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
    private final OpenAiProperties openAiProperties;
    private final LinkedInJobParser linkedInJobParser;
    private final RestClient restClient;

    public TelegramService(TelegramProperties telegramProperties,
                           OpenAiProperties openAiProperties,
                           LinkedInJobParser linkedInJobParser) {
        this.telegramProperties = telegramProperties;
        this.openAiProperties = openAiProperties;
        this.linkedInJobParser = linkedInJobParser;
        this.restClient = RestClient.builder().build();
    }

    public void sendJob(Job job) {
        if (!telegramProperties.isEnabled()) {
            return;
        }

        String logoUrl = linkedInJobParser.fetchLogoUrlByJobId(job.getJobId());

        if (logoUrl != null && !logoUrl.isBlank()) {
            sendPhoto(job, logoUrl);
            return;
        }

        sendMessage(job);
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

        message.append(buildHeader(job));
        message.append("🏢 Company: ").append(safe(job.getCompany())).append("\n");
        message.append("💼 Title: ").append(safe(job.getTitle())).append("\n");
        message.append("📍 Location: ").append(safe(job.getLocation())).append("\n");
        message.append("🆔 JobId: ").append(safe(job.getJobId())).append("\n");

        if (job.getFit() != null) {
            message.append("✅ Fit: ").append(job.getFit()).append("\n");
        }

        if (job.getFitScore() != null) {
            message.append("📊 FitScore: ").append(job.getFitScore()).append("\n");
        }

        if (job.getRoleType() != null) {
            message.append("🧩 RoleType: ").append(job.getRoleType()).append("\n");
        }

        if (job.getSeniorityMatch() != null) {
            message.append("📈 Seniority: ").append(job.getSeniorityMatch()).append("\n");
        }

        if (job.getTechMatch() != null) {
            message.append("🛠 TechMatch: ").append(job.getTechMatch()).append("\n");
        }

        if (job.getVerdict() != null) {
            message.append("🎯 Verdict: ").append(job.getVerdict()).append("\n");
        }

        if (job.getReason() != null && !job.getReason().isBlank()) {
            message.append("\n📝 Reason:\n")
                    .append(job.getReason())
                    .append("\n");
        }

        if (job.getCoverLetter() != null && !job.getCoverLetter().isBlank()) {
            message.append("\n✉️ Cover Letter:\n")
                    .append(job.getCoverLetter())
                    .append("\n");
        }

        message.append("\n🔗 Link:\n")
                .append(safe(job.getLink()));

        return message.toString();
    }

    private String buildHeader(Job job) {
        Integer score = job.getFitScore();

        if (score != null && score >= openAiProperties.getCoverLetterThreshold()) {
            return "🔥🔥🔥 HOT MATCH 🔥🔥🔥\n"
                    + "━━━━━━━━━━━━━━━━━━━━\n\n";
        }

        return "💼 New opportunity\n"
                + "━━━━━━━━━━━━━━━━━━━━\n\n";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}