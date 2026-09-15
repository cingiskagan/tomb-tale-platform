package com.tombtale.commons.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static org.assertj.core.api.Assertions.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tests the error contract both services inherit.
 *
 * <p>
 * Commons has no controllers, so the probe classes below stand in. They are
 * scaffolding, not the subject.
 */

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.UnitTestShouldIncludeAssert" })
class PlatformExceptionHandlerTest {

    private static final String SORT_URL = "/probe/sort";
    private static final String LOCK_URL = "/probe/lock";
    private static final String VALIDATE_URL = "/probe/validate";

    /** Named in the exception message, and must not reach the client. */
    private static final String LOCKED_ENTITY = "Purchase";
    private static final long LOCKED_ID = 987_654_321L;

    private static final String INVALID_SORT_FIELD = "Invalid Sort Field";
    private static final String REJECTED_FIELD = "nonsense";

    private static final String INVALID_POST_BODY = """
            {
            "name": "robin12"
            }
            """;

    private static final String INVALID_POST_BODY_2 = """
            {
            "name": "0z"
            }
            """;

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new ProbeAdvice())
            .build();

    /**
     * 400, with the exception message as {@code detail}.
     *
     * <p>
     * The title is the same for every rejected field, so only the detail
     * says which one. Which fields get rejected is tested in the repositories.
     */
    @Test
    void invalidSortFieldBecomesBadRequest() throws Exception {
        mockMvc.perform(get(SORT_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(INVALID_SORT_FIELD))
                .andExpect(jsonPath("$.detail").value("Invalid sort field: " + REJECTED_FIELD))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * 409 with a fixed retry message.
     *
     * <p>
     * The real assertion is the negative one: the entity name and id from
     * the exception must not reach the client.
     */
    @Test
    void optimisticLockBecomesConflictWithoutLeakingTheEntity() throws Exception {
        mockMvc.perform(get(LOCK_URL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("modified by another transaction")))
                .andExpect(content().string(not(containsString(LOCKED_ENTITY))))
                .andExpect(content().string(not(equalTo(LOCKED_ID))));
    }

    /**
     * Field messages land in the {@code errors} member.
     *
     * <p>
     * Assert {@code $.errors.name}, not {@code $.detail} — that one is
     * Spring's own constant. {@code "ab"} fails one rule.
     */
    @Test
    void validationFailurePutsFieldMessagesInTheErrorsMember() throws Exception {
        mockMvc.perform(post(VALIDATE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_POST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").value("must be lowercase letters"));
    }

    /**
     * Two failures on one field merge instead of throwing.
     *
     * <p>
     * {@code Collectors.toMap} dies on a duplicate key without a merge
     * function — a 500 from inside the error handler. {@code "A"} fails both
     * rules; the order is not fixed, so do not assert on it.
     */
    @Test
    void twoFailuresOnOneFieldAreMergedIntoOneMessage() throws Exception {
        mockMvc.perform(post(VALIDATE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_POST_BODY_2))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(allOf(
                        containsString("must be lowercase letters"),
                        containsString("must be at least 3 characters"),
                        containsString("; "))));
    }

    /** Raises one exception per handler under test. */
    @RestController
    static class ProbeController {

        @GetMapping(SORT_URL)
        void sort() {
            throw new InvalidSortFieldException(REJECTED_FIELD);
        }

        /** Message carries the entity name and id, as Spring Data's really does. */
        @GetMapping(LOCK_URL)
        void lock() {
            throw new ObjectOptimisticLockingFailureException(LOCKED_ENTITY, LOCKED_ID);
        }

        @PostMapping(VALIDATE_URL)
        void validate(@Valid @RequestBody ProbeRequest request) {
            // Never reached: validation fails before the body is invoked.
        }
    }

    /**
     * @param name {@code "robin12"} fails the pattern rule only; {@code "0z"} fails both
     */
    record ProbeRequest(
            @Size(min = 3, message = "must be at least 3 characters")
            @Pattern(regexp = "[a-z]*", message = "must be lowercase letters")
            String name) {
    }

    /** Empty, exactly like service-player's. */
    @RestControllerAdvice
    static class ProbeAdvice extends PlatformExceptionHandler {
    }
}
