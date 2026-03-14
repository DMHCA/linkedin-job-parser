package com.romantrippel.linkedinjobparser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romantrippel.linkedinjobparser.config.CandidateProperties;
import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OpenAiJobFitService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/responses";

    private final OpenAiProperties openAiProperties;
    private final CandidateProperties candidateProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public OpenAiJobFitService(OpenAiProperties openAiProperties,
                               CandidateProperties candidateProperties) {
        this.openAiProperties = openAiProperties;
        this.candidateProperties = candidateProperties;
        this.restClient = RestClient.builder().build();
    }

    public JobFitResponse evaluate(Job job) {
        try {
            String prompt = buildPrompt(job);

            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "input", prompt
            );

            String responseBody = restClient.post()
                    .uri(OPENAI_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            String jsonText = extractOutputText(responseBody);

            return objectMapper.readValue(jsonText, JobFitResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI evaluation failed", e);
        }
    }

    private String buildPrompt(Job job) {
        return """
                You evaluate job offers for a software engineer.

                Use the candidate profile and the job description.

                Return JSON only.

                Candidate profile:
                """ + candidateProperties.getProfile() + """

                Job title:
                """ + safe(job.getTitle()) + """

                Company:
                """ + safe(job.getCompany()) + """

                Location:
                """ + safe(job.getLocation()) + """

                Job description:
                """ + safe(job.getDescription()) + """

                Return JSON in this exact format:
                {
                  "fit": true,
                  "fitScore": 0,
                  "roleType": "backend | fullstack | frontend | qa | devops | support | other",
                  "seniorityMatch": "good | slightly_senior | too_senior",
                  "techMatch": "good | partial | poor",
                  "reason": "short explanation",
                  "verdict": "recommended | borderline | not_recommended"
                }
                """;
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode outputText = root.path("output_text");
            if (!outputText.isMissingNode() && !outputText.isNull()) {
                return outputText.asText();
            }

            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (content.isArray()) {
                        for (JsonNode part : content) {
                            JsonNode text = part.path("text");
                            if (!text.isMissingNode() && !text.isNull()) {
                                return text.asText();
                            }
                        }
                    }
                }
            }

            throw new RuntimeException("Could not extract model text from response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}