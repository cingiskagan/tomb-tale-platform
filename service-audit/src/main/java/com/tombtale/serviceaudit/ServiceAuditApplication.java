package com.tombtale.serviceaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Audit microservice: the domain events, kept in MongoDB.
 */
@SpringBootApplication
public class ServiceAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAuditApplication.class, args);
    }
}
