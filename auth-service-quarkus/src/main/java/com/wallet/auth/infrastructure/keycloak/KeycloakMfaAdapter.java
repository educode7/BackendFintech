package com.wallet.auth.infrastructure.keycloak;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import com.wallet.auth.domain.MfaSetup;
import com.wallet.auth.domain.MfaVerification;
import com.wallet.auth.domain.exception.InvalidMfaCodeException;
import com.wallet.auth.domain.exception.MfaAlreadyEnabledException;
import com.wallet.auth.domain.exception.MfaNotEnabledException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Keycloak adapter for MFA operations — TOTP generation/validation, QR codes, recovery codes.
 * <p>
 * MFA state is stored as Keycloak user attributes via the Admin REST API.
 */
@ApplicationScoped
public class KeycloakMfaAdapter implements KeycloakMfaPort {

    @ConfigProperty(name = "keycloak.admin.url")
    String adminUrl;

    @ConfigProperty(name = "keycloak.admin.username")
    String adminUsername;

    @ConfigProperty(name = "keycloak.admin.password")
    String adminPassword;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "wallet.auth.mfa.totp.algorithm", defaultValue = "HmacSHA1")
    String totpAlgorithm;

    @ConfigProperty(name = "wallet.auth.mfa.totp.digits", defaultValue = "6")
    int totpDigits;

    @ConfigProperty(name = "wallet.auth.mfa.totp.period", defaultValue = "30")
    int totpPeriod;

    @ConfigProperty(name = "wallet.auth.mfa.totp.window", defaultValue = "1")
    int totpWindow;

    @ConfigProperty(name = "wallet.auth.mfa.recovery-codes.count", defaultValue = "10")
    int recoveryCodeCount;

    @ConfigProperty(name = "wallet.auth.mfa.recovery-codes.length", defaultValue = "8")
    int recoveryCodeLength;

    private final WebClient webClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private String adminToken;

    @Inject
    public KeycloakMfaAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    void init() {
        Log.infof("KeycloakMfaAdapter initialized — algorithm=%s, digits=%d, period=%ds",
                totpAlgorithm, totpDigits, totpPeriod);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Setup
    // ────────────────────────────────────────────────────────────────────

    @Override
    public Uni<MfaSetup> initiateSetup(String userId) {
        return getAdminToken()
                .chain(token -> isMfaEnabled(userId, token))
                .chain(enabled -> {
                    if (enabled) {
                        return Uni.createFrom().failure(new MfaAlreadyEnabledException(userId));
                    }
                    return getAdminToken();
                })
                .chain(token -> {
                    // Generate TOTP secret (160-bit / 20 bytes → 32-char base32)
                    String secret = generateTOTPSecret();
                    List<String> recoveryCodes = generateRecoveryCodes();

                    // Retrieve user email for QR code label
                    return getUserEmail(userId, token)
                            .map(email -> {
                                // Store attributes in Keycloak
                                storeUserAttribute(userId, "mfa_secret", secret, token);
                                storeUserAttribute(userId, "mfa_enabled", "false", token); // pending verification
                                storeUserAttributeJsonArray(userId, "mfa_recovery_codes", recoveryCodes, token);

                                // Build QR code URI (Google Authenticator-compatible)
                                String issuer = "Wallet";
                                String qrUri = String.format(
                                        "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                                        issuer, email, secret, issuer, "SHA1", totpDigits, totpPeriod
                                );

                                // Generate QR code as Base64-encoded PNG
                                String qrCodeBase64 = generateQRCodePng(qrUri);

                                return new MfaSetup(qrCodeBase64, secret, recoveryCodes, issuer, email);
                            });
                });
    }

    // ────────────────────────────────────────────────────────────────────
    //  Verify
    // ────────────────────────────────────────────────────────────────────

    @Override
    public Uni<MfaVerification> verifyCode(String userId, String code) {
        return getAdminToken()
                .chain(token -> getUserAttribute(userId, "mfa_secret", token)
                        .map(secret -> new Object[]{secret, token}))
                .map(result -> {
                    String secret = (String) result[0];
                    String token = (String) result[1];

                    boolean valid = validateTOTP(secret, code);
                    if (!valid) {
                        throw new InvalidMfaCodeException();
                    }

                    // If MFA was pending, activate it now
                    String enabled = getUserAttributeValue(userId, "mfa_enabled", token);
                    if (!"true".equals(enabled)) {
                        storeUserAttribute(userId, "mfa_enabled", "true", token);
                    }

                    int remaining = getRecoveryCodesCount(userId, token);
                    return new MfaVerification(true, remaining);
                });
    }

    // ────────────────────────────────────────────────────────────────────
    //  Disable
    // ────────────────────────────────────────────────────────────────────

    @Override
    public Uni<Void> disableMfa(String userId, String code) {
        return getAdminToken()
                .chain(token -> {
                    // Verify code before disabling
                    return getUserAttribute(userId, "mfa_secret", token)
                            .map(secret -> {
                                boolean valid = validateTOTP(secret, code);
                                if (!valid) {
                                    throw new InvalidMfaCodeException();
                                }

                                // Remove MFA attributes
                                removeUserAttribute(userId, "mfa_enabled", token);
                                removeUserAttribute(userId, "mfa_secret", token);
                                removeUserAttribute(userId, "mfa_recovery_codes", token);

                                Log.infof("MFA disabled for user %s", userId);
                                return null;
                            });
                })
                .replaceWithVoid();
    }

    // ────────────────────────────────────────────────────────────────────
    //  TOTP Algorithm (RFC 6238)
    // ────────────────────────────────────────────────────────────────────

    private String generateTOTPSecret() {
        byte[] buffer = new byte[20]; // 160 bits
        secureRandom.nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }

    boolean validateTOTP(String secret, String code) {
        long timeStep = System.currentTimeMillis() / (totpPeriod * 1000L);

        // Check current window ± configured window
        for (int i = -totpWindow; i <= totpWindow; i++) {
            String expected = generateTOTP(secret, timeStep + i);
            if (constantTimeEquals(expected, code)) {
                return true;
            }
        }
        return false;
    }

    String generateTOTP(String secret, long timeStep) {
        try {
            byte[] key = Base64.getDecoder().decode(secret);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation (RFC 4226 §5.4)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, totpDigits);
            return String.format("%0" + totpDigits + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Recovery Codes
    // ────────────────────────────────────────────────────────────────────

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(recoveryCodeCount);
        for (int i = 0; i < recoveryCodeCount; i++) {
            codes.add(generateAlphanumericCode(recoveryCodeLength));
        }
        return codes;
    }

    private String generateAlphanumericCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // ambiguous chars removed
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────
    //  QR Code Generation (minimal PNG via Google ZXing-free approach)
    // ────────────────────────────────────────────────────────────────────

    private String generateQRCodePng(String content) {
        // Use Keycloak admin API to generate TOTP QR or return a data URI
        // For simplicity, return a Base64-encoded SVG as placeholder
        // In production, use ZXing or a QR code library
        try {
            String svg = generateSimpleQrSvg(content);
            byte[] svgBytes = svg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(svgBytes);
        } catch (Exception e) {
            Log.warnf("QR code generation failed, returning raw URI: %s", e.getMessage());
            return Base64.getEncoder().encodeToString(content.getBytes());
        }
    }

    private String generateSimpleQrSvg(String content) {
        // Minimal SVG placeholder — production should use a proper QR library
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\">"
                + "<rect width=\"200\" height=\"200\" fill=\"white\"/>"
                + "<text x=\"10\" y=\"100\" font-family=\"monospace\" font-size=\"8\">"
                + escapeXml(content.substring(0, Math.min(content.length(), 40)))
                + "</text></svg>";
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ────────────────────────────────────────────────────────────────────
    //  Keycloak Admin API helpers
    // ────────────────────────────────────────────────────────────────────

    private Uni<String> getAdminToken() {
        if (adminToken != null) {
            return Uni.createFrom().item(adminToken);
        }

        return webClient.postAbs(adminUrl + "/realms/master/protocol/openid-connect/token")
                .addQueryParam("grant_type", "client_credentials")
                .addQueryParam("client_id", "admin-cli")
                .addQueryParam("username", adminUsername)
                .addQueryParam("password", adminPassword)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        adminToken = response.bodyAsJsonObject().getString("access_token");
                        return adminToken;
                    }
                    throw new RuntimeException("Failed to obtain Keycloak admin token: " + response.statusCode());
                });
    }

    private Uni<Boolean> isMfaEnabled(String userId, String token) {
        return getUserAttribute(userId, "mfa_enabled", token)
                .map("true"::equals)
                .onFailure()
                .recoverWithItem(false);
    }

    private Uni<String> getUserEmail(String userId, String token) {
        return webClient.getAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        return response.bodyAsJsonObject().getString("email", "unknown@wallet.local");
                    }
                    Log.warnf("Could not fetch user email for %s, status %d", userId, response.statusCode());
                    return "unknown@wallet.local";
                });
    }

    private Uni<String> getUserAttribute(String userId, String attributeName, String token) {
        return webClient.getAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject user = response.bodyAsJsonObject();
                        JsonObject attrs = user.getJsonObject("attributes", new JsonObject());
                        JsonArray arr = attrs.getJsonArray(attributeName);
                        if (arr != null && !arr.isEmpty()) {
                            return arr.getString(0);
                        }
                    }
                    throw new MfaNotEnabledException(userId);
                });
    }

    private String getUserAttributeValue(String userId, String attributeName, String token) {
        try {
            var response = webClient.getAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                    .putHeader("Authorization", "Bearer " + token)
                    .send()
                    .await().indefinitely();

            if (response.statusCode() == 200) {
                JsonObject attrs = response.bodyAsJsonObject().getJsonObject("attributes", new JsonObject());
                JsonArray arr = attrs.getJsonArray(attributeName);
                if (arr != null && !arr.isEmpty()) {
                    return arr.getString(0);
                }
            }
        } catch (Exception e) {
            Log.debugf("Attribute %s not found for user %s", attributeName, userId);
        }
        return null;
    }

    private void storeUserAttribute(String userId, String attributeName, String value, String token) {
        // Fetch current user
        webClient.getAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject user = response.bodyAsJsonObject();
                        JsonObject attrs = user.getJsonObject("attributes", new JsonObject());
                        attrs.put(attributeName, new JsonArray().add(value));
                        user.put("attributes", attrs);

                        // Update user
                        return webClient.putAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                                .putHeader("Authorization", "Bearer " + token)
                                .sendJsonObject(user);
                    }
                    Log.warnf("Could not fetch user %s to store attribute %s", userId, attributeName);
                    return Uni.createFrom().voidItem();
                })
                .subscribe()
                .with(
                        resp -> Log.debugf("Stored %s for user %s", attributeName, userId),
                        err -> Log.errorf(err, "Failed to store %s for user %s", attributeName, userId)
                );
    }

    private void storeUserAttributeJsonArray(String userId, String attributeName,
                                              List<String> values, String token) {
        storeUserAttribute(userId, attributeName, String.join(",", values), token);
    }

    private void removeUserAttribute(String userId, String attributeName, String token) {
        webClient.getAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject user = response.bodyAsJsonObject();
                        JsonObject attrs = user.getJsonObject("attributes", new JsonObject());
                        attrs.remove(attributeName);
                        user.put("attributes", attrs);

                        return webClient.putAbs(adminUrl + "/admin/realms/wallet/users/" + userId)
                                .putHeader("Authorization", "Bearer " + token)
                                .sendJsonObject(user);
                    }
                    return Uni.createFrom().nullItem();
                })
                .subscribe()
                .with(
                        resp -> Log.debugf("Removed %s for user %s", attributeName, userId),
                        err -> Log.errorf(err, "Failed to remove %s for user %s", attributeName, userId)
                );
    }

    private int getRecoveryCodesCount(String userId, String token) {
        try {
            String codes = getUserAttributeValue(userId, "mfa_recovery_codes", token);
            if (codes != null) {
                return codes.split(",").length;
            }
        } catch (Exception e) {
            Log.debugf("Could not count recovery codes for user %s", userId);
        }
        return 0;
    }
}
