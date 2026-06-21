package com.termux.app.ai;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TermnxAiSettingsActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_CARD = 0xFF161B22;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;
    private static final int COLOR_FREE = 0xFF39D353;
    private static final int COLOR_PAID = 0xFFE3B341;

    private TermnxAiPrefs prefs;
    private EditText keyField;
    private EditText modelField;
    private EditText countField;
    private EditText urlField;
    private LinearLayout modelList;
    private TextView listStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxAiPrefs(this);
        setTitle("Termnx AI Settings");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(6));

        header.addView(label("API key"));
        keyField = field("Paste your OpenRouter API key", prefs.getApiKey());
        header.addView(keyField);

        header.addView(label("Model"));
        modelField = field("Tap a model below or type its id", prefs.getModel());
        header.addView(modelField);

        header.addView(label("How many models to show"));
        countField = field("50", "50");
        countField.setInputType(InputType.TYPE_CLASS_NUMBER);
        header.addView(countField);

        Button loadButton = new Button(this);
        loadButton.setText("Load models");
        loadButton.setOnClickListener(v -> loadModels());
        header.addView(loadButton);

        listStatus = new TextView(this);
        listStatus.setTextColor(COLOR_DIM);
        listStatus.setTextSize(12f);
        listStatus.setPadding(0, dp(8), 0, 0);
        listStatus.setText("Enter your key, set a count, then load the model list.");
        header.addView(listStatus);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        modelList = new LinearLayout(this);
        modelList.setOrientation(LinearLayout.VERTICAL);
        modelList.setPadding(dp(12), dp(2), dp(12), dp(8));
        scroll.addView(modelList);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(16), dp(4), dp(16), dp(12));

        footer.addView(label("API URL"));
        urlField = field("https://openrouter.ai/api/v1/chat/completions", prefs.getBaseUrl());
        footer.addView(urlField);

        Button saveButton = new Button(this);
        saveButton.setText("Save");
        saveButton.setOnClickListener(v -> save());
        footer.addView(saveButton);

        root.addView(header);
        root.addView(scroll);
        root.addView(footer);
        setContentView(root);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(COLOR_DIM);
        view.setTextSize(13f);
        view.setPadding(0, dp(10), 0, dp(2));
        return view;
    }

    private EditText field(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setHintTextColor(COLOR_DIM);
        edit.setTextColor(COLOR_TEXT);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        if (value != null && !value.isEmpty()) edit.setText(value);
        return edit;
    }

    private void save() {
        prefs.setApiKey(keyField.getText().toString().trim());
        prefs.setModel(modelField.getText().toString().trim());
        prefs.setBaseUrl(urlField.getText().toString().trim());
        Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int parseCount() {
        try {
            int count = Integer.parseInt(countField.getText().toString().trim());
            if (count < 1) return 1;
            if (count > 200) return 200;
            return count;
        } catch (Exception e) {
            return 50;
        }
    }

    private String modelsEndpoint() {
        String base = urlField.getText().toString().trim();
        if (base.endsWith("/chat/completions")) {
            return base.substring(0, base.length() - "/chat/completions".length()) + "/models";
        }
        return "https://openrouter.ai/api/v1/models";
    }

    private void loadModels() {
        final int count = parseCount();
        final String endpoint = modelsEndpoint();
        final String key = keyField.getText().toString().trim();
        listStatus.setText("Loading models...");
        modelList.removeAllViews();
        new Thread(() -> {
            try {
                List<String[]> models = fetchModels(endpoint, key);
                runOnUiThread(() -> showModels(models, count));
            } catch (Exception e) {
                runOnUiThread(() -> listStatus.setText("Could not load models: " + e.getMessage()));
            }
        }).start();
    }

    private List<String[]> fetchModels(String endpoint, String key) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        List<String[]> result = new ArrayList<>();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Termnx");
            if (!key.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + key);
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            JSONObject root = new JSONObject(builder.toString());
            JSONArray data = root.optJSONArray("data");
            if (data == null) return result;
            for (int i = 0; i < data.length(); i++) {
                JSONObject model = data.optJSONObject(i);
                if (model == null) continue;
                String id = model.optString("id", "");
                if (id.isEmpty()) continue;
                JSONObject pricing = model.optJSONObject("pricing");
                boolean free = false;
                if (pricing != null) {
                    String prompt = pricing.optString("prompt", "0");
                    String completion = pricing.optString("completion", "0");
                    free = isZero(prompt) && isZero(completion);
                }
                result.add(new String[]{id, free ? "free" : "paid"});
            }
        } finally {
            connection.disconnect();
        }
        return result;
    }

    private boolean isZero(String value) {
        if (value == null) return true;
        String trimmed = value.trim();
        return trimmed.equals("0") || trimmed.equals("0.0") || trimmed.equals("0.00") || trimmed.isEmpty();
    }

    private void showModels(List<String[]> models, int count) {
        modelList.removeAllViews();
        if (models.isEmpty()) {
            listStatus.setText("No models returned.");
            return;
        }
        int shown = Math.min(count, models.size());
        int freeCount = 0;
        for (String[] entry : models) {
            if ("free".equals(entry[1])) freeCount++;
        }
        listStatus.setText("Showing " + shown + " of " + models.size() + " models ("
            + freeCount + " free in total). Tap one to select it.");

        for (int i = 0; i < shown; i++) {
            final String[] entry = models.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(COLOR_CARD);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(4);
            row.setLayoutParams(rowParams);

            TextView id = new TextView(this);
            id.setText(entry[0]);
            id.setTextColor(COLOR_TEXT);
            id.setTextSize(13f);
            id.setTypeface(Typeface.MONOSPACE);
            id.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView chip = new TextView(this);
            boolean free = "free".equals(entry[1]);
            chip.setText(free ? "free" : "paid");
            chip.setTextColor(free ? COLOR_FREE : COLOR_PAID);
            chip.setTextSize(12f);
            chip.setPadding(dp(8), 0, 0, 0);

            row.addView(id);
            row.addView(chip);
            row.setOnClickListener(v -> {
                modelField.setText(entry[0]);
                Toast.makeText(this, "Selected " + entry[0], Toast.LENGTH_SHORT).show();
            });
            modelList.addView(row);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
