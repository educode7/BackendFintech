package com.wallet.auth.interfaces.rest.dto;

import io.quarkus.security.identity.SecurityIdentity;

import java.util.List;

/**
 * User identity response — returned by GET /auth/me.
 * Extracts identity from the SecurityIdentity (works with both OIDC and test security).
 */
public record UserInfoResponse(
        String sub,
        String email,
        List<String> roles
) {
    public static UserInfoResponse fromIdentity(SecurityIdentity identity) {
        return new UserInfoResponse(
                identity.getPrincipal().getName(),
                identity.getAttribute("email"),
                identity.getRoles() != null ? List.copyOf(identity.getRoles()) : List.of()
        );
    }
}
