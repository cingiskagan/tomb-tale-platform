package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One uploaded image, stored once under the SHA-256 of its bytes. Nothing changes it after the upload.
 */
@Entity
@Table(name = "item_icons")
@Getter
@NoArgsConstructor
@SuperBuilder
public class ItemIcon extends BaseEntity {

    private static final int HASH_LENGTH = 64;
    private static final int CONTENT_TYPE_MAX_LENGTH = 20;

    /** The SHA-256 of {@link #data}, in lower-case hex. Its URL carries it. */
    @Column(name = "hash", nullable = false, unique = true, updatable = false, length = HASH_LENGTH)
    private String hash;

    /** {@code image/png} or {@code image/webp}, read from the file's first bytes. */
    @Column(name = "content_type", nullable = false, updatable = false, length = CONTENT_TYPE_MAX_LENGTH)
    private String contentType;

    /** The image itself. */
    @Column(name = "data", nullable = false, updatable = false)
    private byte[] data;
}
