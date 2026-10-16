package com.tombtale.serviceaudit.support;

import java.util.Map;
import java.util.UUID;

import org.bson.UuidRepresentation;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;

/**
 * One MongoDB container for every test, set up by the stack's own init-mongo.sh. The service connects as
 * service_audit, and a test reads {@code events} back as root, because service_audit cannot read.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class MongoTestBase {

    private static final int MONGO_PORT = 27_017;
    private static final String ROOT_PASSWORD = "root-pass";
    private static final String AUDIT_USER = "service_audit";
    private static final String AUDIT_PASSWORD = "audit-pass";
    private static final String READER_USER = "audit_reader";
    private static final String READER_PASSWORD = "reader-pass";
    private static final String DATABASE = "audit";
    private static final String EVENTS = "events";

    /** The init script runs on a first server, which stops, so the second "Waiting for connections" is the ready one. */
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:8.0")
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", ROOT_PASSWORD)
            .withEnv("MONGO_AUDIT_USER", AUDIT_USER)
            .withEnv("MONGO_AUDIT_PASSWORD", AUDIT_PASSWORD)
            .withEnv("MONGO_AUDIT_READER_USER", READER_USER)
            .withEnv("MONGO_AUDIT_READER_PASSWORD", READER_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath("../infrastructure/init-mongo.sh"),
                    "/docker-entrypoint-initdb.d/init-mongo.sh")
            .withExposedPorts(MONGO_PORT)
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 2));

    private static final MongoClient ROOT;

    static {
        MONGO.start();
        ROOT = connect("root:" + ROOT_PASSWORD, "admin");
    }

    private static MongoClient connect(String credentials, String authenticationDatabase) {
        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://" + credentials + "@" + MONGO.getHost() + ":"
                        + MONGO.getMappedPort(MONGO_PORT) + "/?authSource=" + authenticationDatabase))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build());
    }

    /**
     * Signs in as audit_reader, the read-only user for people. The caller closes the client.
     *
     * @return a client signed in as audit_reader
     */
    protected static MongoClient connectAsReader() {
        return connect(READER_USER + ":" + READER_PASSWORD, DATABASE);
    }

    @DynamicPropertySource
    static void connectAsServiceAudit(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.host", MONGO::getHost);
        registry.add("spring.mongodb.port", () -> MONGO.getMappedPort(MONGO_PORT));
        registry.add("spring.mongodb.username", () -> AUDIT_USER);
        registry.add("spring.mongodb.password", () -> AUDIT_PASSWORD);
    }

    /**
     * Reads one stored event as root.
     *
     * @param eventId the event's id, which is its {@code _id}
     * @return the document, or null if none has that id
     */
    protected static Map<String, Object> storedEvent(UUID eventId) {
        return ROOT.getDatabase(DATABASE).getCollection(EVENTS).find(Filters.eq("_id", eventId)).first();
    }

    /**
     * Counts every stored event as root. Each test deletes its own, so this is what the running test stored.
     *
     * @return the number of documents in {@code events}
     */
    protected static long storedEventCount() {
        return ROOT.getDatabase(DATABASE).getCollection(EVENTS).countDocuments();
    }

    /**
     * Deletes one stored event as root, because service_audit cannot.
     *
     * @param eventId the event's id, which is its {@code _id}
     */
    protected static void deleteStoredEvent(UUID eventId) {
        ROOT.getDatabase(DATABASE).getCollection(EVENTS).deleteOne(Filters.eq("_id", eventId));
    }
}
