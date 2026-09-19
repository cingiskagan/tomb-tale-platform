package com.tombtale.serviceplayer.config;

import com.tombtale.serviceplayer.client.ZitadelApiUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wiring for the outbound Zitadel client.
 *
 * <p>Both timeouts are set on purpose. This call sits inside the provisioning
 * request Zitadel itself is waiting on, so a Zitadel that has stopped answering
 * must not hold a thread here until something else gives up first.
 *
 * <p>The base URL is checked before the token is attached, so a URL that would
 * leak it stops start-up instead of working quietly. See {@link ZitadelApiUrl}.
 */
@Configuration
public class ZitadelClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Value("${app.zitadel.api.base-url:${ZITADEL_ISSUER_URI:}}")
    private String baseUrl;

    @Value("${app.zitadel.api.token:}")
    private String serviceToken;

    /**
     * The Zitadel-bound client, carrying the service account's token.
     *
     * @return a client scoped to the Zitadel API
     */
    @Bean
    public RestClient zitadelRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(ZitadelApiUrl.requireSecure(baseUrl))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .build();
    }
}
