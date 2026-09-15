package com.wallet.payment.infrastructure.web;

import java.net.URI;
import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.wallet.payment.application.GetPaymentUseCase;
import com.wallet.payment.application.PaymentResponse;
import com.wallet.payment.application.ProcessPaymentCommand;
import com.wallet.payment.application.ProcessPaymentUseCase;
import com.wallet.payment.domain.exception.DuplicatePaymentException;
import com.wallet.payment.domain.exception.MissingIdempotencyKeyException;
import com.wallet.shared.api.PageResponse;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;

/**
 * REST adapter: Payment API endpoint.
 * <p>
 * Implements the OpenAPI contract defined in contracts/openapi/payment-service-v1.yaml.
 * Uses Quarkus REST (reactive, non-blocking).
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Payment creation and retrieval")
public class PaymentResource {

    private static final Logger log = Logger.getLogger(PaymentResource.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @Inject
    public PaymentResource(ProcessPaymentUseCase processPaymentUseCase,
                           GetPaymentUseCase getPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @POST
    @Operation(summary = "Process a new payment", description = "Creates a payment. Requires Idempotency-Key header.")
    @APIResponse(responseCode = "201", description = "Payment created")
    @APIResponse(responseCode = "400", description = "Invalid request", content = @Content)
    @APIResponse(responseCode = "409", description = "Duplicate payment", content = @Content)
    @APIResponse(responseCode = "422", description = "Validation error", content = @Content)
    public Uni<Response> processPayment(
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ProcessPaymentRequest request,
            @Context UriInfo uriInfo) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }

        Money amount = new Money(request.amount().amount(), request.amount().currency());
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                request.accountId(), request.userId(), amount, idempotencyKey);

        String correlationId = uriInfo.getRequestUri().toString();

        return processPaymentUseCase.execute(command, correlationId)
                .map(response -> {
                    URI location = uriInfo.getAbsolutePathBuilder()
                            .path(response.id())
                            .build();
                    return Response.created(location).entity(response).build();
                })
                .onFailure(MissingIdempotencyKeyException.class)
                        .recoverWithItem(e -> jakarta.ws.rs.core.Response.status(400)
                                .entity(new com.wallet.shared.api.ErrorResponse(
                                        URI.create("urn:wallet:payment:IDEMPOTENCY_KEY_MISSING"),
                                        "Idempotency Key Missing",
                                        400,
                                        e.getMessage(),
                                        null,
                                        "IDEMPOTENCY_KEY_MISSING",
                                        null,
                                        Instant.now()))
                                .build())
                .onFailure(DuplicatePaymentException.class)
                        .recoverWithItem(e -> jakarta.ws.rs.core.Response.status(409)
                                .entity(new com.wallet.shared.api.ErrorResponse(
                                        URI.create("urn:wallet:payment:DUPLICATE_PAYMENT"),
                                        "Duplicate Payment",
                                        409,
                                        e.getMessage(),
                                        null,
                                        "DUPLICATE_PAYMENT",
                                        null,
                                        Instant.now()))
                                .build());
    }

    @GET
    @Operation(summary = "List all payments with pagination")
    public Uni<PageResponse<PaymentResponse>> list(
            @QueryParam("page") @Min(0) int page,
            @QueryParam("size") @Min(1) @Max(100) int size) {

        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;

        return getPaymentUseCase.findAll(page, size);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get payment by ID")
    @APIResponse(responseCode = "200", description = "Payment found")
    @APIResponse(responseCode = "404", description = "Payment not found", content = @Content)
    public Uni<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathParam("id") String id) {
        return getPaymentUseCase.execute(id);
    }
}
