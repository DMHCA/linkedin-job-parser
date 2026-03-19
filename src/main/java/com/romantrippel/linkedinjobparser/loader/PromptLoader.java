package com.romantrippel.linkedinjobparser.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

    private static final String PROMPT_FILE = "prompts/job_fit.txt";

    public String loadPrompt() {

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(PROMPT_FILE);

        if (inputStream == null) {
            log.error("Prompt file not found in resources: {}", PROMPT_FILE);
            throw new RuntimeException("Prompt file not found: " + PROMPT_FILE);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            return reader.lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            log.error("Failed to load prompt file: {}", PROMPT_FILE, e);
            throw new RuntimeException("Failed to load prompt file: " + PROMPT_FILE, e);
        }
    }
}