package com.tombtale.commons.security;

/**
 * The platform's three Zitadel project roles, and the {@code @PreAuthorize}
 * rules built from them. Lowercase, as Zitadel issues them; kept in sync with
 * the frontend's {@code PlatformRole}.
 *
 * <p>Strings rather than an enum because an annotation value must be a
 * compile-time constant, and concatenated literals are the only form the
 * compiler folds.
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
