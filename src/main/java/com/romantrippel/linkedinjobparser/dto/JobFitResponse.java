package com.romantrippel.linkedinjobparser.dto;

public class JobFitResponse {

    private boolean fit;
    private int fitScore;
    private String roleType;
    private String seniorityMatch;
    private String techMatch;
    private String reason;
    private String verdict;

    public JobFitResponse() {
    }

    public boolean isFit() {
        return fit;
    }

    public void setFit(boolean fit) {
        this.fit = fit;
    }

    public int getFitScore() {
        return fitScore;
    }

    public void setFitScore(int fitScore) {
        this.fitScore = fitScore;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getSeniorityMatch() {
        return seniorityMatch;
    }

    public void setSeniorityMatch(String seniorityMatch) {
        this.seniorityMatch = seniorityMatch;
    }

    public String getTechMatch() {
        return techMatch;
    }

    public void setTechMatch(String techMatch) {
        this.techMatch = techMatch;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }
}