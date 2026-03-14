package com.romantrippel.linkedinjobparser.parser;

import com.romantrippel.linkedinjobparser.entity.Job;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LinkedInJobParser {

    public List<Job> parse(String url) throws Exception {
        List<Job> jobs = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(15000)
                .get();

        Elements jobElements = doc.select("ul.jobs-search__results-list li");

        for (Element jobElement : jobElements) {
            String title = jobElement.select("h3").text();
            String company = jobElement.select("h4").text();
            String location = jobElement.select(".job-search-card__location").text();
            String link = extractJobLink(jobElement);
            String jobId = extractJobId(link);

            if (jobId.isBlank()) {
                continue;
            }

            jobs.add(new Job(jobId, title, company, location, link, ""));
        }

        return jobs;
    }

    public String fetchDescriptionByJobId(String jobId) {
        try {
            String publicUrl = buildPublicJobUrl(jobId);

            Document jobDoc = Jsoup.connect(publicUrl)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(15000)
                    .get();

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

    private String extractJobLink(Element jobElement) {
        String link = jobElement.select("a.base-card__full-link").attr("href");

        if (link == null || link.isBlank()) {
            link = jobElement.select("a").attr("href");
        }

        return link == null ? "" : link.trim();
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

        String viewMarker = "/view/";
        int viewIndex = cleanLink.indexOf(viewMarker);

        if (viewIndex != -1) {
            String tail = cleanLink.substring(viewIndex + viewMarker.length());

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

        String currentJobIdMarker = "currentJobId=";
        int currentJobIdIndex = link.indexOf(currentJobIdMarker);

        if (currentJobIdIndex != -1) {
            int start = currentJobIdIndex + currentJobIdMarker.length();
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