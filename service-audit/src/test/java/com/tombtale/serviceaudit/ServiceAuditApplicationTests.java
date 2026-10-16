package com.tombtale.serviceaudit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.mongodb.health.MongoHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tombtale.serviceaudit.support.MongoTestBase;

/**
 * The real context on a MongoDB set up by init-mongo.sh. The health check signs in as service_audit, so it
 * proves that the insert-only user can still report the database as up.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceAuditApplicationTests extends MongoTestBase {

    @Autowired
    private MongoHealthIndicator mongoHealthIndicator;

    @Test
    void mongoIsUpForTheInsertOnlyUser() {
        assertThat(mongoHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
