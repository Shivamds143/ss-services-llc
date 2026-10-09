package com.financialcrm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialcrm.entity.UtilityBill;
import com.financialcrm.service.PhonePePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhonePePaymentServiceImpl implements PhonePePaymentService {

    private static final String PAY_ENDPOINT = "/pg/v1/pay";

    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.payment.phonepe.enabled:false}")
    private boolean phonePeEnabled;

    @Value("${app.payment.phonepe.base-url:https://api-preprod.phonepe.com/apis/pg-sandbox}")
    private String phonePeBaseUrl;

    @Value("${app.payment.phonepe.merchant-id:}")
    private String merchantId;

    @Value("${app.payment.phonepe.salt-key:}")
    private String saltKey;

    @Value("${app.payment.phonepe.salt-index:1}")
    private int saltIndex;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public boolean isConfigured() {
        return phonePeEnabled
                && merchantId != null && !merchantId.isBlank()
                && saltKey != null && !saltKey.isBlank();
    }

    @Override
    public PaymentInitResult initiateUtilityBillPayment(UtilityBill bill, Long userId) {
        ensureConfigured();

        try {
            String merchantTransactionId = buildMerchantTransactionId(bill.getId());
            String encodedPayload = Base64.getEncoder().encodeToString(buildPayPayload(bill, userId, merchantTransactionId)
                    .getBytes(StandardCharsets.UTF_8));

            Map<String, String> requestBody = Map.of("request", encodedPayload);
            String xVerify = sha256Hex(encodedPayload + PAY_ENDPOINT + saltKey) + "###" + saltIndex;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(phonePeBaseUrl + PAY_ENDPOINT))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("X-VERIFY", xVerify)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400 || !root.path("success").asBoolean(false)) {
                String message = root.path("message").asText("PhonePe payment request failed.");
                throw new IllegalStateException(message);
            }

            String redirectUrl = firstNonBlank(
                    root.path("data").path("instrumentResponse").path("redirectInfo").path("url").asText(null),
                    root.path("data").path("redirectInfo").path("url").asText(null)
            );

            if (redirectUrl == null || redirectUrl.isBlank()) {
                throw new IllegalStateException("PhonePe redirect URL was not returned.");
            }

            return new PaymentInitResult(redirectUrl, merchantTransactionId);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initiate PhonePe payment: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PaymentStatusResult checkPaymentStatus(String merchantTransactionId) {
        ensureConfigured();

        try {
            String statusPath = "/pg/v1/status/" + merchantId + "/" + merchantTransactionId;
            String xVerify = sha256Hex(statusPath + saltKey) + "###" + saltIndex;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(phonePeBaseUrl + statusPath))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("X-VERIFY", xVerify)
                    .header("X-MERCHANT-ID", merchantId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            JsonNode instrument = data.path("paymentInstrument");

            return new PaymentStatusResult(
                    root.path("success").asBoolean(false),
                    data.path("state").asText("UNKNOWN"),
                    data.path("responseCode").asText(root.path("code").asText("UNKNOWN")),
                    root.path("message").asText("Unknown payment status"),
                    instrument.path("type").asText("UNKNOWN"),
                    data.path("transactionId").asText(""),
                    firstNonBlank(
                            instrument.path("utr").asText(null),
                            instrument.path("pgTransactionId").asText(null),
                            instrument.path("bankTransactionId").asText(null)
                    )
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify PhonePe payment: " + ex.getMessage(), ex);
        }
    }

    private String buildPayPayload(UtilityBill bill, Long userId, String merchantTransactionId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchantId", merchantId);
        payload.put("merchantTransactionId", merchantTransactionId);
        payload.put("merchantUserId", "utility-bill-user-" + userId);
        payload.put("amount", toPaise(bill.getAmount()));
        payload.put("redirectUrl", buildRedirectUrl(bill.getId(), userId, merchantTransactionId));
        payload.put("redirectMode", "REDIRECT");
        payload.put("callbackUrl", buildCallbackUrl(bill.getId(), userId, merchantTransactionId));
        payload.put("paymentInstrument", Map.of("type", "PAY_PAGE"));
        return objectMapper.writeValueAsString(payload);
    }

    private String buildRedirectUrl(Long billId, Long userId, String merchantTransactionId) {
        return UriComponentsBuilder.fromHttpUrl(appBaseUrl)
                .path("/utility-bills/{billId}/phonepe/return")
                .queryParam("tx", merchantTransactionId)
                .queryParam("uid", userId)
                .buildAndExpand(billId)
                .toUriString();
    }

    private String buildCallbackUrl(Long billId, Long userId, String merchantTransactionId) {
        return UriComponentsBuilder.fromHttpUrl(appBaseUrl)
                .path("/utility-bills/{billId}/phonepe/callback")
                .queryParam("tx", merchantTransactionId)
                .queryParam("uid", userId)
                .buildAndExpand(billId)
                .toUriString();
    }

    private String buildMerchantTransactionId(Long billId) {
        return "UB" + billId + "-" + System.currentTimeMillis();
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValue();
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("PhonePe is not configured. Set merchant credentials in application properties.");
        }
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }
}
