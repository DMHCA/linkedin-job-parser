package com.romantrippel.linkedinjobparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {

    private List<String> locations;
    private List<String> englishLocations;

    private List<String> javaKeywords;
    private List<String> bigTechKeywords;

    private String fTpr;
    private String fWt;

    private List<String> bigTechCompanies;
    private List<String> excludedLanguages;
    private List<String> excludedTitleWords;

    private int maxYearsExperience;

    private String interval;

    // Delays
    private long searchDelayMinMs;
    private long searchDelayMaxMs;
    private long jobDelayMinMs;
    private long jobDelayMaxMs;

    // ===================== GETTERS / SETTERS =====================

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getEnglishLocations() {
        return englishLocations;
    }

    public void setEnglishLocations(List<String> englishLocations) {
        this.englishLocations = englishLocations;
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

    // ===================== DELAYS =====================

    public long getSearchDelayMinMs() {
        return searchDelayMinMs;
    }

    public void setSearchDelayMinMs(long searchDelayMinMs) {
        this.searchDelayMinMs = searchDelayMinMs;
    }

    public long getSearchDelayMaxMs() {
        return searchDelayMaxMs;
    }

    public void setSearchDelayMaxMs(long searchDelayMaxMs) {
        this.searchDelayMaxMs = searchDelayMaxMs;
    }

    public long getJobDelayMinMs() {
        return jobDelayMinMs;
    }

    public void setJobDelayMinMs(long jobDelayMinMs) {
        this.jobDelayMinMs = jobDelayMinMs;
    }

    public long getJobDelayMaxMs() {
        return jobDelayMaxMs;
    }

    public void setJobDelayMaxMs(long jobDelayMaxMs) {
        this.jobDelayMaxMs = jobDelayMaxMs;
    }
}