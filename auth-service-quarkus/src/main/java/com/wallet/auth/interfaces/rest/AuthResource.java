package com.wallet.auth.interfaces.rest;

import com.wallet.auth.application.service.TokenRefreshService;
import com.wallet.auth.application.service.TokenRevocationService;
import com.wallet.auth.domain.TokenPair;
import com.wallet.auth.interfaces.rest.dto.RefreshRequest;
import com.wallet.auth.interfaces.rest.dto.RefreshResponse;
import com.wallet.auth.interfaces.rest.dto.RevokeRequest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST adapter: token refresh and revocation endpoints.
 * These are token-level operations — the refresh token itself is the credential.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Token refresh and revocation")
public class AuthResource {

    private static final Logger log = Logger.getLogger(AuthResource.class);

    private final TokenRefreshService refreshService;
    private final TokenRevocationService revocationService;

    @Inject
    public AuthResource(TokenRefreshService refreshService,
                        TokenRevocationService revocationService) {
        this.refreshService = refreshService;
        this.revocationService = revocationService;
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new token pair")
    public Uni<RefreshResponse> refresh(@Valid RefreshRequest request) {
        return refreshService.refresh(request.refreshToken())
                .map(pair -> RefreshResponse.from(
                        pair.accessToken(),
                        pair.refreshToken(),
                        pair.expiresIn()))
                .onFailure().transform(f -> {
                    log.warnf("Refresh failed: %s", f.getMessage());
                    return f;
                });
    }

    @POST
    @Path("/revoke")
    @Operation(summary = "Revoke a refresh token")
    public Uni<Response> revoke(@Valid RevokeRequest request) {
        return revocationService.revoke(request.refreshToken())
                .map(ignore -> Response.noContent().build());
    }
}
