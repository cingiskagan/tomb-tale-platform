package com.tombtale.serviceplayer.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether the configured Zitadel URL is one a token may be sent to.
 *
 * <p>Every call this service makes to Zitadel carries the provisioner token, and
 * plain HTTP puts it on the wire in clear. So http is accepted on loopback only,
 * which is how the local stack serves Zitadel. Anywhere else has to be https.
 */
public final class ZitadelApiUrl {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    /**
     * {@code getHost} keeps the brackets on an IPv6 literal, so both spellings are here.
     *
     * <p>AvoidUsingHardCodedIP is suppressed because these addresses are the rule
     * itself, not a server we happened to write down. Resolving the host instead
     * would mean a DNS lookup while the context starts.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private ZitadelApiUrl() {
    }

    /**
     * Checks a base URL before the token is attached to it.
     *
     * @param baseUrl the configured URL; blank means the client is not configured at all
     * @return the same URL, once it is safe to send a token to
     * @throws IllegalStateException if the token would travel unencrypted
     */
    public static String requireSecure(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }

        URI uri = parse(baseUrl);
        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());

        if (HTTPS.equals(scheme) || HTTP.equals(scheme) && LOOPBACK_HOSTS.contains(host)) {
            return baseUrl;
        }

        throw new IllegalStateException(
                "app.zitadel.api.base-url is " + baseUrl + ", which the provisioner token cannot be sent to. "
                        + "Every call to Zitadel carries that token, so plain http is allowed on loopback only. "
                        + "Use https for any other host.");
    }

    private static URI parse(String baseUrl) {
        try {
            return new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("app.zitadel.api.base-url is not a URL: " + baseUrl, e);
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
