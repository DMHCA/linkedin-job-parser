package com.romantrippel.linkedinjobparser.repository;

import com.romantrippel.linkedinjobparser.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    boolean existsByJobId(String jobId);
}