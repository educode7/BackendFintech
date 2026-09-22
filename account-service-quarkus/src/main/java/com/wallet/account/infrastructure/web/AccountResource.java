package com.wallet.account.infrastructure.web;

import java.math.BigDecimal;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.wallet.account.application.AccountCommand;
import com.wallet.account.application.AccountCommandService;
import com.wallet.account.application.AccountQueryService;
import com.wallet.account.application.AccountResponse;
import com.wallet.shared.event.AccountData;
import com.wallet.shared.event.AccountType;
import com.wallet.shared.event.HolderDocumentType;
import com.wallet.shared.api.PageResponse;
import com.wallet.shared.money.Money;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

/**
 * REST adapter: Account API endpoint.
 */
@Path("/api/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
@Tag(name = "Accounts", description = "Account lifecycle and balance operations")
public class AccountResource {

    private static final Logger log = Logger.getLogger(AccountResource.class);

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    @Inject
    public AccountResource(AccountCommandService commandService, AccountQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GET
    @Operation(summary = "List all accounts with pagination")
    public Uni<PageResponse<AccountResponse>> list(
            @QueryParam("page") @Min(0) int page,
            @QueryParam("size") @Min(1) @Max(100) int size,
            @QueryParam("userId") String userId) {

        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;

        return queryService.findAll(page, size, userId);
    }

    @POST
    @Operation(summary = "Open a new account")
    public Uni<Response> openAccount(@Valid OpenAccountRequest request, @Context UriInfo uriInfo) {
        Money initialBalance = new Money(request.initialBalance().amount(), request.initialBalance().currency());

        // Build AccountData from request
        AccountData accountData = buildAccountData(request);

        AccountCommand.OpenAccount command = new AccountCommand.OpenAccount(
                request.userId(), initialBalance, null, accountData);

        return commandService.openAccount(command)
                .map(response -> {
                    URI location = uriInfo.getAbsolutePathBuilder().path(response.accountId()).build();
                    return Response.created(location).entity(response).build();
                });
    }

    private AccountData buildAccountData(OpenAccountRequest request) {
        return new AccountData(
                request.accountNumber(),
                request.accountType() != null ? AccountType.valueOf(request.accountType()) : null,
                request.cci(),
                request.iban(),
                request.swiftBic(),
                request.holderName(),
                request.holderDocumentType() != null ? HolderDocumentType.valueOf(request.holderDocumentType()) : null,
                request.holderDocumentNumber(),
                request.holderEmail(),
                request.holderPhone(),
                request.bankCode(),
                request.bankName(),
                request.currency(),
                request.country(),
                null, // availableAmount — not in open request
                null, // holdAmount — not in open request
                request.overdraftLimit() != null ? new Money(new BigDecimal(request.overdraftLimit()), request.initialBalance().currency()) : null,
                request.dailyLimit() != null ? new Money(new BigDecimal(request.dailyLimit()), request.initialBalance().currency()) : null,
                request.monthlyLimit() != null ? new Money(new BigDecimal(request.monthlyLimit()), request.initialBalance().currency()) : null,
                request.singleTransactionLimit() != null ? new Money(new BigDecimal(request.singleTransactionLimit()), request.initialBalance().currency()) : null
        );
    }

    @GET
    @Path("/{accountId}")
    @Operation(summary = "Get account by ID")
    public Uni<AccountResponse> getAccount(@PathParam("accountId") String accountId) {
        return queryService.findById(accountId);
    }

    @POST
    @Path("/{accountId}/deposits")
    @Operation(summary = "Deposit funds into an account")
    public Uni<AccountResponse> deposit(@PathParam("accountId") String accountId,
                                        @Valid DepositRequest request) {
        AccountCommand.Deposit command = new AccountCommand.Deposit(
                accountId, request.amount(), request.currency(), null);
        return commandService.deposit(command);
    }

    @POST
    @Path("/{accountId}/withdrawals")
    @Operation(summary = "Withdraw funds from an account")
    public Uni<AccountResponse> withdraw(@PathParam("accountId") String accountId,
                                         @Valid WithdrawRequest request) {
        AccountCommand.Withdraw command = new AccountCommand.Withdraw(
                accountId, request.amount(), request.currency(), null);
        return commandService.withdraw(command);
    }

    // --- Request DTOs ---

    public record OpenAccountRequest(
            @NotBlank String userId,
            @NotNull MoneyAmount initialBalance,
            String accountNumber,
            String accountType,
            String cci,
            String iban,
            String swiftBic,
            String holderName,
            String holderDocumentType,
            String holderDocumentNumber,
            String holderEmail,
            String holderPhone,
            String bankCode,
            String bankName,
            String currency,
            String country,
            String dailyLimit,
            String monthlyLimit,
            String singleTransactionLimit,
            String overdraftLimit
    ) {
        public record MoneyAmount(@NotNull BigDecimal amount, @NotBlank String currency) {}
    }

    public record DepositRequest(
            @NotNull @DecimalMin("0.0001") BigDecimal amount,
            @NotBlank String currency
    ) {}

    public record WithdrawRequest(
            @NotNull @DecimalMin("0.0001") BigDecimal amount,
            @NotBlank String currency
    ) {}
}
