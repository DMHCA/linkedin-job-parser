package com.romantrippel.linkedinjobparser.scheduler;

import com.romantrippel.linkedinjobparser.service.JobProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    @Mock
    private JobProcessor processor;

    @InjectMocks
    private JobScheduler scheduler;

    @Test
    void shouldCallProcessor() throws Exception {
        scheduler.runParser();

        verify(processor).process();
    }
}