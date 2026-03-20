package com.romantrippel.linkedinjobparser.parser;

import com.romantrippel.linkedinjobparser.entity.Job;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class LinkedInJobParserTest {

    @Test
    void parse_shouldSkipJob_whenJobIdMissing() throws Exception {
        String html = """
                <html>
                <body>
                  <ul class="jobs-search__results-list">
                    <li>
                      <a class="base-card__full-link" href="#"></a>
                      <h3>Java Developer</h3>
                      <h4>Company</h4>
                      <span class="job-search-card__location">Berlin</span>
                    </li>
                  </ul>
                </body>
                </html>
                """;

        Document fakeDocument = Jsoup.parse(html);

        LinkedInJobParser parser = spy(new LinkedInJobParser());
        doReturn(fakeDocument)
                .when(parser)
                .fetchDocument(anyString());

        List<Job> jobs = parser.parse("fake-url");

        assertEquals(0, jobs.size()); // job пропущен
    }

    @Test
    void parse_shouldReturnJobs_whenHtmlIsValid() throws Exception {
        String html = """
                <html>
                <body>
                  <ul class="jobs-search__results-list">
                    <li>
                      <a class="base-card__full-link"
                         href="https://www.linkedin.com/jobs/view/12345"></a>
                      <h3>Java Developer</h3>
                      <h4>Company</h4>
                      <span class="job-search-card__location">Berlin</span>
                    </li>
                  </ul>
                </body>
                </html>
                """;

        Document fakeDocument = Jsoup.parse(html);

        LinkedInJobParser parser = spy(new LinkedInJobParser());
        doReturn(fakeDocument)
                .when(parser)
                .fetchDocument(anyString());

        List<Job> jobs = parser.parse("fake-url");

        assertEquals(1, jobs.size());
        Job job = jobs.get(0);
        assertEquals("12345", job.getJobId());
        assertEquals("Java Developer", job.getTitle());
        assertEquals("Company", job.getCompany());
        assertEquals("Berlin", job.getLocation());
    }
}