package com.financialcrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class FinancialCrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialCrmApplication.class, args);
    }
}
