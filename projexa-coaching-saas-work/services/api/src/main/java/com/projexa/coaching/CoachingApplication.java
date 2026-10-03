package com.projexa.coaching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoachingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoachingApplication.class, args);
    }
}
