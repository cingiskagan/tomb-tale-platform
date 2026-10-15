package com.tombtale.serviceinventory.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One Postgres container for every database test in this service. Nothing stops it: Ryuk removes it when the
 * JVM exits. Extending this class wires {@code @ServiceConnection} into the test's context.
 */
@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod", "PMD.AbstractClassWithoutAbstractMethod"})
public abstract class PostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.2-alpine");

    static {
        POSTGRES.start();
    }
}
