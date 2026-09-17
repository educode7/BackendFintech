package com.wallet.auth.interfaces.rest;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import com.wallet.auth.domain.MfaSetup;
import com.wallet.auth.domain.MfaVerification;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "test-user", roles = {"user"})
class MfaResourceTest {

    @InjectMock
    KeycloakMfaPort keycloakMfaPort;

    @Test
    void shouldReturnSetupData() {
        var setup = new MfaSetup("data:image/png;base64,abc", "SECRET",
                List.of("code1", "code2"), "WalletApp", "test-user");
        when(keycloakMfaPort.initiateSetup(anyString()))
                .thenReturn(Uni.createFrom().item(setup));

        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/auth/mfa/setup")
        .then()
            .statusCode(200)
            .body("secret", org.hamcrest.Matchers.equalTo("SECRET"))
            .body("issuer", org.hamcrest.Matchers.equalTo("WalletApp"))
            .body("recovery_codes.size()", org.hamcrest.Matchers.equalTo(2));
    }

    @Test
    void shouldVerifyValidCode() {
        var verification = new MfaVerification(true, 10);
        when(keycloakMfaPort.verifyCode(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(verification));

        given()
            .contentType(ContentType.JSON)
            .body("{\"code\": \"123456\"}")
        .when()
            .post("/auth/mfa/verify")
        .then()
            .statusCode(200)
            .body("verified", org.hamcrest.Matchers.equalTo(true))
            .body("backup_codes_remaining", org.hamcrest.Matchers.equalTo(10));
    }

    @Test
    void shouldDisableMfaSuccessfully() {
        when(keycloakMfaPort.disableMfa(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        given()
            .contentType(ContentType.JSON)
            .body("{\"code\": \"123456\"}")
        .when()
            .post("/auth/mfa/disable")
        .then()
            .statusCode(204);
    }

    @Test
    void shouldReturn400OnMissingCode() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/auth/mfa/verify")
        .then()
            .statusCode(400);
    }
}
