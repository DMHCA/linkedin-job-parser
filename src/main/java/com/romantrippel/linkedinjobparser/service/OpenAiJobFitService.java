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

            JobFitResponse result = objectMapper.readValue(jsonText, JobFitResponse.class);

            if (result.getFitScore() >= openAiProperties.getCoverLetterThreshold()) {
                String coverLetter = generateCoverLetter(job);
                result.setCoverLetter(coverLetter);
            }

            return result;

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

                Use the candidate profile and the job description.

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
                  "fit": true,
                  "fitScore": 0,
                  "roleType": "backend",
                  "seniorityMatch": "good",
                  "techMatch": "good",
                  "reason": "short explanation",
                  "verdict": "recommended",
                  "coverLetter": ""
                }
                """;
    }

    private String generateCoverLetter(Job job) {
        try {
            String prompt = """
                    Write a very short professional cover letter in 3-4 sentences.

                    Return plain text only.
                    Do not add markdown.

                    Candidate profile:
                    """ + candidateProperties.getProfile() + """

                    Job title:
                    """ + safe(job.getTitle()) + """

                    Company:
                    """ + safe(job.getCompany()) + """

                    Job description:
                    """ + safe(job.getDescription()) + """
                    """;

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

            return extractOutputText(responseBody);

        } catch (Exception e) {
            System.out.println("Cover letter generation failed for jobId=" + job.getJobId()
                    + ", reason=" + e.getMessage());
            return "";
        }
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
            throw new RuntimeException("Failed to parse OpenAI response. Raw body: " + responseBody, e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}