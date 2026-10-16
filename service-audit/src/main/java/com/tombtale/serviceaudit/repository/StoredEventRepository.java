package com.tombtale.serviceaudit.repository;

import com.tombtale.serviceaudit.document.StoredEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

/**
 * Access to {@code events}. Only {@code insert} works: the database refuses this service everything else, reads too.
 */
public interface StoredEventRepository extends MongoRepository<StoredEvent, UUID> {
}
