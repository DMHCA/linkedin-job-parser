package com.romantrippel.linkedinjobparser.parser;

import com.romantrippel.linkedinjobparser.entity.Job;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class LinkedInJobParser {

    private static final Logger log =
            LoggerFactory.getLogger(LinkedInJobParser.class);

    private static final Random RANDOM = new Random();

    private static final int MAX_RETRIES = 3;

    // ========================= SEARCH =========================

    public List<Job> parse(String url) {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {
                List<Job> jobs = new ArrayList<>();

                Document doc = fetchDocument(url);

                var jobElements = doc.select("ul.jobs-search__results-list li");

                for (Element jobElement : jobElements) {

                    String title = extractText(jobElement, "h3");
                    String company = extractText(jobElement, "h4");
                    String location = extractText(jobElement, ".job-search-card__location");

                    String link = extractJobLink(jobElement);
                    String jobId = extractJobId(link);

                    if (jobId.isBlank()) {
                        continue;
                    }

                    jobs.add(new Job(jobId, title, company, location, link, ""));
                }

                return jobs;

            } catch (org.jsoup.HttpStatusException e) {

                if (e.getStatusCode() == 429) {

                    long delay = randomDelaySeconds(40, 65);

                    log.warn(
                            "SEARCH RATE LIMITED (429) | attempt={} | url={} | retry_in={}s",
                            attempt,
                            url,
                            delay / 1000
                    );

                    sleep(delay);

                    continue;
                }

                log.error(
                        "SEARCH HTTP ERROR | attempt={} | url={} | status={}",
                        attempt,
                        url,
                        e.getStatusCode(),
                        e
                );

                break;

            } catch (Exception e) {

                log.error(
                        "SEARCH FAILED | attempt={} | url={} | error={}",
                        attempt,
                        url,
                        e.getMessage(),
                        e
                );

                break;
            }
        }

        log.error("SEARCH FAILED FINAL | url={}", url);
        return new ArrayList<>();
    }

    // ========================= DESCRIPTION =========================

    public String fetchDescriptionByJobId(String jobId) {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {
                Document jobDoc = fetchJobDocument(jobId);

                Element descriptionElement = jobDoc.selectFirst(".show-more-less-html__markup");

                if (descriptionElement == null) {
                    descriptionElement = jobDoc.selectFirst(".description__text");
                }

                if (descriptionElement == null) {
                    descriptionElement = jobDoc.selectFirst(".core-section-container__content");
                }

                if (descriptionElement == null) {
                    log.warn("DESCRIPTION EMPTY | jobId={}", jobId);
                    return "";
                }

                String text = descriptionElement.text()
                        .replaceAll("\\s+", " ")
                        .trim();

                if (text.isBlank()) {
                    log.warn("DESCRIPTION BLANK | jobId={}", jobId);
                    return "";
                }

                log.info("DESCRIPTION OK | jobId={} | length={}", jobId, text.length());

                return text;

            } catch (org.jsoup.HttpStatusException e) {

                if (e.getStatusCode() == 429) {

                    long delay = randomDelaySeconds(40, 65);

                    log.warn(
                            "DESCRIPTION RATE LIMITED (429) | attempt={} | jobId={} | retry_in={}s",
                            attempt,
                            jobId,
                            delay / 1000
                    );

                    sleep(delay);

                    continue;
                }

                log.error(
                        "DESCRIPTION HTTP ERROR | jobId={} | status={} | url={}",
                        jobId,
                        e.getStatusCode(),
                        e.getUrl(),
                        e
                );

                return "";

            } catch (Exception e) {

                log.error(
                        "DESCRIPTION FETCH FAILED | jobId={} | error={}",
                        jobId,
                        e.getMessage(),
                        e
                );

                return "";
            }
        }

        log.error("DESCRIPTION FAILED FINAL | jobId={}", jobId);
        return "";
    }

    // ========================= HTTP =========================

    protected Document fetchDocument(String url) throws Exception {

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(15000)
                .get();
    }

    private Document fetchJobDocument(String jobId) throws Exception {
        return fetchDocument(buildPublicJobUrl(jobId));
    }

    // ========================= HELPERS =========================

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long randomDelaySeconds(int min, int max) {
        return (min + RANDOM.nextInt(max - min + 1)) * 1000L;
    }

    private String extractText(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el == null ? "" : el.text().trim();
    }

    private String extractJobLink(Element jobElement) {
        Element linkElement = jobElement.selectFirst("a.base-card__full-link");

        if (linkElement == null) {
            linkElement = jobElement.selectFirst("a");
        }

        if (linkElement == null) {
            return "";
        }

        return linkElement.attr("href").trim();
    }

    private String extractJobId(String link) {
        if (link == null || link.isBlank()) {
            return "";
        }

        String cleanLink = link;

        int questionIndex = cleanLink.indexOf("?");
        if (questionIndex != -1) {
            cleanLink = cleanLink.substring(0, questionIndex);
        }

        String marker = "/view/";
        int index = cleanLink.indexOf(marker);

        if (index != -1) {
            String tail = cleanLink.substring(index + marker.length());

            if (tail.matches("\\d+")) {
                return tail;
            }

            int lastDashIndex = tail.lastIndexOf("-");
            if (lastDashIndex != -1 && lastDashIndex + 1 < tail.length()) {
                String possibleId = tail.substring(lastDashIndex + 1);

                if (possibleId.matches("\\d+")) {
                    return possibleId;
                }
            }
        }

        int paramIndex = link.indexOf("currentJobId=");
        if (paramIndex != -1) {
            int start = paramIndex + "currentJobId=".length();
            int end = start;

            while (end < link.length() && Character.isDigit(link.charAt(end))) {
                end++;
            }

            if (end > start) {
                return link.substring(start, end);
            }
        }

        return "";
    }

    private String buildPublicJobUrl(String jobId) {
        return "https://www.linkedin.com/jobs/view/" + jobId;
    }
}