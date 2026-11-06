package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.IconFormat;
import com.tombtale.serviceinventory.dto.ItemIconResponse;
import com.tombtale.serviceinventory.entity.ItemIcon;
import com.tombtale.serviceinventory.exception.IconNotFoundException;
import com.tombtale.serviceinventory.exception.IconTooLargeException;
import com.tombtale.serviceinventory.exception.UnsupportedIconException;
import com.tombtale.serviceinventory.repository.ItemIconRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Stores uploaded icons once each, keyed by the SHA-256 of their bytes, and serves them.
 */
@Service
@Transactional(readOnly = true)
public class ItemIconService {

    private final ItemIconRepository itemIconRepository;
    private final long maxBytes;

    /** The limit comes from {@code inventory.icons.max-size} in application.yml. */
    public ItemIconService(
            ItemIconRepository itemIconRepository, @Value("${inventory.icons.max-size}") DataSize maxSize) {
        this.itemIconRepository = itemIconRepository;
        this.maxBytes = maxSize.toBytes();
    }

    /**
     * Saves an icon, or finds the one an earlier upload of the same bytes saved.
     *
     * @param bytes the uploaded file
     * @return the icon's hash and type
     * @throws IconTooLargeException    if the file is over the limit
     * @throws UnsupportedIconException if the file is neither PNG nor WebP by its first bytes
     */
    @Transactional
    public ItemIconResponse upload(byte[] bytes) {
        if (bytes.length > maxBytes) {
            throw new IconTooLargeException(maxBytes);
        }
        IconFormat format = IconFormat.detect(bytes).orElseThrow(UnsupportedIconException::new);
        String hash = sha256(bytes);
        ItemIcon icon = itemIconRepository.findByHash(hash).orElseGet(() -> itemIconRepository.save(
                ItemIcon.builder().hash(hash).contentType(format.getContentType()).data(bytes).build()));
        return new ItemIconResponse(icon.getHash(), icon.getContentType());
    }

    /**
     * @param hash the icon's hash
     * @return the stored icon
     * @throws IconNotFoundException if no upload had the hash
     */
    public ItemIcon find(String hash) {
        return itemIconRepository.findByHash(hash).orElseThrow(() -> new IconNotFoundException(hash));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JVM has SHA-256", e);
        }
    }
}
