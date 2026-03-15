package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
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

    public JobProcessor(LinkedInJobParser parser,
                        JobRepository jobRepository,
                        OpenAiJobFitService openAiJobFitService,
                        OpenAiProperties openAiProperties) {
        this.parser = parser;
        this.jobRepository = jobRepository;
        this.openAiJobFitService = openAiJobFitService;
        this.openAiProperties = openAiProperties;
    }

    public void process() throws Exception {
        processJavaJobs();
        processBigTechJobs();
    }

    public void processJavaJobs() throws Exception {
        List<String> urls = buildSearchUrls(splitToList(javaKeywordsRaw));
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preFilter = job ->
                containsJava(job)
                        && !hasExcludedTitleWord(job, excludedTitleWords);

        Predicate<Job> postFilter = job -> hasAcceptableExperience(job);

        processJobs(urls, preFilter, postFilter);
    }

    public void processBigTechJobs() throws Exception {
        List<String> urls = buildSearchUrls(splitToList(bigTechKeywordsRaw));
        List<String> bigTechCompanies = splitToList(bigTechCompaniesRaw);
        List<String> excludedLanguages = splitToList(excludedLanguagesRaw);
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preFilter = job ->
                isBigTechCompany(job, bigTechCompanies)
                        && !hasExcludedTitleWord(job, excludedTitleWords);

        Predicate<Job> postFilter = job ->
                (containsJava(job) || doesNotContainExcludedLanguages(job, excludedLanguages))
                        && hasAcceptableExperience(job);

        processJobs(urls, preFilter, postFilter);
    }

    private void processJobs(List<String> urls,
                             Predicate<Job> preFilter,
                             Predicate<Job> postFilter) throws Exception {

        for (String url : urls) {
            List<Job> jobs = parser.parse(url);

            for (Job job : jobs) {
                if (!preFilter.test(job)) continue;
                if (!hasValidJobId(job)) continue;
                if (jobRepository.existsByJobId(job.getJobId())) continue;

                String description = parser.fetchDescriptionByJobId(job.getJobId());
                job.setDescription(description);

                if (!languageDetector.isEnglishDescription(job.getDescription())) continue;
                if (!postFilter.test(job)) continue;

                save(job);
            }
        }
    }

    private boolean hasValidJobId(Job job) {
        return job.getJobId() != null && !job.getJobId().isBlank();
    }

    private boolean hasExcludedTitleWord(Job job, List<String> excludedTitleWords) {
        String title = safeLower(job.getTitle());
        for (String word : excludedTitleWords) {
            if (title.contains(word)) return true;
        }
        return false;
    }

    private boolean containsJava(Job job) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        return JAVA_PATTERN.matcher(text).find();
    }

    private boolean isBigTechCompany(Job job, List<String> companies) {
        String company = safeLower(job.getCompany());
        for (String item : companies) {
            if (company.contains(item)) return true;
        }
        return false;
    }

    private boolean doesNotContainExcludedLanguages(Job job, List<String> excludedLanguages) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        for (String language : excludedLanguages) {
            if (text.contains(language)) return false;
        }
        return true;
    }

    private boolean hasAcceptableExperience(Job job) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        for (int i = maxYearsExperience + 1; i <= 20; i++) {
            if (text.contains(i + "+")) return false;
        }
        return true;
    }

    private void save(Job job) {
        Job savedJob = jobRepository.save(job);
        System.out.println("NEW JOB SAVED: " + savedJob.getTitle() + " @ " + savedJob.getCompany());
    }

    private List<String> buildSearchUrls(List<String> keywords) {
        List<String> urls = new ArrayList<>();
        List<String> geoIds = extractGeoIds(geoIdsRaw);

        for (String keyword : keywords) {
            for (String geoId : geoIds) {
                urls.add(buildUrl(keyword, geoId)); // 21 URL получится
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
}