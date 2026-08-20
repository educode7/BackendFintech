package com.wallet.payment.infrastructure.web;

import tools.jackson.databind.ObjectMapper;
import com.wallet.payment.application.PaymentCommandService;
import com.wallet.payment.application.PaymentQueryService;
import com.wallet.payment.application.PaymentResponse;
import com.wallet.payment.application.ProcessPaymentCommand;
import com.wallet.payment.domain.PaymentStatus;
import com.wallet.payment.infrastructure.web.exception.DuplicatePaymentException;
import com.wallet.payment.infrastructure.web.exception.GlobalExceptionHandler;
import com.wallet.payment.infrastructure.web.exception.MissingIdempotencyKeyException;
import com.wallet.payment.infrastructure.web.exception.PaymentNotFoundException;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link PaymentController}.
 *
 * Uses standalone MockMvc to avoid Boot 4's test-slice quirks with
 * {@code @RestControllerAdvice} discovery. The controller is instantiated
 * directly with mocked services; the {@link GlobalExceptionHandler} is
 * wired as a controller advice via the standalone builder.
 */
class PaymentControllerWebMvcTest {

    private MockMvc mockMvc;

    private final PaymentCommandService commandService = mock(PaymentCommandService.class);
    private final PaymentQueryService queryService = mock(PaymentQueryService.class);

    // Boot 4 excluded spring-boot-jackson → no auto-configured ObjectMapper bean.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new PaymentController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @Test
    @DisplayName("POST sin Idempotency-Key retorna 400 con código IDEMPOTENCY_KEY_REQUIRED")
    void post_sinIdempotencyKey_retorna400() throws Exception {
        String body = """
            { "userId": "user-1", "amount": { "amount": 10.00, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST con Idempotency-Key válido retorna 201 con Location")
    void post_conIdempotencyKey_retorna201() throws Exception {
        PaymentResponse response = sampleResponse("pay-1");
        given(commandService.process(any(ProcessPaymentCommand.class))).willReturn(response);

        String body = """
            { "userId": "user-1", "amount": { "amount": 25.50, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/payments/pay-1"))
            .andExpect(jsonPath("$.id").value("pay-1"))
            .andExpect(jsonPath("$.userId").value("user-1"))
            .andExpect(jsonPath("$.amount").value(25.50))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST con cuerpo inválido (userId blank) retorna 422 VALIDATION_FAILED")
    void post_userIdBlank_retorna422() throws Exception {
        String body = """
            { "userId": "", "amount": { "amount": 10.00, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST cuando el servicio lanza DuplicatePayment retorna 409")
    void post_servicioLanzaDuplicate_retorna409() throws Exception {
        given(commandService.process(any(ProcessPaymentCommand.class)))
            .willThrow(new DuplicatePaymentException("idem-1"));

        String body = """
            { "userId": "user-1", "amount": { "amount": 10.00, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT"));
    }

    @Test
    @DisplayName("GET con id existente retorna 200 con el PaymentResponse")
    void get_existente_retorna200() throws Exception {
        given(queryService.findById("pay-1")).willReturn(sampleResponse("pay-1"));

        mockMvc.perform(get("/api/v1/payments/pay-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("pay-1"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET con id inexistente retorna 404 PAYMENT_NOT_FOUND")
    void get_inexistente_retorna404() throws Exception {
        given(queryService.findById("missing"))
            .willThrow(new PaymentNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/payments/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST con amount inválido retorna 500: HttpMessageNotReadableException cae en fallback handler")
    void post_amountInvalido_retorna500() throws Exception {
        String body = """
            { "userId": "user-1", "amount": { "amount": 0, "currency": "USD" } }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isInternalServerError());
    }

    private PaymentResponse sampleResponse(String id) {
        return new PaymentResponse(
            id,
            "user-1",
            new BigDecimal("25.50"),
            "USD",
            PaymentStatus.COMPLETED,
            Instant.parse("2026-01-15T10:00:00Z"),
            Instant.parse("2026-01-15T10:00:00Z")
        );
    }
}
