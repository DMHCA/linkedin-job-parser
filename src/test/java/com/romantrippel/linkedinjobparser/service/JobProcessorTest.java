package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.config.ParserProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import com.romantrippel.linkedinjobparser.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobProcessorTest {

    @Mock
    private LinkedInJobParser parser;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private OpenAiJobFitService openAiJobFitService;
    @Mock
    private OpenAiProperties openAiProperties;
    @Mock
    private TelegramService telegramService;
    @Mock
    private ParserProperties parserProperties;

    @InjectMocks
    private JobProcessor jobProcessor;

    @BeforeEach
    void setup() {
        lenient().when(parserProperties.getJavaKeywords()).thenReturn(List.of("java"));
        lenient().when(parserProperties.getExcludedTitleWords()).thenReturn(List.of("senior"));
        lenient().when(parserProperties.getExcludedLanguages()).thenReturn(List.of("python"));
        lenient().when(parserProperties.getEnglishGeoIds()).thenReturn(List.of("1"));
        lenient().when(parserProperties.getGeoIds()).thenReturn("de:1");
        lenient().when(parserProperties.getFTpr()).thenReturn("r86400");
        lenient().when(parserProperties.getFWt()).thenReturn("2");
        lenient().when(parserProperties.getMaxYearsExperience()).thenReturn(3);

        lenient().when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldSkipJobWithoutJavaBeforeOpenAi() throws Exception {
        Job job = new Job("1", "Backend Developer", "Test", "DE",
                "http://test?geoId=1", "");

        when(parser.parse(anyString())).thenReturn(List.of(job));

        jobProcessor.processJavaJobs();

        // OpenAI не вызывается
        verify(openAiJobFitService, never()).evaluate(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void shouldProcessAndCallOpenAiForValidJob() throws Exception {
        Job job = new Job("1", "Java Developer", "Test", "DE",
                "http://test?geoId=1", "");

        when(parser.parse(anyString())).thenReturn(List.of(job));
        when(parser.fetchDescriptionByJobId("1")).thenReturn("Java 2+ experience");
        when(jobRepository.existsByJobId("1")).thenReturn(false);
        when(openAiProperties.isEnabled()).thenReturn(true);

        JobFitResponse response = new JobFitResponse();
        response.setFitScore(90);

        when(openAiJobFitService.evaluate(any())).thenReturn(response);

        jobProcessor.processJavaJobs();

        verify(jobRepository).save(any(Job.class));
        verify(telegramService).sendJob(any(Job.class));
    }

    @Test
    void shouldSkipJobWithExcludedLanguageEvenIfJavaAbsent() throws Exception {
        Job job = new Job("1", "Backend Developer", "Test", "DE",
                "http://test?geoId=1", "");

        when(parser.parse(anyString())).thenReturn(List.of(job));
        when(jobRepository.existsByJobId("1")).thenReturn(false);
        when(openAiProperties.isEnabled()).thenReturn(true);

        jobProcessor.processJavaJobs();

        // Фильтр по языкам сработал, OpenAI не вызывается
        verify(openAiJobFitService, never()).evaluate(any());
        verify(jobRepository, never()).save(any());
    }
}