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

        when(parserProperties.getJavaKeywords()).thenReturn(List.of("java"));
        when(parserProperties.getExcludedTitleWords()).thenReturn(List.of("senior"));
        when(parserProperties.getExcludedLanguages()).thenReturn(List.of("python"));
        when(parserProperties.getLocations()).thenReturn(List.of("Germany"));
        when(parserProperties.getEnglishLocations()).thenReturn(List.of("United Kingdom", "Ireland"));
        when(parserProperties.getFTpr()).thenReturn("r3600");
        when(parserProperties.getFWt()).thenReturn("1,2");
        when(parserProperties.getMaxYearsExperience()).thenReturn(3);

        when(jobRepository.existsByJobId("1")).thenReturn(false);
        when(openAiProperties.isEnabled()).thenReturn(true);

        when(jobRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldProcessAndCallOpenAiForValidJob() throws Exception {

        Job job = new Job(
                "1",
                "Java Spring Backend Developer",
                "Java Spring 3+ years experience",
                "Germany",
                "http://test",
                ""
        );

        when(parser.parse(anyString())).thenReturn(List.of(job));
        when(parser.fetchDescriptionByJobId("1")).thenReturn("Java Spring 3+ years experience");

        JobFitResponse response = new JobFitResponse();
        response.setFitScore(95);

        when(openAiJobFitService.evaluate(any())).thenReturn(response);

        jobProcessor.processJavaJobs();

        verify(jobRepository).save(any(Job.class));
        verify(telegramService).sendJob(any(Job.class));
    }
}