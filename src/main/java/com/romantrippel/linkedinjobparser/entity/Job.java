package com.romantrippel.linkedinjobparser.entity;

import com.romantrippel.linkedinjobparser.dto.JobFitResponse;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_job_id", columnNames = "job_id")
})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, length = 1000)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Boolean fit;

    @Column(name = "fit_score")
    private Integer fitScore;

    @Column(name = "role_type")
    private String roleType;

    @Column(name = "seniority_match")
    private String seniorityMatch;

    @Column(name = "tech_match")
    private String techMatch;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column
    private String verdict;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Job() {
    }

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
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public void applyFitResponse(JobFitResponse response) {
        if (response == null) {
            return;
        }

        this.fit = response.isFit();
        this.fitScore = response.getFitScore();
        this.roleType = response.getRoleType();
        this.seniorityMatch = response.getSeniorityMatch();
        this.techMatch = response.getTechMatch();
        this.reason = response.getReason();
        this.verdict = response.getVerdict();
        this.coverLetter = response.getCoverLetter();
    }

    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getLink() {
        return link;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getFit() {
        return fit;
    }

    public Integer getFitScore() {
        return fitScore;
    }

    public String getRoleType() {
        return roleType;
    }

    public String getSeniorityMatch() {
        return seniorityMatch;
    }

    public String getTechMatch() {
        return techMatch;
    }

    public String getReason() {
        return reason;
    }

    public String getVerdict() {
        return verdict;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFit(Boolean fit) {
        this.fit = fit;
    }

    public void setFitScore(Integer fitScore) {
        this.fitScore = fitScore;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public void setSeniorityMatch(String seniorityMatch) {
        this.seniorityMatch = seniorityMatch;
    }

    public void setTechMatch(String techMatch) {
        this.techMatch = techMatch;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}