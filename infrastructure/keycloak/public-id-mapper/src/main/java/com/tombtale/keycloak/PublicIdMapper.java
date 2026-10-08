package com.tombtale.keycloak;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Puts the player's {@code publicId} in the token, and creates it on the user's first
 * token: a UUID of its own, so no service depends on Keycloak's user id (ADR 0024).
 */
public class PublicIdMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    /** The id the realm file names this mapper by. */
    public static final String PROVIDER_ID = "tombtale-public-id-mapper";

    /** The user attribute that keeps the id. The user profile does not manage it, so users cannot edit it. */
    public static final String ATTRIBUTE = "public_id";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, PublicIdMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Tomb Tale public id";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "The player's publicId. The user's first token creates it.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, publicIdOf(userSession.getUser()));
    }

    /**
     * The user's {@code publicId}, created and stored on first use. The write joins the
     * transaction that issues the token, so no token carries an id that was not stored.
     */
    static String publicIdOf(UserModel user) {
        String publicId = user.getFirstAttribute(ATTRIBUTE);

        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
            user.setSingleAttribute(ATTRIBUTE, publicId);
        }

        return publicId;
    }
}
