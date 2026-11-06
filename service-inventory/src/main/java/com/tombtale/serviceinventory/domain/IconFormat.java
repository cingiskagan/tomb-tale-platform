package com.tombtale.serviceinventory.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The image formats an icon may have, told apart by the file's first bytes and never by its name.
 */
public enum IconFormat {

    PNG("image/png") {
        @Override
        boolean matches(byte[] bytes) {
            return startsWith(bytes, 0, PNG_SIGNATURE);
        }
    },

    /** A RIFF container whose form type, at byte 8, is {@code WEBP}. */
    WEBP("image/webp") {
        @Override
        boolean matches(byte[] bytes) {
            return startsWith(bytes, 0, RIFF) && startsWith(bytes, WEBP_OFFSET, WEBP_FORM);
        }
    };

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_FORM = {'W', 'E', 'B', 'P'};
    private static final int WEBP_OFFSET = 8;

    /** The {@code Content-Type} the icon is served with. */
    @Getter
    private final String contentType;

    IconFormat(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @param bytes the uploaded file
     * @return its format, or empty when it is neither PNG nor WebP
     */
    public static Optional<IconFormat> detect(byte[] bytes) {
        return Arrays.stream(values()).filter(format -> format.matches(bytes)).findFirst();
    }

    abstract boolean matches(byte[] bytes);

    private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        return bytes.length >= offset + prefix.length
                && Arrays.equals(bytes, offset, offset + prefix.length, prefix, 0, prefix.length);
    }
}
