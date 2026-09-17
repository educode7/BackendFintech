package com.wallet.auth.interfaces.rest;

import com.wallet.auth.application.service.TokenRefreshService;
import com.wallet.auth.application.service.TokenRevocationService;
import com.wallet.auth.domain.TokenPair;
import com.wallet.auth.interfaces.rest.dto.RefreshRequest;
import com.wallet.auth.interfaces.rest.dto.RefreshResponse;
import com.wallet.auth.interfaces.rest.dto.RevokeRequest;
import com.wallet.auth.interfaces.rest.dto.UserInfoResponse;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST adapter: token refresh, revocation, and identity endpoints.
 *
 * Refresh token flow:
 * - Cookie mode (preferred): refresh_token in HttpOnly Secure SameSite=Strict cookie
 * - Body mode (backward compat): refresh_token in JSON body (deprecated, will be removed)
 *
 * The cookie is automatically sent by the browser on every request to the auth domain.
 * The backend reads it when the request body is empty.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Token refresh, revocation, and user identity")
public class AuthResource {

    private static final Logger log = Logger.getLogger(AuthResource.class);

    /** Cookie name for the refresh token */
    static final String REFRESH_COOKIE = "refresh_token";

    /** Cookie max-age: 7 days in seconds */
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    private final TokenRefreshService refreshService;
    private final TokenRevocationService revocationService;

    @Inject
    SecurityIdentity identity;

    @Inject
    public AuthResource(TokenRefreshService refreshService,
                        TokenRevocationService revocationService) {
        this.refreshService = refreshService;
        this.revocationService = revocationService;
    }

    /**
     * Exchange a valid refresh token for a new token pair.
     *
     * Reads the refresh token from:
     * 1. The HttpOnly cookie (preferred — browser sends it automatically)
     * 2. The JSON request body (backward compatibility during migration)
     *
     * Sets the new refresh token as an HttpOnly cookie in the response.
     */
    @POST
    @Path("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new token pair")
    public Uni<Response> refresh(
            @CookieParam(REFRESH_COOKIE) String cookieRefreshToken,
            RefreshRequest request) {

        // Priority: cookie > body (body is deprecated, will be removed after frontend migration)
        String refreshToken = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request != null ? request.refreshToken() : null);

        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("missing_token", "Refresh token is required (cookie or body)"))
                    .build());
        }

        return refreshService.refresh(refreshToken)
                .map(pair -> {
                    RefreshResponse body = RefreshResponse.from(
                            pair.accessToken(),
                            pair.refreshToken(),
                            pair.expiresIn());

                    NewCookie refreshCookie = new NewCookie.Builder(REFRESH_COOKIE)
                            .value(pair.refreshToken())
                            .path("/api/v1/auth")
                            .httpOnly(true)
                            .secure(true)
                            .sameSite(NewCookie.SameSite.STRICT)
                            .maxAge(REFRESH_COOKIE_MAX_AGE)
                            .comment("Refresh token — HttpOnly, Secure, SameSite=Strict")
                            .build();

                    return Response.ok(body)
                            .cookie(refreshCookie)
                            .build();
                })
                .onFailure().transform(f -> {
                    log.warnf("Refresh failed: %s", f.getMessage());
                    return f;
                });
    }

    /**
     * Revoke a refresh token.
     *
     * Reads from cookie or body (same priority as /refresh).
     * Clears the cookie on success.
     */
    @POST
    @Path("/revoke")
    @Operation(summary = "Revoke a refresh token and clear the cookie")
    public Uni<Response> revoke(
            @CookieParam(REFRESH_COOKIE) String cookieRefreshToken,
            RevokeRequest request) {

        String refreshToken = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request != null ? request.refreshToken() : null);

        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("missing_token", "Refresh token is required (cookie or body)"))
                    .build());
        }

        NewCookie clearedCookie = new NewCookie.Builder(REFRESH_COOKIE)
                .value("")
                .path("/api/v1/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .maxAge(0)
                .build();

        return revocationService.revoke(refreshToken)
                .map(ignore -> Response.noContent().cookie(clearedCookie).build());
    }

    /**
     * Return the authenticated user's identity from the access_token.
     *
     * The frontend cannot decode the JWT (it's opaque), so this endpoint
     * extracts the identity claims and returns them.
     *
     * Requires a valid access_token in the Authorization header.
     */
    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get the current user's identity from the access token")
    public Uni<UserInfoResponse> me() {
        return Uni.createFrom().item(UserInfoResponse.fromIdentity(identity));
    }

    /** Simple error response body */
    record ErrorResponse(String code, String message) {}
}
