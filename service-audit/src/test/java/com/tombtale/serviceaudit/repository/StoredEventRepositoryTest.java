package com.tombtale.serviceaudit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.tombtale.serviceaudit.document.StoredEvent;
import com.tombtale.serviceaudit.support.MongoTestBase;

/**
 * The rights init-mongo.sh gives its two users. An event goes in once as service_audit, nothing changes it
 * afterwards, and audit_reader can only read it.
 */
@DataMongoTest
@ActiveProfiles("test")
class StoredEventRepositoryTest extends MongoTestBase {

    private final UUID eventId = UUID.randomUUID();

    @Autowired
    private StoredEventRepository storedEventRepository;

    @AfterEach
    void removeTheEvent() {
        deleteStoredEvent(eventId);
    }

    @Test
    void anEventGoesInUnderItsEventId() {
        storedEventRepository.insert(anEvent());

        assertThat(storedEvent(eventId))
                .containsEntry("eventType", "player.created")
                .extractingByKey("data", MAP)
                .containsEntry("playerPublicId", "p-1");
    }

    @Test
    void theSameEventIdCannotGoInTwice() {
        storedEventRepository.insert(anEvent());

        assertThatThrownBy(() -> storedEventRepository.insert(anEvent()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void aStoredEventCannotBeChangedOrDeletedOrRead() {
        storedEventRepository.insert(anEvent());

        assertUnauthorized(() -> storedEventRepository.save(anEvent()));
        assertUnauthorized(() -> storedEventRepository.deleteById(eventId));
        assertUnauthorized(() -> storedEventRepository.findById(eventId));
        assertThat(storedEvent(eventId)).isNotNull();
    }

    @Test
    @SuppressWarnings("PMD.LooseCoupling") // Document is the driver's document type, not a choice of collection class
    void theReaderCanReadAnEventButNotChangeIt() {
        storedEventRepository.insert(anEvent());

        try (MongoClient reader = connectAsReader()) {
            MongoCollection<Document> events = reader.getDatabase("audit").getCollection("events");

            assertThat(events.find(Filters.eq("_id", eventId)).first()).isNotNull();
            assertUnauthorized(() -> events.insertOne(new Document("_id", UUID.randomUUID())));
            assertUnauthorized(() -> events.deleteOne(Filters.eq("_id", eventId)));
        }
        assertThat(storedEvent(eventId)).isNotNull();
    }

    /** Spring and the driver both name MongoDB's error 13 in the message, so the code name is the evidence. */
    private static void assertUnauthorized(ThrowingCallable operation) {
        assertThatThrownBy(operation).hasMessageContaining("(Unauthorized)");
    }

    private StoredEvent anEvent() {
        return new StoredEvent(eventId, "player.created", 1, Instant.parse("2026-10-16T10:15:30Z"),
                "service-player", Map.of("playerPublicId", "p-1"));
    }
}
