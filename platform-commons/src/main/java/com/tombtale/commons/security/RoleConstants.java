package com.tombtale.commons.security;

/**
 * The platform's three Zitadel project roles, and the authorization
 * expressions built from them.
 *
 * <p>
 * The names are lowercase because that is how Zitadel issues them in the
 * {@code urn:zitadel:iam:org:project:roles} claim, and
 * {@link ZitadelRoleConverter} passes them through unchanged. They match the
 * {@code PlatformRole} enum in the frontend.
 *
 * <p>
 * The {@code IS_*} constants exist so a controller can write
 * {@code @PreAuthorize(RoleConstants.IS_ADMIN)} instead of repeating a SpEL
 * string. Annotation values must be compile-time constants, which rules out
 * an enum here: concatenating {@code static final String} literals is the
 * only form the compiler will fold.
 */
// PMD reads a class of public constants with no behaviour as a Data Class.
// That is exactly what this is, and what it has to be to work in an annotation.
@SuppressWarnings("PMD.DataClass")
public final class RoleConstants {

    /** A normal game account. Holds no rights in the admin portal. */
    public static final String PLAYER = "player";

    /** Support and moderation. Reads everything, changes nothing in commerce. */
    public static final String GAME_MASTER = "game_master";

    /** Full access, including every mutation. */
    public static final String PLATFORM_ADMIN = "platform_admin";

    /** Authorization rule: {@code platform_admin} only. */
    public static final String IS_ADMIN =
            "hasAuthority('" + PLATFORM_ADMIN + "')";

    /** Authorization rule: {@code platform_admin} or {@code game_master}. */
    public static final String IS_ADMIN_OR_GAME_MASTER =
            IS_ADMIN + " or hasAuthority('" + GAME_MASTER + "')";

    /** Authorization rule: any caller with a valid token. */
    public static final String IS_AUTHENTICATED = "isAuthenticated()";

    private RoleConstants() {
        // Constants holder.
    }
}
