package com.romantrippel.linkedinjobparser.parser;

import com.romantrippel.linkedinjobparser.entity.Job;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LinkedInJobParser {

    public List<Job> parse(String url) throws Exception {
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
                throw new RuntimeException("LinkedIn rate limit hit (429) for url: " + url, e);
            }
            throw e;
        }
    }

    public String fetchDescriptionByJobId(String jobId) {
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
                return "";
            }

            return descriptionElement.text()
                    .replaceAll("\\s+", " ")
                    .trim();

        } catch (Exception e) {
            return "";
        }
    }

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
        String publicUrl = buildPublicJobUrl(jobId);
        return fetchDocument(publicUrl);
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