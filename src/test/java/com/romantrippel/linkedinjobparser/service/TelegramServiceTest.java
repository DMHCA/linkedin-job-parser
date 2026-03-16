package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.config.TelegramProperties;
import com.romantrippel.linkedinjobparser.entity.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TelegramServiceTest {

    private TelegramProperties telegramProperties;
    private OpenAiProperties openAiProperties;
    private RestTemplate restTemplate;

    private TelegramService telegramService;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setBotToken("TEST_TOKEN");
        telegramProperties.setChatId("123");
        telegramProperties.setEnabled(true);

        openAiProperties = mock(OpenAiProperties.class);
        when(openAiProperties.getCoverLetterThreshold()).thenReturn(80);

        restTemplate = mock(RestTemplate.class);

        telegramService = new TelegramService(
                telegramProperties,
                openAiProperties,
                restTemplate
        );
    }

    private Job createJob() {
        return new Job(
                "job123",
                "Backend Developer",
                "Amazon",
                "Berlin",
                "https://linkedin.com/job",
                "Some description",
                90,
                "Senior",
                "Java, Spring",
                "Build microservices",
                "Strong Java experience"
        );
    }

    @Test
    void buildMessage_shouldBuildFullMessage() {

        Job job = createJob();

        String message = telegramService.buildMessage(job);

        assertThat(message).contains("Amazon");
        assertThat(message).contains("Backend Developer");
        assertThat(message).contains("Berlin");
        assertThat(message).contains("Java, Spring");
        assertThat(message).contains("Build microservices");
        assertThat(message).contains("Strong Java experience");
        assertThat(message).contains("HOT MATCH");
    }

    @Test
    void buildMessage_shouldBuildNormalHeader() {

        Job job = createJob();
        job.setFitScore(50);

        String message = telegramService.buildMessage(job);

        assertThat(message).contains("New opportunity");
        assertThat(message).doesNotContain("HOT MATCH");
    }

    @Test
    void sendJob_shouldSendMessage() {

        Job job = createJob();

        telegramService.sendJob(job);

        verify(restTemplate, times(1))
                .postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void sendJob_shouldNotSendIfTelegramDisabled() {

        telegramProperties.setEnabled(false);

        telegramService.sendJob(createJob());

        verify(restTemplate, never())
                .postForObject(anyString(), any(), any());
    }

    @Test
    void sendJob_shouldThrowException_whenJobIsNull() {
        NullPointerException exception = org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> telegramService.sendJob(null)
        );

        assertThat(exception.getMessage()).isEqualTo("job must not be null");
    }

    @Test
    void sendJob_shouldSendCorrectUrl() {

        Job job = createJob();

        telegramService.sendJob(job);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        verify(restTemplate).postForObject(
                urlCaptor.capture(),
                any(HttpEntity.class),
                eq(String.class)
        );

        String url = urlCaptor.getValue();

        assertThat(url)
                .isEqualTo("https://api.telegram.org/botTEST_TOKEN/sendMessage");
    }
}