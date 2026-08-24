package com.tombtale.serviceplayer.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tombtale.serviceplayer.config.SecurityConfig;
import com.tombtale.serviceplayer.config.ZitadelRoleConverter;
import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.UpdateCharacterStatsRequest;
import com.tombtale.serviceplayer.service.CharacterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@WebMvcTest(CharacterController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@SuppressWarnings({
        "PMD.TooManyStaticImports",
        "PMD.UseConcurrentHashMap" })
class CharacterControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterService characterService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String ROLE_PLAYER = "player";
    private static final String ROLE_GAME_MASTER = "game_master";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String SUBJECT = "zitadel-sub-314159";

    private static final UUID PLAYER_PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHARACTER_PUBLIC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String STATS_URL = "/api/v1/players/" + PLAYER_PUBLIC_ID
            + "/characters/" + CHARACTER_PUBLIC_ID + "/stats";

    private static final int STATS_LEVEL = 2;
    private static final long STATS_XP = 100L;

    private static final String VALID_STATS_BODY = """
            {
              "level": 2,
              "experiencePoints": 100
            }
            """;

    /** level 0 violates @Min(1) on UpdateCharacterStatsRequest → 400. */
    private static final String INVALID_STATS_BODY = """
            {
              "level": 0,
              "experiencePoints": 100
            }
            """;

    private static CharacterResponse aCharacterResponse() {
        return new CharacterResponse(
                CHARACTER_PUBLIC_ID,
                "Aiden the Brave",
                STATS_LEVEL,
                STATS_XP,
                Instant.now());
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        Map<String, Object> rolesClaim = new LinkedHashMap<>();
        for (String role : roles) {
            rolesClaim.put(role, Map.of());
        }
        return jwt()
                .jwt(token -> token.subject(SUBJECT).claim(ZITADEL_ROLES_CLAIM, rolesClaim))
                .authorities(new ZitadelRoleConverter());
    }

    @Test
    void updateStatsAsAnonymousReturns401() throws Exception {
        mockMvc.perform(patch(STATS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_STATS_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(characterService);
    }

    @Test
    void updateStatsAsPlayerReturns403() throws Exception {
        mockMvc.perform(patch(STATS_URL)
                .with(tokenWithRoles(ROLE_PLAYER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_STATS_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(characterService);
    }

    @Test
    void updateStatsAsGameMasterReturns200() throws Exception {
        when(characterService.updateCharacterStats(eq(PLAYER_PUBLIC_ID), eq(CHARACTER_PUBLIC_ID), any()))
                .thenReturn(aCharacterResponse());

        mockMvc.perform(patch(STATS_URL)
                .with(tokenWithRoles(ROLE_GAME_MASTER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_STATS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(STATS_LEVEL));

        verify(characterService).updateCharacterStats(eq(PLAYER_PUBLIC_ID), eq(CHARACTER_PUBLIC_ID), any());
    }

    @Test
    void updateStatsAsAdminReturns200() throws Exception {
        when(characterService.updateCharacterStats(eq(PLAYER_PUBLIC_ID), eq(CHARACTER_PUBLIC_ID), any()))
                .thenReturn(aCharacterResponse());

        mockMvc.perform(patch(STATS_URL)
                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_STATS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(STATS_LEVEL));

        ArgumentCaptor<UpdateCharacterStatsRequest> captor = ArgumentCaptor.forClass(UpdateCharacterStatsRequest.class);
        verify(characterService).updateCharacterStats(eq(PLAYER_PUBLIC_ID), eq(CHARACTER_PUBLIC_ID), captor.capture());
        assertThat(captor.getValue().getExperiencePoints()).isEqualTo(STATS_XP);
        assertThat(captor.getValue().getLevel()).isEqualTo(STATS_LEVEL);
    }

    @Test
    void updateStatsAsAdminWithLevelZeroReturns400() throws Exception {
        mockMvc.perform(patch(STATS_URL)
                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_STATS_BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(characterService);
    }
}
