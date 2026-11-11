package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.ItemIconResponse;
import com.tombtale.serviceinventory.entity.ItemIcon;
import com.tombtale.serviceinventory.service.ItemIconService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * Icon uploads for {@code platform_admin}, and icon reads for anyone. An icon never changes under its hash.
 */
@RestController
@RequestMapping(ItemIconController.PATH)
@RequiredArgsConstructor
public class ItemIconController {

    /** An icon's URL is this path, a slash and its hash. */
    public static final String PATH = "/api/v1/inventory/icons";

    private static final Duration CACHE_TIME = Duration.ofDays(365);

    private final ItemIconService itemIconService;

    /**
     * The type is judged by the file's bytes, and the same image twice is stored once.
     *
     * @param file a PNG or WebP image
     * @return the icon's hash, which a template then names
     * @throws IOException if the upload can't be read
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(RoleConstants.IS_ADMIN)
    public ItemIconResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return itemIconService.upload(file.getBytes());
    }

    /**
     * Needs no token, so the game and an {@code img} tag can load it.
     *
     * @param hash the icon's hash
     * @return the image, cached for a year
     */
    @GetMapping("/{hash}")
    public ResponseEntity<byte[]> find(@PathVariable String hash) {
        ItemIcon icon = itemIconService.find(hash);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(icon.getContentType()))
                .cacheControl(CacheControl.maxAge(CACHE_TIME).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(icon.getData());
    }

    /**
     * @param hash a template's icon hash, or null
     * @return the icon's URL, or null for a generic icon
     */
    public static String urlOf(String hash) {
        return hash == null ? null : PATH + "/" + hash;
    }
}
