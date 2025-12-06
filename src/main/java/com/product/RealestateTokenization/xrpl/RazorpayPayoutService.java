package com.product.RealestateTokenization.xrpl;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

@Component

public class RazorpayPayoutService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public JSONObject createRazorpayPayout(JSONObject payoutRequest) throws Exception {

        URL url = new URL("https://api.razorpay.com/v1/payouts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String auth = keyId + ":" + keySecret;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payoutRequest.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        BufferedReader br = (responseCode >= 200 && responseCode < 300)
                ? new BufferedReader(new InputStreamReader(conn.getInputStream()))
                : new BufferedReader(new InputStreamReader(conn.getErrorStream()));

        String response = br.lines().collect(Collectors.joining());
        return new JSONObject(response);
    }

    public JSONObject createFundAccount(JSONObject fundRequest) throws Exception {

        URL url = new URL("https://api.razorpay.com/v1/fund_accounts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String auth = keyId + ":" + keySecret;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(fundRequest.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        BufferedReader br = (responseCode >= 200 && responseCode < 300)
                ? new BufferedReader(new InputStreamReader(conn.getInputStream()))
                : new BufferedReader(new InputStreamReader(conn.getErrorStream()));

        String response = br.lines().collect(Collectors.joining());
        return new JSONObject(response);
    }
    public JSONObject createContact(JSONObject request) throws Exception {

        URL url = new URL("https://api.razorpay.com/v1/contacts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String auth = keyId + ":" + keySecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = br.lines().collect(Collectors.joining());

        return new JSONObject(response);
    }

}
