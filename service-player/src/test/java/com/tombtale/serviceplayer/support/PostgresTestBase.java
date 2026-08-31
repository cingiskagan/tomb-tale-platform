package com.tombtale.serviceplayer.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Postgres container for every database-backed test in this service.
 *
 * <p>One container for the whole suite: the static initializer starts it, and
 * nothing stops it — Testcontainers' Ryuk sidecar removes it when the JVM
 * exits. Extending this class from a test is what wires {@code @ServiceConnection}
 * into that test's context.
 */
@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod", "PMD.AbstractClassWithoutAbstractMethod"})
public abstract class PostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.2-alpine");

    static {
        POSTGRES.start();
    }
}
