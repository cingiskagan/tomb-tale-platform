package com.tombtale.serviceplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ServicePlayerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ServicePlayerApplication.class, args);
    }

}
