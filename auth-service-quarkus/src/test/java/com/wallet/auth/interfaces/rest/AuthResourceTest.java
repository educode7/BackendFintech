package com.wallet.auth.interfaces.rest;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import com.wallet.auth.domain.TokenPair;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class AuthResourceTest {

    @InjectMock
    KeycloakTokenPort keycloakTokenPort;

    @Test
    void shouldRefreshTokenSuccessfully() {
        var pair = TokenPair.of("new-access", "new-refresh", 300);
        when(keycloakTokenPort.exchangeRefreshToken("valid-refresh"))
                .thenReturn(Uni.createFrom().item(pair));

        given()
            .contentType(ContentType.JSON)
            .body("{\"refresh_token\": \"valid-refresh\"}")
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(200)
            .body("access_token", org.hamcrest.Matchers.equalTo("new-access"))
            .body("refresh_token", org.hamcrest.Matchers.equalTo("new-refresh"))
            .body("expires_in", org.hamcrest.Matchers.equalTo(300))
            .body("token_type", org.hamcrest.Matchers.equalTo("Bearer"));
    }

    @Test
    void shouldReturn400OnMissingRefreshToken() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(400);
    }

    @Test
    void shouldReturn500OnKeycloakFailure() {
        when(keycloakTokenPort.exchangeRefreshToken(anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Keycloak unavailable")));

        given()
            .contentType(ContentType.JSON)
            .body("{\"refresh_token\": \"some-token\"}")
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(500);
    }

    @Test
    void shouldRevokeTokenSuccessfully() {
        when(keycloakTokenPort.revokeRefreshToken("valid-refresh"))
                .thenReturn(Uni.createFrom().voidItem());

        given()
            .contentType(ContentType.JSON)
            .body("{\"refresh_token\": \"valid-refresh\"}")
        .when()
            .post("/auth/revoke")
        .then()
            .statusCode(204);
    }
}
