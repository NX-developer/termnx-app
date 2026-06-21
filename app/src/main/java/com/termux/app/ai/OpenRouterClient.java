package com.termux.app.ai;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenRouterClient {

    public static class Message {
        public final String role;
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenRouterClient(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public String complete(@NonNull List<Message> messages) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray messageArray = new JSONArray();
        for (Message message : messages) {
            JSONObject obj = new JSONObject();
            obj.put("role", message.role);
            obj.put("content", message.content);
            messageArray.put(obj);
        }
        body.put("messages", messageArray);
        body.put("temperature", 0.2);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(120000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("HTTP-Referer", "https://github.com/NX-developer/termnx-app");
            connection.setRequestProperty("X-Title", "Termnx");

            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = readStream(stream);

            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + ": " + extractError(response));
            }

            return extractContent(response);
        } finally {
            connection.disconnect();
        }
    }

    private String extractContent(String response) throws JSONException {
        JSONObject root = new JSONObject(response);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new RuntimeException("Empty response from model");
        }
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        if (message == null) {
            throw new RuntimeException("Malformed response from model");
        }
        Object content = message.opt("content");
        if (content instanceof JSONArray) {
            JSONArray blocks = (JSONArray) content;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.optJSONObject(i);
                if (block == null) continue;
                String text = block.optString("text", "");
                if (!text.isEmpty()) {
                    if (builder.length() > 0) builder.append("\n");
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        if (content == null || content == JSONObject.NULL) {
            return "";
        }
        return content.toString();
    }

    private String extractError(String response) {
        try {
            JSONObject root = new JSONObject(response);
            JSONObject error = root.optJSONObject("error");
            if (error != null) {
                return error.optString("message", response);
            }
        } catch (Exception ignored) {
        }
        return response;
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
