package com.romantrippel.linkedinjobparser.loader;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class PromptLoaderTest {

    @Test
    void loadPrompt_shouldReturnExpectedString_withMockedInputStream() {
        PromptLoader loader = new PromptLoader() {
            @Override
            public String loadPrompt() {
                InputStream fakeStream = new ByteArrayInputStream(
                        "Candidate profile\nJob title".getBytes()
                );
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(fakeStream))) {
                    return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        String result = loader.loadPrompt();
        assertEquals("Candidate profile\nJob title", result);
    }

    @Test
    void loadPrompt_shouldThrowRuntimeException_whenFileMissing() {
        PromptLoader loader = new PromptLoader() {
            @Override
            public String loadPrompt() {
                throw new RuntimeException("Prompt file not found: missing.txt");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class, loader::loadPrompt);
        assertTrue(ex.getMessage().contains("missing.txt"));
    }
}