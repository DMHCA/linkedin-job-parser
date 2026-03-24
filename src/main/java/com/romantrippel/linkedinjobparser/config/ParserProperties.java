package com.romantrippel.linkedinjobparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {

    /**
     * uk:101165590,spain:105646813,...
     */
    private String geoIds;

    private List<String> englishGeoIds;

    private List<String> javaKeywords;
    private List<String> bigTechKeywords;

    private String fTpr;
    private String fWt;

    private List<String> bigTechCompanies;
    private List<String> excludedLanguages;
    private List<String> excludedTitleWords;

    private int maxYearsExperience;

    private String interval;

    // ===================== CUSTOM MAPPING =====================

    public Map<String, Integer> getGeoIdMap() {
        if (geoIds == null || geoIds.isBlank()) {
            return Map.of();
        }

        Map<String, Integer> result = new LinkedHashMap<>();

        String[] entries = geoIds.split(",");

        for (String entry : entries) {
            String[] parts = entry.split(":");

            if (parts.length != 2) continue;

            String country = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            try {
                result.put(country, Integer.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    // ===================== GETTERS / SETTERS =====================

    public String getGeoIds() {
        return geoIds;
    }

    public void setGeoIds(String geoIds) {
        this.geoIds = geoIds;
    }

    public List<String> getEnglishGeoIds() {
        return englishGeoIds;
    }

    public void setEnglishGeoIds(List<String> englishGeoIds) {
        this.englishGeoIds = englishGeoIds;
    }

    public List<String> getJavaKeywords() {
        return javaKeywords;
    }

    public void setJavaKeywords(List<String> javaKeywords) {
        this.javaKeywords = javaKeywords;
    }

    public List<String> getBigTechKeywords() {
        return bigTechKeywords;
    }

    public void setBigTechKeywords(List<String> bigTechKeywords) {
        this.bigTechKeywords = bigTechKeywords;
    }

    public String getFTpr() {
        return fTpr;
    }

    public void setFTpr(String fTpr) {
        this.fTpr = fTpr;
    }

    public String getFWt() {
        return fWt;
    }

    public void setFWt(String fWt) {
        this.fWt = fWt;
    }

    public List<String> getBigTechCompanies() {
        return bigTechCompanies;
    }

    public void setBigTechCompanies(List<String> bigTechCompanies) {
        this.bigTechCompanies = bigTechCompanies;
    }

    public List<String> getExcludedLanguages() {
        return excludedLanguages;
    }

    public void setExcludedLanguages(List<String> excludedLanguages) {
        this.excludedLanguages = excludedLanguages;
    }

    public List<String> getExcludedTitleWords() {
        return excludedTitleWords;
    }

    public void setExcludedTitleWords(List<String> excludedTitleWords) {
        this.excludedTitleWords = excludedTitleWords;
    }

    public int getMaxYearsExperience() {
        return maxYearsExperience;
    }

    public void setMaxYearsExperience(int maxYearsExperience) {
        this.maxYearsExperience = maxYearsExperience;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }
}