package com.wallet.account.infrastructure.grpc;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.inject.Inject;

import com.wallet.account.application.AccountCommand;
import com.wallet.account.application.AccountCommandService;
import com.wallet.account.application.AccountQueryService;
import com.wallet.account.application.AccountResponse;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.domain.exception.ConcurrentModificationException;
import com.wallet.account.domain.exception.InsufficientFundsException;

import com.wallet.account.proto.*;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

/**
 * Infrastructure adapter: gRPC server for Account Service.
 */
@GrpcService
public class AccountGrpcServiceImpl extends MutinyAccountServiceGrpc.AccountServiceImplBase {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    @Inject
    public AccountGrpcServiceImpl(AccountCommandService commandService, AccountQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Override
    public Uni<OpenAccountResponse> openAccount(OpenAccountRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId() : "";
        com.wallet.shared.money.Money initialBalance = new com.wallet.shared.money.Money(
                new BigDecimal(request.getInitialBalance().getAmount()),
                request.getInitialBalance().getCurrency());

        AccountCommand.OpenAccount command = new AccountCommand.OpenAccount(
                request.getUserId(), initialBalance, requestId);

        return commandService.openAccount(command)
                .map(this::toGrpcOpenResponse)
                .onFailure(AccountNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(ConcurrentModificationException.class)
                        .transform(e -> io.grpc.Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<GetAccountResponse> getAccount(GetAccountRequest request) {
        return queryService.findById(request.getAccountId())
                .map(this::toGrpcGetResponse)
                .onFailure(AccountNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<DepositResponse> deposit(DepositRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId() : "";
        AccountCommand.Deposit command = new AccountCommand.Deposit(
                request.getAccountId(),
                new BigDecimal(request.getAmount().getAmount()),
                request.getAmount().getCurrency(),
                requestId);

        return commandService.deposit(command)
                .map(this::toGrpcDepositResponse)
                .onFailure(AccountNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(ConcurrentModificationException.class)
                        .transform(e -> io.grpc.Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<WithdrawResponse> withdraw(WithdrawRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId() : "";
        AccountCommand.Withdraw command = new AccountCommand.Withdraw(
                request.getAccountId(),
                new BigDecimal(request.getAmount().getAmount()),
                request.getAmount().getCurrency(),
                requestId);

        return commandService.withdraw(command)
                .map(this::toGrpcWithdrawResponse)
                .onFailure(AccountNotFoundException.class)
                        .transform(e -> io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(InsufficientFundsException.class)
                        .transform(e -> io.grpc.Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException())
                .onFailure(ConcurrentModificationException.class)
                        .transform(e -> io.grpc.Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    }

    private OpenAccountResponse toGrpcOpenResponse(AccountResponse r) {
        return OpenAccountResponse.newBuilder()
                .setAccountId(r.accountId())
                .setUserId(r.userId())
                .setBalance(Money.newBuilder().setAmount(r.balanceAmount().toPlainString()).setCurrency(r.balanceCurrency()).build())
                .setStatus(mapStatus(r.status()))
                .setVersion(r.version())
                .setCreatedAt(toTimestamp(r.lastUpdated()))
                .build();
    }

    private GetAccountResponse toGrpcGetResponse(AccountResponse r) {
        return GetAccountResponse.newBuilder()
                .setAccountId(r.accountId())
                .setUserId(r.userId())
                .setBalance(Money.newBuilder().setAmount(r.balanceAmount().toPlainString()).setCurrency(r.balanceCurrency()).build())
                .setStatus(mapStatus(r.status()))
                .setVersion(r.version())
                .setLastUpdated(toTimestamp(r.lastUpdated()))
                .build();
    }

    private DepositResponse toGrpcDepositResponse(AccountResponse r) {
        return DepositResponse.newBuilder()
                .setAccountId(r.accountId())
                .setNewBalance(Money.newBuilder().setAmount(r.balanceAmount().toPlainString()).setCurrency(r.balanceCurrency()).build())
                .setStatus(mapStatus(r.status()))
                .setVersion(r.version())
                .setUpdatedAt(toTimestamp(r.lastUpdated()))
                .build();
    }

    private WithdrawResponse toGrpcWithdrawResponse(AccountResponse r) {
        return WithdrawResponse.newBuilder()
                .setAccountId(r.accountId())
                .setNewBalance(Money.newBuilder().setAmount(r.balanceAmount().toPlainString()).setCurrency(r.balanceCurrency()).build())
                .setStatus(mapStatus(r.status()))
                .setVersion(r.version())
                .setUpdatedAt(toTimestamp(r.lastUpdated()))
                .build();
    }

    private AccountStatus mapStatus(String status) {
        return switch (status) {
            case "OPEN" -> AccountStatus.ACCOUNT_STATUS_OPEN;
            case "CLOSED" -> AccountStatus.ACCOUNT_STATUS_CLOSED;
            default -> AccountStatus.ACCOUNT_STATUS_UNSPECIFIED;
        };
    }

    private com.google.protobuf.Timestamp toTimestamp(Instant instant) {
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
