package com.romantrippel.linkedinjobparser.dto;

public class JobFitResponse {

    private int fitScore;
    private String seniority;
    private String stack;
    private String responsibilities;
    private String matchReason;

    public JobFitResponse() {
    }

    public JobFitResponse(int fitScore, String seniority, String stack,
                          String responsibilities, String matchReason) {
        this.fitScore = fitScore;
        this.seniority = seniority;
        this.stack = stack;
        this.responsibilities = responsibilities;
        this.matchReason = matchReason;
    }

    public int getFitScore() {
        return fitScore;
    }

    public void setFitScore(int fitScore) {
        this.fitScore = fitScore;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public String getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(String responsibilities) {
        this.responsibilities = responsibilities;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }
}