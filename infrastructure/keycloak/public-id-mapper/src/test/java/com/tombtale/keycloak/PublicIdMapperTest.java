package com.tombtale.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;

// Mockito and AssertJ are read through static imports, one per call.
@SuppressWarnings("PMD.TooManyStaticImports")
class PublicIdMapperTest {

    private static final String STORED = "6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f3b64";

    @Test
    void reusesTheStoredId() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(PublicIdMapper.ATTRIBUTE)).thenReturn(STORED);

        assertThat(PublicIdMapper.publicIdOf(user)).isEqualTo(STORED);
        verify(user, never()).setSingleAttribute(anyString(), anyString());
    }

    /** The first token stores what it hands out, so the next token carries the same id. */
    @Test
    void createsAndStoresAnIdOnTheFirstToken() {
        UserModel user = mock(UserModel.class);

        String created = PublicIdMapper.publicIdOf(user);

        assertThat(UUID.fromString(created)).hasToString(created);
        verify(user).setSingleAttribute(PublicIdMapper.ATTRIBUTE, created);
    }

    /** The claim carries the attribute, never the Keycloak user id. */
    @Test
    void putsTheStoredIdInTheConfiguredClaim() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(PublicIdMapper.ATTRIBUTE)).thenReturn(STORED);
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getUser()).thenReturn(user);
        ProtocolMapperModel mappingModel = new ProtocolMapperModel();
        mappingModel.setConfig(Map.of(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "public_id"));
        AccessToken token = new AccessToken();

        new PublicIdMapper().setClaim(token, mappingModel, userSession);

        assertThat(token.getOtherClaims()).containsEntry("public_id", STORED);
    }
}
