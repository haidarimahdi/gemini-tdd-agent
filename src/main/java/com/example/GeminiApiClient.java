package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiApiClient {

    private final String apiKey;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_BACKOFF_MS = 1000;


    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public JSONObject buildRequestBody(JSONArray contents, JSONArray tools) {
        JSONObject request = new JSONObject()
                .put("contents", contents);

        if (tools != null) {
            request.put("tools", new JSONArray().put(new JSONObject().put("functionDeclarations", tools)));
        }
        return request;
    }

    public String callGeminiAPI(JSONArray contents, JSONArray tools) throws Exception {
        JSONObject requestBody = buildRequestBody(contents, tools);
        URL url = new URL(API_URL + "?key=" + apiKey);

        int attempts = 0;
        int backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempts++;

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                StringBuilder responseBuilder = new StringBuilder();
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    while (scanner.hasNextLine()) {
                        responseBuilder.append(scanner.nextLine());
                    }
                }

                return responseBuilder.toString();
            } else {
                // Handle all other response codes as errors
                String errorResponse = handleHttpError(connection);
                System.err.println("API call failed with code " + responseCode + ". Response: " + errorResponse);

                if ((responseCode == 429 || responseCode >= 500) && attempts < MAX_RETRIES) {
                    System.err.println("Attempt " + attempts + " failed. Retrying in " + backoffMs + " ms...");
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff
                } else {
                    throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url +
                            ". Response: " + errorResponse);
                }
            }
        }

    }

    private String handleHttpError(HttpURLConnection connection) {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "No error stream available.";
        }
        StringBuilder errorBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                errorBuilder.append(scanner.nextLine());
            }
        }
        return errorBuilder.toString();
    }

    public JSONObject parseResponse(String response) {
        JSONObject jsonResponse = new JSONObject(response);
        // Check if the API returned an error instead of candidates
        if (!jsonResponse.has("candidates")) {
            System.err.println("Error: API response did not contain 'candidates'. Full response:");
            System.err.println(jsonResponse.toString(2)); // Pretty-print the error

            // Return a "text" part to inform the agent of the error
            String errorMsg = "API Error: " + jsonResponse.optString("error", "Unknown error");
            return new JSONObject().put("text", errorMsg);
        }

        // Proceed as normal if candidates exist
        try {
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            return parts.getJSONObject(0); // Return the first part
        } catch (Exception e) {
            System.err.println("Error parsing successful response: " + e.getMessage());
            return new JSONObject().put("text", "Error parsing response: " + e.getMessage());
        }
    }

}
