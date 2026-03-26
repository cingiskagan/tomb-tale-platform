package com.tombtale.servicecommerce.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that demonstrates Zitadel JWT integration.
 * All endpoints under {@code /api/v1/secure} require a valid Bearer token
 * issued by the configured Zitadel instance.
 */
@RestController
@RequestMapping("/api/v1/secure")
public class SecureTestController {

    /**
     * Returns a personalised greeting using the authenticated user's subject claim.
     * Proves the full Zitadel ↔ Spring Security OAuth2 Resource Server integration
     * by extracting the {@code sub} claim from the validated JWT.
     *
     * @param jwt The validated JWT token injected by Spring Security.
     * @return A greeting that includes the token's subject identifier.
     */
    @GetMapping("/hello")
    public String returnSecureHello(@AuthenticationPrincipal Jwt jwt) {
        String subject = jwt.getSubject();
        return "Hello, " + subject + "!";
    }
}
