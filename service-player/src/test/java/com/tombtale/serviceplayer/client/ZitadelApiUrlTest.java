package com.tombtale.serviceplayer.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the rule about where the provisioner token may be sent.
 *
 * <p>The mistake this guards against is a working one: plain http to a remote
 * Zitadel provisions players correctly and puts the token on the wire in clear
 * every time, so nothing about the behaviour would give it away.
 */
class ZitadelApiUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:8080",
        "http://LOCALHOST:8080",
        "http://127.0.0.1:8080",
        "http://[::1]:8080"
    })
    @DisplayName("http is fine on loopback, which is how the local stack runs")
    void allowsHttpOnLoopback(String baseUrl) {
        assertThat(ZitadelApiUrl.requireSecure(baseUrl)).isEqualTo(baseUrl);
    }

    @Test
    @DisplayName("https is fine anywhere")
    void allowsHttpsAnywhere() {
        assertThat(ZitadelApiUrl.requireSecure("https://auth.tombtale.example")).isEqualTo("https://auth.tombtale.example");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://auth.tombtale.example",
        "http://10.0.0.4:8080",
        "http://localhost.tombtale.example"
    })
    @DisplayName("plain http to anywhere else would put the token on the wire")
    void refusesHttpOffLoopback(String baseUrl) {
        assertThatThrownBy(() -> ZitadelApiUrl.requireSecure(baseUrl))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base-url");
    }

    /**
     * Blank is the unconfigured environment, including the test profile. It
     * cannot leak anything, and the failure it does cause belongs to the call.
     */
    @Test
    @DisplayName("blank is left alone so an unconfigured service still starts")
    void allowsBlank() {
        assertThat(ZitadelApiUrl.requireSecure("")).isEmpty();
        assertThat(ZitadelApiUrl.requireSecure(null)).isEmpty();
    }

    @Test
    @DisplayName("a scheme we do not know is refused rather than assumed safe")
    void refusesUnknownScheme() {
        assertThatThrownBy(() -> ZitadelApiUrl.requireSecure("localhost:8080/management"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("something that is not a URL fails where it is configured, not at the first call")
    void refusesGarbage() {
        assertThatThrownBy(() -> ZitadelApiUrl.requireSecure("http://host name"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a URL");
    }
}
