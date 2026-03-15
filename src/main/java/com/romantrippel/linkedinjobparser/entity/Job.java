package com.romantrippel.linkedinjobparser.entity;

import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, length = 1000)
    private String link;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "fit_score")
    private Integer fitScore;

    private String seniority;

    private String stack;

    @Column(columnDefinition = "text")
    private String responsibilities;

    @Column(name = "match_reason", columnDefinition = "text")
    private String matchReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Job() {
    }

    // Новый конструктор для парсера
    public Job(String jobId, String title, String company, String location, String link, String description) {
        this.jobId = jobId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.link = link;
        this.description = description;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public void applyFitResponse(JobFitResponse response) {
        this.fitScore = response.getFitScore();
        this.seniority = response.getSeniority();
        this.stack = response.getStack();
        this.responsibilities = response.getResponsibilities();
        this.matchReason = response.getMatchReason();
    }

    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFitScore() {
        return fitScore;
    }

    public String getSeniority() {
        return seniority;
    }

    public String getStack() {
        return stack;
    }

    public String getResponsibilities() {
        return responsibilities;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}