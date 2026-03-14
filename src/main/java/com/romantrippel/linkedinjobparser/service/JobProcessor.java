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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        Predicate<Job> preDescriptionFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && containsJava(job);

        Predicate<Job> postDescriptionFilter = this::shouldSaveByOpenAiOrDefault;

        processJobs(urls, preDescriptionFilter, postDescriptionFilter);
    }

    public void processBigTechJobs() throws Exception {
        List<String> urls = buildSearchUrls(splitToList(bigTechKeywordsRaw));
        List<String> bigTechCompanies = splitToList(bigTechCompaniesRaw);
        List<String> excludedLanguages = splitToList(excludedLanguagesRaw);
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preDescriptionFilter = job ->
                !hasExcludedTitleWord(job, excludedTitleWords)
                        && isBigTechCompany(job, bigTechCompanies);

        Predicate<Job> postDescriptionFilter = job ->
                (containsJava(job) || doesNotContainExcludedLanguages(job, excludedLanguages))
                        && shouldSaveByOpenAiOrDefault(job);

        processJobs(urls, preDescriptionFilter, postDescriptionFilter);
    }

    private void processJobs(List<String> urls,
                             Predicate<Job> preDescriptionFilter,
                             Predicate<Job> postDescriptionFilter) throws Exception {

        for (String url : urls) {
            List<Job> jobs = parser.parse(url);

            for (Job job : jobs) {
                if (!preDescriptionFilter.test(job)) {
                    continue;
                }

                if (!hasValidJobId(job)) {
                    continue;
                }

                if (jobRepository.existsByJobId(job.getJobId())) {
                    continue;
                }

                String description = parser.fetchDescriptionByJobId(job.getJobId());
                job.setDescription(description);

                if (!languageDetector.isEnglishDescription(job.getDescription())) {
                    continue;
                }

                if (!postDescriptionFilter.test(job)) {
                    continue;
                }

                save(job);
            }
        }
    }

    private boolean shouldSaveByOpenAiOrDefault(Job job) {

        if (!openAiProperties.isEnabled()) {
            return true;
        }

        JobFitResponse response = openAiJobFitService.evaluate(job);

        System.out.println("OPENAI EVALUATION");
        System.out.println("Title: " + job.getTitle());
        System.out.println("Company: " + job.getCompany());
        System.out.println("Location: " + job.getLocation());
        System.out.println("Link: " + job.getLink());
        System.out.println("Score: " + response.getFitScore());
        System.out.println("RoleType: " + response.getRoleType());
        System.out.println("Seniority: " + response.getSeniorityMatch());
        System.out.println("TechMatch: " + response.getTechMatch());
        System.out.println("Verdict: " + response.getVerdict());
        System.out.println("Reason: " + response.getReason());
        System.out.println("-----------------------------");

        if (!response.isFit()) {
            return false;
        }

        return response.getFitScore() >= 60;
    }

    private boolean hasValidJobId(Job job) {
        return job.getJobId() != null && !job.getJobId().isBlank();
    }

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
                + "&geoId="
                + geoId
                + "&f_TPR="
                + timePostedRaw
                + "&f_WT="
                + workTypeRaw;
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

        if (colonIndex == -1) {
            return value.trim();
        }

        return value.substring(colonIndex + 1).trim();
    }

    private boolean hasExcludedTitleWord(Job job, List<String> excludedTitleWords) {
        String title = safeLower(job.getTitle());

        for (String word : excludedTitleWords) {
            if (title.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsJava(Job job) {
        String text = buildSearchableText(job);
        return JAVA_PATTERN.matcher(text).find();
    }

    private boolean isBigTechCompany(Job job, List<String> companies) {
        String company = safeLower(job.getCompany());

        for (String item : companies) {
            if (company.contains(item)) {
                return true;
            }
        }

        return false;
    }

    private boolean doesNotContainExcludedLanguages(Job job, List<String> excludedLanguages) {
        String text = buildSearchableText(job);

        for (String language : excludedLanguages) {
            if (text.contains(language)) {
                return false;
            }
        }

        return true;
    }

    private void save(Job job) {
        Job savedJob = jobRepository.save(job);

        System.out.println("NEW JOB SAVED");
        System.out.println("Id: " + savedJob.getId());
        System.out.println("JobId: " + savedJob.getJobId());
        System.out.println("Title: " + savedJob.getTitle());
        System.out.println("Company: " + savedJob.getCompany());
        System.out.println("Location: " + savedJob.getLocation());
        System.out.println("Link: " + savedJob.getLink());
        System.out.println("Description: " + savedJob.getDescription());
        System.out.println();
    }

    private String buildSearchableText(Job job) {
        return (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
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