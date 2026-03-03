package com.bank.progress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProgressToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgressToolApplication.class, args);
    }
}
