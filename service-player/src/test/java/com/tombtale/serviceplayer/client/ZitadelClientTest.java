package com.tombtale.serviceplayer.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for the one call this service makes out to Zitadel.
 *
 * <p>A player who registers and does not get this role cannot obtain a token,
 * so the interesting cases are the two failure shapes: a conflict, which means
 * the job is already done, and everything else, which must not be swallowed.
 */
@SuppressWarnings("PMD.TooManyStaticImports")
class ZitadelClientTest {

    private static final String PROJECT_ID = "391333321403072515";
    private static final String USER_ID = "336494809936035843";
    private static final String GRANTS_URL = "http://zitadel.test/management/v1/users/" + USER_ID + "/grants";

    private MockRestServiceServer server;
    private ZitadelClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://zitadel.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ZitadelClient(builder.build(), PROJECT_ID);
    }

    @Test
    @DisplayName("grants the player role on the configured project")
    void grantsThePlayerRole() {
        server.expect(requestTo(GRANTS_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.roleKeys[0]").value("player"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.grantPlayerRole(USER_ID);

        server.verify();
    }

    @Test
    @DisplayName("a conflict means the role is already there, which is the outcome we wanted")
    void treatsConflictAsSuccess() {
        server.expect(requestTo(GRANTS_URL))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatCode(() -> client.grantPlayerRole(USER_ID)).doesNotThrowAnyException();

        server.verify();
    }

    /**
     * Anything else has to propagate. Zitadel retries a provisioning call that
     * fails, and swallowing this would leave a user with a profile, no role, and
     * no way in — while the registration looked like it worked.
     */
    @Test
    @DisplayName("any other failure propagates so the provisioning call is retried")
    void letsOtherFailuresThrough() {
        server.expect(requestTo(GRANTS_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.grantPlayerRole(USER_ID))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("an unconfigured project id fails loudly instead of silently not granting")
    void refusesToRunUnconfigured() {
        ZitadelClient unconfigured = new ZitadelClient(RestClient.builder().build(), "");

        assertThatThrownBy(() -> unconfigured.grantPlayerRole(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("project-id"));
    }
}
