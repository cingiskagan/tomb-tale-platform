package com.tombtale.servicecommerce.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke-test controller for verifying application routing.
 *
 * <p>Despite the name, {@code /api/v1/test/**} sits under the authenticated
 * {@code /api/**} matcher, so these endpoints require a valid Bearer token —
 * use the actuator health endpoint for unauthenticated liveness checks.
 */
@RestController
@RequestMapping("/api/v1/test")
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Not a test class — named "Test" for smoke-test endpoints
public class TestController {

    /**
     * Returns a basic greeting string, indicating the service is reachable
     * and the request survived authentication.
     *
     * @return A static string greeting.
     */
    @GetMapping("/hello")
    public String returnHelloWorld() {
        return "Hello world.";
    }
}
