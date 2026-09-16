package com.wallet.auth.interfaces.rest;

import com.wallet.auth.application.service.MfaDisableService;
import com.wallet.auth.application.service.MfaSetupService;
import com.wallet.auth.application.service.MfaVerificationService;
import com.wallet.auth.domain.MfaSetup;
import com.wallet.auth.domain.MfaVerification;
import com.wallet.auth.interfaces.rest.dto.MfaDisableRequest;
import com.wallet.auth.interfaces.rest.dto.MfaSetupResponse;
import com.wallet.auth.interfaces.rest.dto.MfaVerifyRequest;
import com.wallet.auth.interfaces.rest.dto.MfaVerifyResponse;

import io.quarkus.security.Authenticated;
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
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * REST adapter: MFA/2FA endpoints.
 * All endpoints are authenticated — the user identity comes from the JWT.
 */
@Path("/auth/mfa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "MFA", description = "Multi-factor authentication setup, verification, and management")
public class MfaResource {

    private static final Logger log = Logger.getLogger(MfaResource.class);

    private final MfaSetupService setupService;
    private final MfaVerificationService verificationService;
    private final MfaDisableService disableService;

    @Inject
    JsonWebToken jwt;

    @Inject
    public MfaResource(MfaSetupService setupService,
                       MfaVerificationService verificationService,
                       MfaDisableService disableService) {
        this.setupService = setupService;
        this.verificationService = verificationService;
        this.disableService = disableService;
    }

    @POST
    @Path("/setup")
    @Operation(summary = "Initiate MFA setup — returns QR code, secret, and recovery codes")
    public Uni<MfaSetupResponse> setup() {
        String userId = jwt.getSubject();
        return setupService.setup(userId)
                .map(this::toSetupResponse);
    }

    @POST
    @Path("/verify")
    @Operation(summary = "Verify a TOTP code")
    public Uni<MfaVerifyResponse> verify(@Valid MfaVerifyRequest request) {
        String userId = jwt.getSubject();
        return verificationService.verify(userId, request.code())
                .map(this::toVerifyResponse);
    }

    @POST
    @Path("/disable")
    @Operation(summary = "Disable MFA — requires valid TOTP code for confirmation")
    public Uni<Response> disable(@Valid MfaDisableRequest request) {
        String userId = jwt.getSubject();
        return disableService.disable(userId, request.code())
                .map(ignore -> Response.noContent().build());
    }

    private MfaSetupResponse toSetupResponse(MfaSetup setup) {
        return new MfaSetupResponse(
                setup.qrCode(),
                setup.secret(),
                setup.recoveryCodes(),
                setup.issuer(),
                setup.accountName());
    }

    private MfaVerifyResponse toVerifyResponse(MfaVerification verification) {
        return new MfaVerifyResponse(
                verification.verified(),
                verification.backupCodesRemaining());
    }
}
