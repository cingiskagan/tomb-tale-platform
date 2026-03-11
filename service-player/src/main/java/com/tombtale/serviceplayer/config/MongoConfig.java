package com.tombtale.serviceplayer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing so that @CreatedDate and @LastModifiedDate
 * annotations on documents are automatically populated.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
