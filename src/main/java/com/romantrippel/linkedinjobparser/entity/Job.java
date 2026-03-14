package com.romantrippel.linkedinjobparser.entity;

import jakarta.persistence.*;

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
}