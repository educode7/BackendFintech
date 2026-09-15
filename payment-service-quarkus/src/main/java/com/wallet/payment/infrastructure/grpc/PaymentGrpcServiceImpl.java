package com.wallet.payment.infrastructure.grpc;

import jakarta.inject.Inject;

import com.wallet.payment.application.GetPaymentUseCase;
import com.wallet.payment.application.PaymentResponse;
import com.wallet.payment.application.ProcessPaymentCommand;
import com.wallet.payment.application.ProcessPaymentUseCase;
import com.wallet.payment.domain.exception.DuplicatePaymentException;
import com.wallet.payment.domain.exception.MissingIdempotencyKeyException;
import com.wallet.payment.domain.exception.PaymentNotFoundException;
import com.wallet.payment.proto.GetPaymentRequest;
import com.wallet.payment.proto.GetPaymentResponse;
import com.wallet.payment.proto.Money;
import com.wallet.payment.proto.MutinyPaymentServiceGrpc;
import com.wallet.payment.proto.PaymentStatus;
import com.wallet.payment.proto.ProcessPaymentRequest;
import com.wallet.payment.proto.ProcessPaymentResponse;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

/**
 * Infrastructure adapter: gRPC server for Payment Service.
 * <p>
 * Exposes ProcessPayment and GetPayment over gRPC for inter-service communication.
 * Uses {@code request_id} for idempotency (mandatory on commands with side effects).
 */
@GrpcService
public class PaymentGrpcServiceImpl extends MutinyPaymentServiceGrpc.PaymentServiceImplBase {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @Inject
    public PaymentGrpcServiceImpl(ProcessPaymentUseCase processPaymentUseCase,
                                   GetPaymentUseCase getPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @Override
    public Uni<ProcessPaymentResponse> processPayment(ProcessPaymentRequest request) {
        // Extract request_id (idempotency key)
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            return Uni.createFrom().failure(new MissingIdempotencyKeyException());
        }

        // Use requestId as correlationId when called via gRPC directly
        String correlationId = requestId;

        // Map proto Money to domain Money
        com.wallet.shared.money.Money amount = new com.wallet.shared.money.Money(
                new java.math.BigDecimal(request.getAmount().getAmount()),
                request.getAmount().getCurrency());

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                request.getAccountId(), request.getUserId(), amount, requestId);

        return processPaymentUseCase.execute(command, correlationId)
                .map(this::toGrpcResponse)
                .onFailure(PaymentNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(DuplicatePaymentException.class)
                        .transform(e -> io.grpc.Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(MissingIdempotencyKeyException.class)
                        .transform(e -> io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<GetPaymentResponse> getPayment(GetPaymentRequest request) {
        return getPaymentUseCase.execute(request.getPaymentId())
                .map(this::toGrpcGetResponse)
                .onFailure(PaymentNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }

    private ProcessPaymentResponse toGrpcResponse(PaymentResponse response) {
        return ProcessPaymentResponse.newBuilder()
                .setPaymentId(response.id())
                .setUserId(response.userId())
                .setAmount(Money.newBuilder()
                        .setAmount(response.amount().toPlainString())
                        .setCurrency(response.currency())
                        .build())
                .setStatus(mapStatus(response.status()))
                .setCreatedAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(response.createdAt().getEpochSecond())
                        .setNanos(response.createdAt().getNano())
                        .build())
                .build();
    }

    private GetPaymentResponse toGrpcGetResponse(PaymentResponse response) {
        return GetPaymentResponse.newBuilder()
                .setPaymentId(response.id())
                .setUserId(response.userId())
                .setAmount(Money.newBuilder()
                        .setAmount(response.amount().toPlainString())
                        .setCurrency(response.currency())
                        .build())
                .setStatus(mapStatus(response.status()))
                .setCreatedAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(response.createdAt().getEpochSecond())
                        .setNanos(response.createdAt().getNano())
                        .build())
                .setUpdatedAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(response.updatedAt().getEpochSecond())
                        .setNanos(response.updatedAt().getNano())
                        .build())
                .build();
    }

    private PaymentStatus mapStatus(String status) {
        return switch (status) {
            case "PENDING" -> PaymentStatus.PAYMENT_STATUS_PENDING;
            case "PROCESSING" -> PaymentStatus.PAYMENT_STATUS_PROCESSING;
            case "COMPLETED" -> PaymentStatus.PAYMENT_STATUS_COMPLETED;
            case "FAILED" -> PaymentStatus.PAYMENT_STATUS_FAILED;
            default -> PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
        };
    }
}
