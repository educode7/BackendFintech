package com.wallet.account.infrastructure.web;

import com.wallet.account.application.AccountCommandService;
import com.wallet.account.application.AccountQueryService;
import com.wallet.account.domain.Account;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.domain.exception.InsufficientFundsException;
import com.wallet.account.infrastructure.projection.AccountProjectionService;
import com.wallet.account.infrastructure.projection.AccountView;
import com.wallet.account.infrastructure.web.exception.GlobalExceptionHandler;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountCommandService commandService;

    @MockitoBean
    private AccountQueryService queryService;

    @MockitoBean
    @SuppressWarnings("unused")
    private AccountProjectionService projectionService;

    @Test
    @DisplayName("POST /accounts con body válido retorna 201 con Location")
    void open_retorna201() throws Exception {
        AccountView view = sampleView("acc-1", "user-1");
        given(commandService.openAccount(eq("user-1"), any(Money.class))).willReturn(reloadedAccount("acc-1", "user-1"));

        String body = """
            { "userId": "user-1", "initialBalance": { "amount": 100.00, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/acc-1"))
            .andExpect(jsonPath("$.accountId").value("acc-1"))
            .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    @DisplayName("GET /accounts/{id} existente retorna 200 con la vista")
    void get_existente_retorna200() throws Exception {
        AccountView view = sampleView("acc-1", "user-1");
        given(queryService.findView("acc-1")).willReturn(view);

        mockMvc.perform(get("/api/v1/accounts/acc-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("acc-1"))
            .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    @DisplayName("GET /accounts/{id} inexistente retorna 404 ACCOUNT_NOT_FOUND")
    void get_inexistente_retorna404() throws Exception {
        given(queryService.findView("missing"))
            .willThrow(new AccountNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/accounts/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposits con amount válido retorna 200")
    void deposit_ok_retorna200() throws Exception {
        Account account = reloadedAccount("acc-1", "user-1");
        given(commandService.deposit(eq("acc-1"), any(Money.class))).willReturn(account);

        String body = """
            { "amount": 50.00, "currency": "USD" }
            """;

        mockMvc.perform(post("/api/v1/accounts/acc-1/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("acc-1"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdrawals con saldo insuficiente retorna 422 INSUFFICIENT_FUNDS")
    void withdraw_insuficiente_retorna422() throws Exception {
        given(commandService.withdraw(eq("acc-1"), any(Money.class)))
            .willThrow(new InsufficientFundsException("acc-1", new BigDecimal("10"), new BigDecimal("100")));

        String body = """
            { "amount": 100.00, "currency": "USD" }
            """;

        mockMvc.perform(post("/api/v1/accounts/acc-1/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    @DisplayName("POST /accounts con body inválido retorna 422 VALIDATION_FAILED")
    void open_userIdBlank_retorna422() throws Exception {
        String body = """
            { "userId": "", "initialBalance": { "amount": 100.00, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static AccountView sampleView(String accountId, String userId) {
        return new AccountView(
            accountId, userId,
            new BigDecimal("100.00"), "USD",
            Account.Status.OPEN, 1L,
            java.time.Instant.now()
        );
    }

    private static Account reloadedAccount(String accountId, String userId) {
        Account account = new Account();
        account.apply(new com.wallet.shared.event.AccountOpenedEvent(
            accountId, userId,
            new Money(new BigDecimal("100.00"), "USD"),
            new com.wallet.shared.event.EventMetadata("evt-1", java.time.Instant.now(), "corr-1", 1)
        ));
        return account;
    }
}
