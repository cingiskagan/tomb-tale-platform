package com.tombtale.servicecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Entry point for the Commerce microservice.
 * <p>
 * Manages in-game purchase orders with ACID transactional guarantees,
 * backed by PostgreSQL (JPA + QueryDSL) and RabbitMQ event publishing.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ServiceCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCommerceApplication.class, args);
    }
}

