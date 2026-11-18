package com.tombtale.serviceplayer;

import com.tombtale.commons.outbox.OutboxPersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@Import(OutboxPersistence.class)
public class ServicePlayerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ServicePlayerApplication.class, args);
    }

}
