package com.romantrippel.linkedinjobparser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobLanguageDetectorTest {

    private JobLanguageDetector detector;
    private List<String> englishGeoIds;

    @BeforeEach
    void setUp() {
        detector = new JobLanguageDetector();
        englishGeoIds = List.of("101165590", "104738515"); // UK and Ireland
    }

    @Test
    void testFastPathEnglishGeoId() {
        String description = "This is a text in another language";
        String geoId = "101165590"; // UK
        assertTrue(detector.isEnglishDescription(description, geoId, englishGeoIds),
                "Fast path: English geoId should return true");
    }

    @Test
    void testEnglishText() {
        String description = "We are looking for a Java developer with experience in Spring";
        String geoId = "105646813"; // Spain
        assertTrue(detector.isEnglishDescription(description, geoId, englishGeoIds),
                "English text should be detected as English");
    }

    @Test
    void testNonEnglishText() {
        String description = "Nous recherchons un développeur Java avec expérience en Spring";
        String geoId = "105646813"; // Spain
        assertFalse(detector.isEnglishDescription(description, geoId, englishGeoIds),
                "Non-English text should return false");
    }

    @Test
    void testNullOrBlankDescription() {
        String geoId = "105646813"; // Spain
        assertFalse(detector.isEnglishDescription(null, geoId, englishGeoIds),
                "Null description should return false");
        assertFalse(detector.isEnglishDescription("", geoId, englishGeoIds),
                "Empty description should return false");
        assertFalse(detector.isEnglishDescription("   ", geoId, englishGeoIds),
                "Blank description should return false");
    }
}