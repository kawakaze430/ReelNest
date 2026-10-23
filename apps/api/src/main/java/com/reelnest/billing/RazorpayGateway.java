package com.reelnest.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Restricted server-side Razorpay adapter; credentials never leave the backend. */
@Component
public class RazorpayGateway {
    private final String keyId, keySecret, baseUrl;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();

    public RazorpayGateway(@Value("${app.billing.razorpay.key-id:}") String keyId,
            @Value("${app.billing.razorpay.key-secret:}") String keySecret,
            @Value("${app.billing.razorpay.api-url:https://api.razorpay.com}") String baseUrl,
            ObjectMapper mapper) {
        this.keyId=keyId;this.keySecret=keySecret;this.baseUrl=baseUrl;this.mapper=mapper;
        if (!baseUrl.matches("https://[a-zA-Z0-9.-]+(:[0-9]+)?"))
            throw new IllegalArgumentException("Razorpay API URL must be HTTPS and contain no path");
    }

    public boolean configured() { return !keyId.isBlank() && !keySecret.isBlank(); }
    public String publicKey() { return keyId; }

    private JsonNode request(String method, String path, Object payload) {
        if (!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Payments are not configured. Set Razorpay test keys and plan IDs.");
        try {
            String auth=Base64.getEncoder().encodeToString((keyId+":"+keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(baseUrl+path))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization","Basic "+auth)
                .header("Accept","application/json");
            if (payload != null) builder.header("Content-Type","application/json");
            switch (method) {
                case "GET" -> builder.GET();
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
                default -> throw new IllegalArgumentException("Unsupported method");
            }
            HttpResponse<String> response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()<200 || response.statusCode()>=300) {
                // Never return provider bodies containing account or payment details to a browser.
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment provider rejected request (HTTP "+response.statusCode()+")");
            }
            return mapper.readTree(response.body());
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "Payment provider unavailable",ex); }
    }

    public JsonNode getPlan(String planId) {
        if (planId==null || !planId.matches("plan_[A-Za-z0-9]{8,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid plan identifier");
        return request("GET","/v1/plans/"+planId,null);
    }
    public JsonNode create(String planId, String userReference) {
        // No inline prices: Razorpay plan ID is configured by the operator.
        return request("POST","/v1/subscriptions",java.util.Map.of(
            "plan_id",planId,"total_count",12,"customer_notify",1,
            "notes",java.util.Map.of("reelnest_user_id",userReference)));
    }
    /** Fetch a payment after refund/dispute events; never rely on a client-reported amount. */
    public JsonNode getPayment(String paymentId) {
        if (paymentId==null || !paymentId.matches("pay_[A-Za-z0-9]{8,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid provider payment ID");
        return request("GET","/v1/payments/"+paymentId,null);
    }
    public JsonNode get(String subscriptionId) {
        requireProviderId(subscriptionId);
        return request("GET","/v1/subscriptions/"+subscriptionId,null);
    }
    public JsonNode cancelAtEnd(String subscriptionId) {
        requireProviderId(subscriptionId);
        return request("POST","/v1/subscriptions/"+subscriptionId+"/cancel",
            java.util.Map.of("cancel_at_cycle_end",1));
    }
    private static void requireProviderId(String id) {
        if (id==null || !id.matches("sub_[A-Za-z0-9]{8,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid provider subscription ID");
    }
}
