package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import com.romantrippel.linkedinjobparser.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Service
public class JobProcessor {

    private static final Pattern JAVA_PATTERN =
            Pattern.compile("(?i)(\\bjava\\b|\\bjava\\d+\\b)");

    private final LinkedInJobParser parser;
    private final JobRepository jobRepository;
    private final JobLanguageDetector languageDetector = new JobLanguageDetector();
    private final OpenAiJobFitService openAiJobFitService;
    private final OpenAiProperties openAiProperties;
    private final TelegramService telegramService;

    @Value("${parser.geo-ids}")
    private String geoIdsRaw;

    @Value("${parser.java-keywords}")
    private String javaKeywordsRaw;

    @Value("${parser.big-tech-keywords}")
    private String bigTechKeywordsRaw;

    @Value("${parser.f-tpr}")
    private String timePostedRaw;

    @Value("${parser.f-wt}")
    private String workTypeRaw;

    @Value("${parser.big-tech-companies}")
    private String bigTechCompaniesRaw;

    @Value("${parser.excluded-languages}")
    private String excludedLanguagesRaw;

    @Value("${parser.excluded-title-words}")
    private String excludedTitleWordsRaw;

    @Value("${parser.max-years-experience}")
    private int maxYearsExperience;

    @Value("${parser.english-geo-ids}")
    private List<String> englishGeoIds;

    public JobProcessor(LinkedInJobParser parser,
                        JobRepository jobRepository,
                        OpenAiJobFitService openAiJobFitService,
                        OpenAiProperties openAiProperties,
                        TelegramService telegramService) {
        this.parser = parser;
        this.jobRepository = jobRepository;
        this.openAiJobFitService = openAiJobFitService;
        this.openAiProperties = openAiProperties;
        this.telegramService = telegramService;
    }

    public void process() throws Exception {
        processJavaJobs();
        processBigTechJobs();
    }

    // ========================= JAVA FLOW =========================
    public void processJavaJobs() throws Exception {
        List<String> urls = buildSearchUrls(splitToList(javaKeywordsRaw));
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && containsJava(job)
                        && hasAcceptableExperience(job);

        processJobs(urls, preFilter);
    }

    // ========================= BIG TECH FLOW =========================
    public void processBigTechJobs() throws Exception {
        List<String> urls = buildSearchUrls(splitToList(bigTechKeywordsRaw));
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && hasAcceptableExperience(job);

        processJobs(urls, preFilter);
    }

    // ========================= MAIN PROCESSING =========================
    private void processJobs(List<String> urls, Predicate<Job> preFilter) throws Exception {

        List<String> excludedLanguages = splitToList(excludedLanguagesRaw);
        List<String> bigTechCompanies = splitToList(bigTechCompaniesRaw);

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

                    if (!languageDetector.isEnglishDescription(description, extractGeoIdFromUrl(job.getLink()), englishGeoIds)) continue;

                    // OpenAI evaluation
                    evaluateAndAttachOpenAi(job);

                    // Additional Big Tech post-filter
                    if (!containsJava(job) && !doesNotContainExcludedLanguages(job, excludedLanguages)) continue;

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
        String text = safeLower(job.getTitle()) + " " + safeLower(job.getDescription());
        for (int i = maxYearsExperience + 1; i <= 20; i++) {
            if (text.contains(i + "+")) return false;
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
        List<String> urls = new ArrayList<>();
        List<String> geoIds = extractGeoIds(geoIdsRaw);

        for (String keyword : keywords) {
            for (String geoId : geoIds) {
                urls.add(buildUrl(keyword, geoId));
            }
        }
        return urls;
    }

    private String buildUrl(String keyword, String geoId) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return "https://www.linkedin.com/jobs/search/?keywords="
                + encodedKeyword
                + "&geoId=" + geoId
                + "&f_TPR=" + timePostedRaw
                + "&f_WT=" + workTypeRaw;
    }

    private List<String> extractGeoIds(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::extractGeoId)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String extractGeoId(String value) {
        int colonIndex = value.indexOf(":");
        return colonIndex == -1 ? value.trim() : value.substring(colonIndex + 1).trim();
    }

    // ========================= HELPERS =========================
    private List<String> splitToList(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .toList();
    }

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
}