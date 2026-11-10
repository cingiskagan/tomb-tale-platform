package com.tombtale.serviceinventory.dto;

/**
 * An uploaded icon. A template names it by its hash, and {@code GET /api/v1/inventory/icons/{hash}} serves it.
 *
 * @param hash        the SHA-256 of the image, in lower-case hex
 * @param contentType {@code image/png} or {@code image/webp}
 */
public record ItemIconResponse(String hash, String contentType) {
}
