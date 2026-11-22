package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.BonusPool;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The roll ranges the portal shows beside a custom bonus.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see.
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class BonusRangeControllerTest extends FixedAuthorTestBase {

    private static final String RANGES_URL = "/api/v1/inventory/bonus-ranges";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");

    @Autowired
    private MockMvc mockMvc;

    /** A weapon rolls damage and no HP, a helmet HP, and a ring rolls both, each in its pool's range. */
    @Test
    void eachItemTypeAnswersWithItsPoolsRanges() throws Exception {
        BonusPool.Range fireDamage = BonusPool.WEAPON.ranges().get(Stat.FIRE_DAMAGE);
        BonusPool.Range maxHp = BonusPool.ARMOR_PIECE.ranges().get(Stat.MAX_HP);

        mockMvc.perform(get(RANGES_URL).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ONE_HANDED_WEAPON.FIRE_DAMAGE.min").value(fireDamage.min()))
                .andExpect(jsonPath("$.ONE_HANDED_WEAPON.FIRE_DAMAGE.max").value(fireDamage.max()))
                .andExpect(jsonPath("$.ONE_HANDED_WEAPON.MAX_HP").doesNotExist())
                .andExpect(jsonPath("$.HELMET.MAX_HP.max").value(maxHp.max()))
                .andExpect(jsonPath("$.RING.FIRE_DAMAGE.max").value(fireDamage.max()))
                .andExpect(jsonPath("$.RING.MAX_HP.max").value(maxHp.max()));
    }

    /** Only the custom item form needs them, and only an admin opens it. */
    @Test
    void aPlayerIsRefused() throws Exception {
        mockMvc.perform(get(RANGES_URL).with(tokenWithRoles(RoleConstants.PLAYER)))
                .andExpect(status().isForbidden());
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
