package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.config.ParserProperties;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import com.romantrippel.linkedinjobparser.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private static final Pattern JAVA_PATTERN =
            Pattern.compile("(?i)(\\bjava\\b|\\bjava\\d+\\b)");

    private final LinkedInJobParser parser;
    private final JobRepository jobRepository;
    private final JobLanguageDetector languageDetector = new JobLanguageDetector();
    private final OpenAiJobFitService openAiJobFitService;
    private final OpenAiProperties openAiProperties;
    private final TelegramService telegramService;
    private final ParserProperties parserProperties;

    private LocalDate currentDay;

    public JobProcessor(LinkedInJobParser parser,
                        JobRepository jobRepository,
                        OpenAiJobFitService openAiJobFitService,
                        OpenAiProperties openAiProperties,
                        TelegramService telegramService,
                        ParserProperties parserProperties) {
        this.parser = parser;
        this.jobRepository = jobRepository;
        this.openAiJobFitService = openAiJobFitService;
        this.openAiProperties = openAiProperties;
        this.telegramService = telegramService;
        this.parserProperties = parserProperties;
    }

    public void process() throws Exception {
        noticeOfNewDay();
        processJavaJobs();
        processBigTechJobs();
    }

    // ========================= JAVA FLOW =========================

    public void processJavaJobs() throws Exception {

        List<String> urls = buildSearchUrls(parserProperties.getJavaKeywords());
        List<String> excludedTitleWords = parserProperties.getExcludedTitleWords();

        Predicate<Job> preFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && containsJava(job)
                        && hasAcceptableExperience(job);

        processJobs(urls, preFilter);
    }

    // ========================= BIG TECH FLOW =========================

    public void processBigTechJobs() throws Exception {

        List<String> urls = buildSearchUrls(parserProperties.getBigTechCompanies());
        List<String> excludedTitleWords = parserProperties.getExcludedTitleWords();

        Predicate<Job> preFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && hasAcceptableExperience(job);

        processJobs(urls, preFilter);
    }

    // ========================= MAIN PROCESSING =========================

    private void processJobs(List<String> urls, Predicate<Job> preFilter) throws Exception {

        List<String> excludedLanguages = parserProperties.getExcludedLanguages();
        List<String> englishGeoIds = parserProperties.getEnglishGeoIds();

        for (String url : urls) {
            try {
                List<Job> jobs = parser.parse(url);
                sleepRandom(3000, 7000);

                for (Job job : jobs) {

                    if (!preFilter.test(job)) continue;

                    if (!hasValidJobId(job) || jobRepository.existsByJobId(job.getJobId())) continue;

                    sleepRandom(2000, 5000);
                    String description = parser.fetchDescriptionByJobId(job.getJobId());
                    job.setDescription(description);

                    if (!languageDetector.isEnglishDescription(
                            description,
                            extractGeoIdFromUrl(job.getLink()),
                            englishGeoIds
                    )) continue;

                    // Additional Big Tech post-filter
                    if (!containsJava(job) && !doesNotContainExcludedLanguages(job, excludedLanguages)) continue;

                    // OpenAI evaluation
                    evaluateAndAttachOpenAi(job);

                    save(job);
                }

            } catch (Exception e) {
                System.out.println("FAILED URL: " + url + " Reason: " + e.getMessage());
            }
        }
    }

    private String extractGeoIdFromUrl(String url) {
        int index = url.indexOf("geoId=");
        if (index == -1) return null;
        int end = url.indexOf("&", index);
        if (end == -1) end = url.length();
        return url.substring(index + 6, end);
    }

    private void evaluateAndAttachOpenAi(Job job) {
        if (!openAiProperties.isEnabled()) return;

        try {
            JobFitResponse response = openAiJobFitService.evaluate(job);
            job.applyFitResponse(response);
        } catch (Exception e) {
            System.out.println("OpenAI evaluation failed for jobId=" + job.getJobId() +
                    ", reason=" + e.getMessage());
        }
    }

    private boolean hasValidJobId(Job job) {
        return job.getJobId() != null && !job.getJobId().isBlank();
    }

    // ========================= FILTER METHODS =========================

    private boolean hasExcludedTitleWord(Job job, List<String> words) {
        String title = safeLower(job.getTitle());
        for (String word : words) {
            if (title.contains(word)) return true;
        }
        return false;
    }

    private boolean containsJava(Job job) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        return JAVA_PATTERN.matcher(text).find();
    }

    private boolean hasAcceptableExperience(Job job) {

        int maxYearsExperience = parserProperties.getMaxYearsExperience();

        String text = safeLower(job.getTitle()) + " " + safeLower(job.getDescription());

        Pattern pattern = Pattern.compile("(\\d+)\\s*\\+?\\s*(years|year|yrs|yr)");

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            if (years > maxYearsExperience) {
                return false;
            }
        }

        return true;
    }

    private boolean doesNotContainExcludedLanguages(Job job, List<String> languages) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        for (String lang : languages) {
            if (text.contains(lang)) return false;
        }
        return true;
    }

    // ========================= URL GENERATION =========================

    private List<String> buildSearchUrls(List<String> keywords) {

        Map<String, Integer> geoMap = parseGeoIds(parserProperties.getGeoIds());
        List<String> urls = new ArrayList<>();

        for (String keyword : keywords) {
            for (Integer geoId : geoMap.values()) {
                urls.add(buildUrl(keyword, geoId));
            }
        }

        return urls;
    }

    private String buildUrl(String keyword, Integer geoId) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        return "https://www.linkedin.com/jobs/search/?keywords="
                + encodedKeyword
                + "&geoId=" + geoId
                + "&f_TPR=" + parserProperties.getFTpr()
                + "&f_WT=" + parserProperties.getFWt();
    }

    // ========================= HELPERS =========================

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase();
    }

    private void save(Job job) {
        Job savedJob = jobRepository.save(job);
        telegramService.sendJob(savedJob);

        System.out.println("NEW JOB SAVED: " + savedJob.getTitle() + " @ " + savedJob.getCompany());
    }

    private void sleepRandom(long minMs, long maxMs) {
        try {
            long delay = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Integer> parseGeoIds(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();

        Map<String, Integer> result = new LinkedHashMap<>();

        String[] entries = raw.split(",");
        for (String entry : entries) {

            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            String country = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            try {
                result.put(country, Integer.valueOf(value));
            } catch (NumberFormatException e) {
                System.out.println("Invalid geoId: " + entry);
            }
        }

        return result;
    }

    private void noticeOfNewDay() {

        LocalDate today = LocalDate.now();

        if (!today.equals(currentDay)) {
            currentDay = today;
            log.info("New day started: {}", today);
        }
    }
}