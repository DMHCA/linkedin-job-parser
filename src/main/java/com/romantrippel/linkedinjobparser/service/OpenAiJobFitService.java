package com.romantrippel.linkedinjobparser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romantrippel.linkedinjobparser.config.CandidateProperties;
import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(openAiProperties.getTimeoutMs());
        requestFactory.setReadTimeout(openAiProperties.getTimeoutMs());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public JobFitResponse evaluate(Job job) {

        try {

            String prompt = buildEvaluationPrompt(job);

            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "input", prompt,
                    "max_output_tokens", openAiProperties.getMaxTokens(),
                    "reasoning", Map.of("effort", "minimal")
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

            throw new RuntimeException(
                    "OpenAI evaluation failed for jobId=" + job.getJobId()
                            + ", title=" + job.getTitle()
                            + ", reason=" + e.getMessage(),
                    e
            );
        }
    }

    private String buildEvaluationPrompt(Job job) {

        return """
You evaluate job offers for a software engineer.

Return valid JSON only.
Do not add markdown.
Do not add code fences.

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
  "fitScore": 72,
  "seniority": "mid",
  "stack": "Java, Spring Boot, PostgreSQL",
  "responsibilities": "Разработка backend микросервисов для платежной платформы",
  "matchReason": "У кандидата есть опыт Java и Spring Boot, стек совпадает примерно на 70%"
}

Rules:

- fitScore must be integer from 0 to 100
- seniority must be one of: junior, mid, senior
- stack must list the main technologies
- responsibilities must be one short sentence
- matchReason must explain why the candidate fits
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

            throw new RuntimeException("Could not extract model text from response: " + responseBody);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse OpenAI response. Raw body: " + responseBody,
                    e
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}