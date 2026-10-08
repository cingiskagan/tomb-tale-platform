package com.tombtale.serviceplayer.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.web.InvalidSortFieldException;
import com.tombtale.serviceplayer.config.SecurityConfig;
import com.tombtale.serviceplayer.controller.PlayerController;
import com.tombtale.serviceplayer.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Proves player's advice is registered and doing its job.
 *
 * <p>The class itself is empty — it inherits everything. So this is a
 * {@code @WebMvcTest}, not a standalone setup: the advice has to be found by
 * component scan, the way it is at runtime. Delete
 * {@code GlobalExceptionHandler} and these two fail while the rest of the
 * suite stays green.
 */
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.UnitTestShouldIncludeAssert" })
@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    private static final String PLAYERS_URL = "/api/v1/players";
    private static final String ME_URL = PLAYERS_URL + "/me";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String SUBJECT = "6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f3b64";

    private static final String VALID_PROFILE_BODY = "{\"displayName\":\"Legolas\"}";
    private static final String NOT_FOUND_REASON = "Player not found";
    private static final String INVALID_SORT_MESSAGE = "Invalid Sort Field";
    private static final String INVALID_SORT_FIELD = "nonsense";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(SUBJECT)
                        .claim(PublicIdClaim.CLAIM, SUBJECT)
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }

    /**
     * The service layer throws {@code ResponseStatusException}, and the reason
     * reaches the client.
     *
     * <p>This is the regression the advice exists for. Without it Spring's
     * default error handling answers, and the body arrives empty.
     */
    @Test
    void responseStatusExceptionKeepsItsReason() throws Exception {

        when(playerService.updateMyProfile(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_REASON));

        mockMvc.perform(patch(ME_URL)
                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(NOT_FOUND_REASON));
    }

    /**
     * A handler player never declares still answers, because the base class in
     * {@code platform-commons} does.
     */
    @Test
    void inheritedSortHandlerIsLiveInThisService() throws Exception {
        
        when(playerService.listPlayers(any(), any()))
                .thenThrow(new InvalidSortFieldException(INVALID_SORT_FIELD));

        mockMvc.perform(get(PLAYERS_URL + "?sort=" + INVALID_SORT_FIELD)
                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value(INVALID_SORT_MESSAGE));
    }
}
