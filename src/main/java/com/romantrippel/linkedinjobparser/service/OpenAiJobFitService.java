package com.romantrippel.linkedinjobparser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romantrippel.linkedinjobparser.config.CandidateProperties;
import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.loader.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpenAiJobFitService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiJobFitService.class);

    private final OpenAiProperties openAiProperties;
    private final CandidateProperties candidateProperties;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiJobFitService(OpenAiProperties openAiProperties,
                               CandidateProperties candidateProperties,
                               PromptLoader promptLoader,
                               ObjectMapper objectMapper,
                               RestTemplate restTemplate) {
        this.openAiProperties = openAiProperties;
        this.candidateProperties = candidateProperties;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public JobFitResponse evaluate(Job job) {

        log.info("Sending job to OpenAI: jobId={}, title={}", job.getJobId(), job.getTitle());

        try {
            String promptTemplate = promptLoader.loadPrompt();
            String prompt = buildEvaluationPrompt(promptTemplate, job);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiProperties.getApiKey());

            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "input", prompt,
                    "max_output_tokens", openAiProperties.getMaxTokens(),
                    "reasoning", Map.of("effort", "minimal")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    openAiProperties.getUrl(),
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("OpenAI returned non-success status: {} for jobId={}",
                        response.getStatusCode(), job.getJobId());
                throw new RuntimeException("OpenAI returned status: " + response.getStatusCode());
            }

            String responseBody = response.getBody();

            log.info("OpenAI response received: jobId={}", job.getJobId());

            String jsonText = extractOutputText(responseBody);

            return objectMapper.readValue(jsonText, JobFitResponse.class);

        } catch (RestClientException e) {
            log.error("HTTP error while calling OpenAI. jobId={}, title={}",
                    job.getJobId(),
                    job.getTitle(),
                    e);
            throw new RuntimeException("HTTP error while calling OpenAI", e);

        } catch (Exception e) {
            log.error("OpenAI evaluation failed. jobId={}, title={}",
                    job.getJobId(),
                    job.getTitle(),
                    e);
            throw new RuntimeException(
                    "OpenAI evaluation failed for jobId=" + job.getJobId()
                            + ", title=" + job.getTitle(),
                    e
            );
        }
    }

    private String buildEvaluationPrompt(String template, Job job) {
        return template
                .replace("{{profile}}", candidateProperties.getProfile())
                .replace("{{title}}", safe(job.getTitle()))
                .replace("{{company}}", safe(job.getCompany()))
                .replace("{{location}}", safe(job.getLocation()))
                .replace("{{description}}", safe(job.getDescription()));
    }

    private String extractOutputText(String responseBody) throws Exception {
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
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}