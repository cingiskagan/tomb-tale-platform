package com.tombtale.servicecommerce.exception;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Commerce's own two handlers. The shared ones are tested in commons.
 *
 * <p>{@link ProbeController} stands in for the real controller so the test does
 * not drag the security filter chain in with it.
 */
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.UnitTestShouldIncludeAssert" })
class GlobalExceptionHandlerTest {

    private static final String NOT_FOUND_URL = "/probe/not-found";
    private static final String BAD_TRANSITION_URL = "/probe/bad-transition";

    private static final String NOT_FOUND = "Not Found";
    private static final String BAD_TRANSITION = "Invalid Status Transition";

    private static final UUID MISSING_PURCHASE =
            UUID.fromString("3f2a1c4e-0000-4000-8000-000000000001");
    private static final String TRANSITION_MESSAGE =
            "Cannot change purchase status from CANCELLED to PENDING";

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    /**
     * 404, with the missing id in {@code detail} so a caller can tell which
     * lookup failed.
     */
    @Test
    void purchaseNotFoundBecomesNotFound() throws Exception {
        
        mockMvc.perform(get(NOT_FOUND_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(containsString(MISSING_PURCHASE.toString())))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * 400, not 409. A rejected transition is a bad request, not a conflict —
     * retrying it unchanged would fail the same way.
     */
    @Test
    void invalidStatusTransitionBecomesBadRequest() throws Exception {
        
        mockMvc.perform(get(BAD_TRANSITION_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(BAD_TRANSITION))
                .andExpect(jsonPath("$.detail").value(TRANSITION_MESSAGE))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /** Raises one exception per handler under test. */
    @RestController
    static class ProbeController {

        @GetMapping(NOT_FOUND_URL)
        void notFound() {
            throw new PurchaseNotFoundException(MISSING_PURCHASE);
        }

        @GetMapping(BAD_TRANSITION_URL)
        void badTransition() {
            throw new InvalidStatusTransitionException(TRANSITION_MESSAGE);
        }
    }
}
