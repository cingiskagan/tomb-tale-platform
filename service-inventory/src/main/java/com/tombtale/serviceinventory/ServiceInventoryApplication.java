package com.tombtale.serviceinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Inventory microservice: the item catalog and the items players hold.
 */
@SpringBootApplication
public class ServiceInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceInventoryApplication.class, args);
    }
}
