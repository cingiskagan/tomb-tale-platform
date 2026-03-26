package com.tombtale.servicecommerce.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller intended for testing base connectivity and future authentication integration.
 * Provides unauthenticated or minimal-auth endpoints to verify application health and routing.
 */
@RestController
@RequestMapping("/api/v1/test")
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Not a test class — named "Test" for smoke-test endpoints
public class TestController {

    /**
     * Returns a basic greeting string.
     * Provides a simple response indicating the service is reachable, serving as a baseline
     * for future connection and validation tests.
     *
     * @return A static string greeting.
     */
    @GetMapping("/hello")
    public String returnHelloWorld() {
        return "Hello world.";
    }
}
