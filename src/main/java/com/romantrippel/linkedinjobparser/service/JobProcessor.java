package com.romantrippel.linkedinjobparser.service;

import com.romantrippel.linkedinjobparser.config.OpenAiProperties;
import com.romantrippel.linkedinjobparser.config.ParserProperties;
import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import com.romantrippel.linkedinjobparser.entity.Job;
import com.romantrippel.linkedinjobparser.enums.JobType;
import com.romantrippel.linkedinjobparser.parser.LinkedInJobParser;
import com.romantrippel.linkedinjobparser.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
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

    public void processJavaJobs() throws Exception {
        log.info("PIPELINE START | type=JAVA");
        processJobs(parserProperties.getJavaKeywords(), JobType.JAVA);
    }

    public void processBigTechJobs() throws Exception {
        log.info("PIPELINE START | type=BIG_TECH");
        processJobs(parserProperties.getBigTechKeywords(), JobType.BIG_TECH);
    }

    // ========================= CORE =========================

    private void processJobs(List<String> keywords, JobType type) throws Exception {

        List<String> locations = parserProperties.getLocations();
        List<String> excludedTitleWords = parserProperties.getExcludedTitleWords();
        List<String> excludedLanguages = parserProperties.getExcludedLanguages();
        List<String> englishLocations = parserProperties.getEnglishLocations();

        for (String keyword : keywords) {
            for (String location : locations) {

                String url = buildUrl(keyword, location);

                try {

                    log.info("SEARCH START | type={} | keyword={} | country={} | url={}",
                            type, keyword, location, url);

                    List<Job> jobs = parser.parse(url);

                    log.info("JOBS FETCHED | type={} | keyword={} | country={} | count={}",
                            type, keyword, location, jobs.size());

                    sleepRandom(
                            parserProperties.getSearchDelayMinMs(),
                            parserProperties.getSearchDelayMaxMs()
                    );

                    for (Job job : jobs) {

                        if (hasExcludedTitleWord(job, excludedTitleWords)) continue;

                        String description = parser.fetchDescriptionByJobId(job.getJobId());
                        job.setDescription(description);

                        if (!languageDetector.isEnglishDescription(
                                description,
                                job.getLocation(),
                                englishLocations
                        )) continue;

                        if (!hasAcceptableExperience(job)) continue;

                        if (type == JobType.JAVA && !containsJava(job)) continue;

                        if (type == JobType.BIG_TECH) {
                            if (!containsJava(job)) {
                                if (!doesNotContainExcludedLanguages(job, excludedLanguages)) {
                                    continue;
                                }
                            }
                        }

                        if (!hasValidJobId(job) || jobRepository.existsByJobId(job.getJobId())) {
                            continue;
                        }

                        evaluateAndAttachOpenAi(job);

                        save(job);

                        sleepRandom(
                                parserProperties.getJobDelayMinMs(),
                                parserProperties.getJobDelayMaxMs()
                        );
                    }

                } catch (Exception e) {

                    log.error(
                            "SEARCH FAILED | type={} | keyword={} | country={} | url={} | error={}",
                            type, keyword, location, url, e.getMessage(), e
                    );
                }
            }
        }
    }

    // ========================= URL =========================

    private String buildUrl(String keyword, String location) {

        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

        return "https://www.linkedin.com/jobs/search/?keywords="
                + encodedKeyword
                + "&location=" + encodedLocation
                + "&f_TPR=" + parserProperties.getFTpr()
                + "&f_WT=" + parserProperties.getFWt()
                + "&sortBy=DD";
    }

    // ========================= HELPERS =========================

    private void evaluateAndAttachOpenAi(Job job) {
        if (!openAiProperties.isEnabled()) return;

        try {
            JobFitResponse response = openAiJobFitService.evaluate(job);
            job.applyFitResponse(response);
        } catch (Exception e) {
            log.error("OPENAI FAILED | jobId={} | error={}", job.getJobId(), e.getMessage(), e);
        }
    }

    private void save(Job job) {
        Job savedJob = jobRepository.save(job);
        telegramService.sendJob(savedJob);

        log.info("JOB SAVED | title={} | location={} | jobId={}",
                savedJob.getTitle(),
                savedJob.getLocation(),
                savedJob.getJobId());
    }

    private void sleepRandom(long minMs, long maxMs) {
        try {
            long delay = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void noticeOfNewDay() {
        LocalDate today = LocalDate.now();

        if (!today.equals(currentDay)) {
            currentDay = today;
            log.info("NEW DAY STARTED | date={}", today);
        }
    }

    // ========================= FILTERS =========================

    private boolean hasExcludedTitleWord(Job job, List<String> words) {
        String title = job.getTitle() == null ? "" : job.getTitle().toLowerCase();
        for (String word : words) {
            if (title.contains(word)) return true;
        }
        return false;
    }

    private boolean containsJava(Job job) {
        String text = ((job.getTitle() == null ? "" : job.getTitle())
                + " "
                + (job.getDescription() == null ? "" : job.getDescription())).toLowerCase();

        return JAVA_PATTERN.matcher(text).find();
    }

    private boolean doesNotContainExcludedLanguages(Job job, List<String> languages) {
        String text = (job.getTitle() + " " + job.getDescription()).toLowerCase();
        for (String lang : languages) {
            if (text.contains(lang)) return false;
        }
        return true;
    }

    private boolean hasValidJobId(Job job) {
        return job.getJobId() != null && !job.getJobId().isBlank();
    }

    private boolean hasAcceptableExperience(Job job) {

        int maxYearsExperience = parserProperties.getMaxYearsExperience();

        String text = (job.getTitle() + " " + job.getDescription()).toLowerCase();

        Pattern pattern = Pattern.compile("(\\d+)\\s*(\\+|to|-)?\\s*(\\d+)?\\s*(years|year|yrs|yr)");
        Matcher matcher = pattern.matcher(text);

        int detectedMinYears = Integer.MAX_VALUE;

        while (matcher.find()) {

            int first = Integer.parseInt(matcher.group(1));
            String secondGroup = matcher.group(3);

            int minYears;

            if (secondGroup != null) {
                int second = Integer.parseInt(secondGroup);
                minYears = Math.min(first, second);
            } else {
                minYears = first;
            }

            detectedMinYears = Math.min(detectedMinYears, minYears);
        }

        return detectedMinYears == Integer.MAX_VALUE
                || detectedMinYears <= maxYearsExperience;
    }
}