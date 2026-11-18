package com.tombtale.serviceinventory;

import com.tombtale.commons.outbox.OutboxPersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Entry point for the Inventory microservice: the item catalog and the items players hold.
 */
@SpringBootApplication
@Import(OutboxPersistence.class)
public class ServiceInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceInventoryApplication.class, args);
    }
}
