package com.romantrippel.linkedinjobparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkedinJobParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkedinJobParserApplication.class, args);
	}

}