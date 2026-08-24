package com.tombtale.serviceplayer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tombtale.serviceplayer.config.SecurityConfig;
import com.tombtale.serviceplayer.config.ZitadelRoleConverter;
import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.service.PlayerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@SuppressWarnings({
        "PMD.TooManyStaticImports",
        "PMD.TooManyMethods",
        "PMD.UnitTestShouldIncludeAssert",
        "PMD.UseConcurrentHashMap" })
class PlayerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private PlayerMapper playerMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String PLAYERS_URL = "/api/v1/players";
    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String ROLE_PLAYER = "player";
    private static final String ROLE_GAME_MASTER = "game_master";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String SUBJECT = "zitadel-sub-314159";

    private static final String ME_URL = PLAYERS_URL + "/me";

    private static final UUID PLAYER_PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHARACTER_PUBLIC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "TombRaider";
    private static final int CHARACTER_LEVEL = 1;
    private static final long CHARACTER_XP = 0L;

    private static final String VALID_PROFILE_BODY = """
            {
              "displayName": "TombRaider"
            }
            """;

    /**
     * Blank name violates @NotBlank (and @Size min 3) on UpdateMyProfileRequest →
     * 400.
     */
    private static final String INVALID_PROFILE_BODY = """
            {
              "displayName": ""
            }
            """;

    /** Content irrelevant: PlayerMapper is mocked, so this entity is never read. */
    private static Player aPlayer() {
        return Player.builder().build();
    }

    private static PlayerResponse aPlayerResponse() {
        return new PlayerResponse(
                PLAYER_PUBLIC_ID,
                DISPLAY_NAME,
                "icon-skull",
                List.of(aCharacterResponse()),
                Instant.now());
    }

    private static CharacterResponse aCharacterResponse() {
        return new CharacterResponse(
                CHARACTER_PUBLIC_ID,
                "Aiden the Brave",
                CHARACTER_LEVEL,
                CHARACTER_XP,
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
    void listAsAnonymousReturns401() throws Exception {
        mockMvc.perform(get(PLAYERS_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    @Test
    void listAsPlayerReturns403() throws Exception {
        mockMvc.perform(get(PLAYERS_URL)
                .with(tokenWithRoles(ROLE_PLAYER)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(playerService);
    }

    @Test
    void listAsGameMasterReturns200() throws Exception {
        when(playerService.listPlayers(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get(PLAYERS_URL)
                .with(tokenWithRoles(ROLE_GAME_MASTER)))
                .andExpect(status().isOk());
    }

    @Test
    void listAsAdminReturns200() throws Exception {
        when(playerService.listPlayers(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get(PLAYERS_URL)
                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void meAsAnonymousReturns401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    @Test
    void meAsPlayerReturns200() throws Exception {
        when(playerService.getOrCreatePlayer(any())).thenReturn(aPlayer());
        when(playerMapper.toResponse(any(Player.class))).thenReturn(aPlayerResponse());

        mockMvc.perform(get(ME_URL)
                .with(tokenWithRoles(ROLE_PLAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(DISPLAY_NAME));
    }

    @Test
    void updateMeAsAnonymousReturns401() throws Exception {
        mockMvc.perform(patch(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    @Test
    void updateMeAsPlayerReturns200() throws Exception {
        when(playerService.updateMyProfile(any(), any()))
                .thenReturn(aPlayerResponse());

        mockMvc.perform(patch(ME_URL)
                .with(tokenWithRoles(ROLE_PLAYER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(DISPLAY_NAME));
    }

    @Test
    void updateMeAsPlayerWithBlankNameReturns400() throws Exception {
        mockMvc.perform(patch(ME_URL)
                .with(tokenWithRoles(ROLE_PLAYER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_PROFILE_BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(playerService);
    }
}
