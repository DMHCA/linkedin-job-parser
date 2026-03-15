package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import com.romantrippel.linkedinjobparser.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

@Service
public class JobProcessor {

    private final LinkedInJobParser parser;
    private final JobRepository jobRepository;
    private final JobLanguageDetector languageDetector = new JobLanguageDetector();

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

    public JobProcessor(LinkedInJobParser parser, JobRepository jobRepository) {
        this.parser = parser;
        this.jobRepository = jobRepository;
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
                containsJava(job)
                        && !hasExcludedTitleWord(job, excludedTitleWords);

        processJobs(urls, preFilter, job -> true);
    }

    // ========================= BIG TECH FLOW =========================

    public void processBigTechJobs() throws Exception {

        List<String> urls = buildSearchUrls(splitToList(bigTechKeywordsRaw));
        List<String> bigTechCompanies = splitToList(bigTechCompaniesRaw);
        List<String> excludedLanguages = splitToList(excludedLanguagesRaw);
        List<String> excludedTitleWords = splitToList(excludedTitleWordsRaw);

        Predicate<Job> preFilter = job ->
                isBigTechCompany(job, bigTechCompanies)
                        && !hasExcludedTitleWord(job, excludedTitleWords);

        Predicate<Job> postFilter = job ->
                containsJava(job)
                        || doesNotContainExcludedLanguages(job, excludedLanguages);

        processJobs(urls, preFilter, postFilter);
    }

    // ========================= MAIN PROCESSING =========================

    private void processJobs(List<String> urls,
                             Predicate<Job> preFilter,
                             Predicate<Job> postFilter) throws Exception {

        for (String url : urls) {

            List<Job> jobs = parser.parse(url);

            for (Job job : jobs) {

                if (!preFilter.test(job)) continue;

                if (jobRepository.existsByJobId(job.getJobId())) continue;

                String description = parser.fetchDescriptionByJobId(job.getJobId());
                job.setDescription(description);

                if (!languageDetector.isEnglishDescription(job.getDescription())) continue;

                if (!postFilter.test(job)) continue;

                jobRepository.save(job);

                System.out.println("NEW JOB: " + job.getTitle() + " @ " + job.getCompany());
            }
        }
    }

    // ========================= FILTER METHODS =========================

    private boolean containsJava(Job job) {
        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();
        return text.contains("java");
    }

    private boolean isBigTechCompany(Job job, List<String> companies) {

        String company = safeLower(job.getCompany());

        for (String c : companies) {
            if (company.contains(c)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasExcludedTitleWord(Job job, List<String> words) {

        String title = safeLower(job.getTitle());

        for (String word : words) {
            if (title.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private boolean doesNotContainExcludedLanguages(Job job, List<String> languages) {

        String text = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase();

        for (String lang : languages) {
            if (text.contains(lang)) {
                return false;
            }
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

        List<String> result = new ArrayList<>();

        String[] parts = raw.split(",");

        for (String part : parts) {

            part = part.trim();

            int index = part.indexOf(":");

            if (index != -1) {
                result.add(part.substring(index + 1));
            }
        }

        return result;
    }

    // ========================= HELPERS =========================

    private List<String> splitToList(String raw) {

        String[] parts = raw.split(",");

        List<String> result = new ArrayList<>();

        for (String p : parts) {
            result.add(p.trim().toLowerCase());
        }

        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase();
    }
}