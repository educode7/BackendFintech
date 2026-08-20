package com.wallet.account.infrastructure.web;

import com.wallet.account.infrastructure.projection.AccountView;
import com.wallet.account.application.AccountCommandService;
import com.wallet.account.application.AccountQueryService;
import com.wallet.account.infrastructure.web.dto.DepositRequest;
import com.wallet.account.infrastructure.web.dto.OpenAccountRequest;
import com.wallet.account.infrastructure.web.dto.WithdrawRequest;
import com.wallet.shared.money.Money;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST surface for account operations.
 *
 * Authoritative writes always go through {@link AccountCommandService}, which
 * appends events. Reads hit the projection in Redis first via the query service.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    @PostMapping
    public ResponseEntity<AccountView> open(@Valid @RequestBody OpenAccountRequest body) {
        AccountView view = AccountView.fromDomain(commandService.openAccount(body.userId(), body.initialBalance()));
        return ResponseEntity
            .created(URI.create("/api/v1/accounts/" + view.accountId()))
            .body(view);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountView> get(@PathVariable String accountId) {
        return ResponseEntity.ok(queryService.findView(accountId));
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<AccountView> deposit(
        @PathVariable String accountId,
        @Valid @RequestBody DepositRequest body
    ) {
        Money amount = body.toMoney();
        AccountView view = AccountView.fromDomain(commandService.deposit(accountId, amount));
        return ResponseEntity.status(HttpStatus.OK).body(view);
    }

    @PostMapping("/{accountId}/withdrawals")
    public ResponseEntity<AccountView> withdraw(
        @PathVariable String accountId,
        @Valid @RequestBody WithdrawRequest body
    ) {
        Money amount = body.toMoney();
        AccountView view = AccountView.fromDomain(commandService.withdraw(accountId, amount));
        return ResponseEntity.status(HttpStatus.OK).body(view);
    }
}
