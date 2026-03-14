package com.romantrippel.linkedinjobparser.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class JobLanguageDetector {

    private static final Set<String> ENGLISH_WORDS = Set.of(
            "the", "and", "with", "for", "you", "your", "are", "our", "will", "have",
            "work", "team", "experience", "role", "requirements", "responsibilities",
            "skills", "engineer", "development", "design", "building", "support"
    );

    private static final Set<String> SPANISH_WORDS = Set.of(
            "el", "la", "los", "las", "de", "del", "con", "para", "que", "una",
            "un", "años", "experiencia", "requisitos", "responsabilidades",
            "desarrollo", "trabajo", "equipo", "conocimiento", "persona"
    );

    private static final Set<String> PORTUGUESE_WORDS = Set.of(
            "o", "a", "os", "as", "de", "do", "da", "com", "para", "que",
            "uma", "um", "não", "anos", "experiência", "requisitos",
            "responsabilidades", "desenvolvimento", "trabalho", "equipe"
    );

    public boolean isEnglishDescription(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = normalize(text);
        String[] words = normalized.split("\\s+");

        int englishScore = countMatches(words, ENGLISH_WORDS);
        int spanishScore = countMatches(words, SPANISH_WORDS);
        int portugueseScore = countMatches(words, PORTUGUESE_WORDS);

        int maxForeignScore = Math.max(spanishScore, portugueseScore);

        if (englishScore < 5) {
            return false;
        }

        return englishScore >= maxForeignScore * 2;
    }

    private int countMatches(String[] words, Set<String> dictionary) {
        int score = 0;

        for (String word : words) {
            if (dictionary.contains(word)) {
                score++;
            }
        }

        return score;
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}