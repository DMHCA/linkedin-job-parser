package com.romantrippel.linkedinjobparser.service;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import com.google.common.base.Optional;

@Component
public class JobLanguageDetector {

    private final LanguageDetector languageDetector;

    public JobLanguageDetector() {
        try {
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            this.languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize language detector", e);
        }
    }

    public boolean isEnglishDescription(String description, String geoId, List<String> englishGeoIds) {
        if (englishGeoIds.contains(geoId)) return true;

        if (description == null || description.isBlank()) return false;

        Optional<LdLocale> lang = languageDetector.detect(description);
        return lang.isPresent() && "en".equals(lang.get().getLanguage());
    }
}