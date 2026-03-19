package com.romantrippel.linkedinjobparser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.romantrippel.linkedinjobparser.config.CandidateProperties;
import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.loader.PromptLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAiJobFitServiceTest {

    private OpenAiProperties openAiProperties;
    private CandidateProperties candidateProperties;
    private PromptLoader promptLoader;
    private ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    private OpenAiJobFitService service;

    @BeforeEach
    void setUp() {
        openAiProperties = mock(OpenAiProperties.class);
        candidateProperties = mock(CandidateProperties.class);
        promptLoader = mock(PromptLoader.class);
        objectMapper = new ObjectMapper();
        restTemplate = mock(RestTemplate.class);

        service = new OpenAiJobFitService(
                openAiProperties,
                candidateProperties,
                promptLoader,
                objectMapper,
                restTemplate
        );
    }

    @Test
    void evaluate_shouldReturnJobFitResponse_whenOpenAiReturnsValidJson() throws Exception {

        // given
        Job job = new Job();
        job.setJobId("123");
        job.setTitle("Java Backend Developer");
        job.setCompany("Test Company");
        job.setLocation("Germany");
        job.setDescription("Java Spring Boot PostgreSQL");

        when(candidateProperties.getProfile()).thenReturn("Java developer");
        when(promptLoader.loadPrompt()).thenReturn("{{profile}} {{title}}");

        when(openAiProperties.getApiKey()).thenReturn("key");
        when(openAiProperties.getModel()).thenReturn("gpt-test");
        when(openAiProperties.getMaxTokens()).thenReturn(500);
        when(openAiProperties.getUrl()).thenReturn("http://test-url");

        String openAiResponse = """
                {
                  "output_text": "{ \\"fitScore\\": 80, \\"seniority\\": \\"mid\\", \\"stack\\": \\"Java, Spring Boot\\", \\"responsibilities\\": \\"Develop backend services\\", \\"matchReason\\": \\"Good match\\" }"
                }
                """;

        ResponseEntity<String> response = new ResponseEntity<>(openAiResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                ArgumentMatchers.<HttpEntity<?>>any(),
                eq(String.class))
        ).thenReturn(response);

        // when
        JobFitResponse result = service.evaluate(job);

        // then
        assertNotNull(result);
        assertEquals(80, result.getFitScore());
        assertEquals("mid", result.getSeniority());
        assertEquals("Java, Spring Boot", result.getStack());
    }

    @Test
    void evaluate_shouldThrowException_whenHttpStatusIsNot200() {

        // given
        Job job = new Job();
        job.setJobId("123");

        when(candidateProperties.getProfile()).thenReturn("profile");
        when(promptLoader.loadPrompt()).thenReturn("template");

        when(openAiProperties.getApiKey()).thenReturn("key");
        when(openAiProperties.getModel()).thenReturn("model");
        when(openAiProperties.getMaxTokens()).thenReturn(100);
        when(openAiProperties.getUrl()).thenReturn("url");

        ResponseEntity<String> response =
                new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);

        // when + then
        assertThrows(RuntimeException.class, () -> service.evaluate(job));
    }

    @Test
    void evaluate_shouldThrowException_whenRestTemplateFails() {

        // given
        Job job = new Job();
        job.setJobId("123");

        when(candidateProperties.getProfile()).thenReturn("profile");
        when(promptLoader.loadPrompt()).thenReturn("template");

        when(openAiProperties.getApiKey()).thenReturn("key");
        when(openAiProperties.getModel()).thenReturn("model");
        when(openAiProperties.getMaxTokens()).thenReturn(100);
        when(openAiProperties.getUrl()).thenReturn("url");

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> service.evaluate(job));
    }

    @Test
    void evaluate_shouldThrowException_whenJsonIsInvalid() {

        // given
        Job job = new Job();
        job.setJobId("123");

        when(candidateProperties.getProfile()).thenReturn("profile");
        when(promptLoader.loadPrompt()).thenReturn("template");

        when(openAiProperties.getApiKey()).thenReturn("key");
        when(openAiProperties.getModel()).thenReturn("model");
        when(openAiProperties.getMaxTokens()).thenReturn(100);
        when(openAiProperties.getUrl()).thenReturn("url");

        String brokenJson = """
                { "output_text": "not a json" }
                """;

        ResponseEntity<String> response =
                new ResponseEntity<>(brokenJson, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);

        // when + then
        assertThrows(RuntimeException.class, () -> service.evaluate(job));
    }

    @Test
    void evaluate_shouldHandleNullFieldsInJob() {

        // given
        Job job = new Job();
        job.setJobId("123");
        job.setTitle(null);
        job.setCompany(null);
        job.setLocation(null);
        job.setDescription(null);

        when(candidateProperties.getProfile()).thenReturn("profile");
        when(promptLoader.loadPrompt()).thenReturn("{{title}} {{company}}");

        when(openAiProperties.getApiKey()).thenReturn("key");
        when(openAiProperties.getModel()).thenReturn("model");
        when(openAiProperties.getMaxTokens()).thenReturn(100);
        when(openAiProperties.getUrl()).thenReturn("url");

        String openAiResponse = """
                {
                  "output_text": "{ \\"fitScore\\": 50, \\"seniority\\": \\"junior\\", \\"stack\\": \\"Java\\", \\"responsibilities\\": \\"Develop backend\\", \\"matchReason\\": \\"Partial match\\" }"
                }
                """;

        ResponseEntity<String> response =
                new ResponseEntity<>(openAiResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);

        // when
        JobFitResponse result = service.evaluate(job);

        // then
        assertNotNull(result);
        assertEquals(50, result.getFitScore());
    }
}