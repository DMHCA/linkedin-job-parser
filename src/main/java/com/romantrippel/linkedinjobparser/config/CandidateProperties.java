package com.romantrippel.linkedinjobparser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CandidateProperties {

    @Value("${candidate.profile}")
    private String profile;

    public String getProfile() {
        return profile;
    }
}