package com.termux.app.ai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TermnxAiActivity extends AppCompatActivity implements AiAgent.Listener {

    public static final String EXTRA_TRANSCRIPT = "termnx_transcript";

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_USER = 0xFF1F6FEB;
    private static final int COLOR_ASSISTANT = 0xFF161B22;
    private static final int COLOR_COMMAND = 0xFF11281A;
    private static final int COLOR_OUTPUT = 0xFF0D1117;
    private static final int COLOR_ERROR = 0xFF3D1418;
    private static final int COLOR_GREEN = 0xFF39D353;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private TermnxAiPrefs prefs;
    private AiAgent agent;
    private LinearLayout transcript;
    private ScrollView scrollView;
    private EditText input;
    private Button sendButton;
    private MenuItem fullAccessItem;
    private MenuItem editModeItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxAiPrefs(this);

        setTitle("Termnx AI");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);

        transcript = new LinearLayout(this);
        transcript.setOrientation(LinearLayout.VERTICAL);
        transcript.setPadding(dp(10), dp(10), dp(10), dp(10));
        transcript.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.addView(transcript);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(dp(8), dp(8), dp(8), dp(8));
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        input = new EditText(this);
        input.setHint("Bir şey sor veya bir görev ver...");
        input.setHintTextColor(COLOR_DIM);
        input.setTextColor(COLOR_TEXT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMaxLines(4);
        input.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        sendButton = new Button(this);
        sendButton.setText("Gönder");
        sendButton.setOnClickListener(v -> onSend());

        inputRow.addView(input);
        inputRow.addView(sendButton);

        root.addView(scrollView);
        root.addView(inputRow);
        setContentView(root);

        String terminalContext = getIntent() != null ? getIntent().getStringExtra(EXTRA_TRANSCRIPT) : null;
        agent = new AiAgent(this, this, terminalContext);

        addBubble("Termnx AI hazır. Örnek: \"python kurulu mu, çalışıyor mu test et\".", COLOR_ASSISTANT, COLOR_TEXT, false);
        if (prefs.getApiKey().isEmpty()) {
            addBubble("Önce menüden Ayarlar'a girip OpenRouter API anahtarını ekle.", COLOR_ERROR, COLOR_TEXT, false);
        }
    }

    private void onSend() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        addBubble(text, COLOR_USER, Color.WHITE, false);
        agent.sendUserMessage(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Ayarlar");
        fullAccessItem = menu.add(Menu.NONE, 2, Menu.NONE, "Tüm izinler");
        fullAccessItem.setCheckable(true);
        fullAccessItem.setChecked(prefs.isFullAccess());
        editModeItem = menu.add(Menu.NONE, 3, Menu.NONE, "Değiştirme modu (Ctrl+A)");
        editModeItem.setCheckable(true);
        editModeItem.setChecked(prefs.isEditMode());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            showSettingsDialog();
            return true;
        } else if (item.getItemId() == 2) {
            boolean newValue = !item.isChecked();
            item.setChecked(newValue);
            prefs.setFullAccess(newValue);
            addBubble(newValue
                ? "Tüm izinler açık: AI hiçbir komut için onay istemez (silme dahil). Dikkatli ol."
                : "Tüm izinler kapalı.", COLOR_OUTPUT, COLOR_DIM, true);
            return true;
        } else if (item.getItemId() == 3) {
            toggleEditMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleEditMode() {
        boolean newValue = !prefs.isEditMode();
        prefs.setEditMode(newValue);
        if (editModeItem != null) editModeItem.setChecked(newValue);
        addBubble(newValue
            ? "Değiştirme modu açık: dosya düzenleme komutları sormadan çalışır. Silme yine onay ister."
            : "Değiştirme modu kapalı: dosya düzenleme için de onay istenir.", COLOR_OUTPUT, COLOR_DIM, true);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_A && event.isCtrlPressed()) {
            toggleEditMode();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText apiKeyField = new EditText(this);
        apiKeyField.setHint("OpenRouter API anahtarı");
        apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKeyField.setText(prefs.getApiKey());

        final EditText modelField = new EditText(this);
        modelField.setHint("Model");
        modelField.setText(prefs.getModel());

        final EditText baseUrlField = new EditText(this);
        baseUrlField.setHint("API URL");
        baseUrlField.setText(prefs.getBaseUrl());

        layout.addView(labelView("API anahtarı"));
        layout.addView(apiKeyField);
        layout.addView(labelView("Model"));
        layout.addView(modelField);
        layout.addView(labelView("API URL"));
        layout.addView(baseUrlField);

        new AlertDialog.Builder(this)
            .setTitle("Termnx AI Ayarları")
            .setView(layout)
            .setPositiveButton("Kaydet", (dialog, which) -> {
                prefs.setApiKey(apiKeyField.getText().toString());
                prefs.setModel(modelField.getText().toString());
                prefs.setBaseUrl(baseUrlField.getText().toString());
                addBubble("Ayarlar kaydedildi.", COLOR_OUTPUT, COLOR_DIM, true);
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private TextView labelView(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(COLOR_DIM);
        label.setPadding(0, dp(8), 0, dp(2));
        return label;
    }

    private void addBubble(String text, int bgColor, int textColor, boolean monospace) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(textColor);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackgroundColor(bgColor);
        if (monospace) {
            view.setTypeface(Typeface.MONOSPACE);
            view.setTextSize(12f);
        } else {
            view.setTextSize(14f);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        params.bottomMargin = dp(4);
        view.setLayoutParams(params);
        transcript.addView(view);
        scrollToBottom();
    }

    private void addCommandCard(final String command, final CommandPolicy.ActionType type) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(COLOR_COMMAND);
        card.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(4);
        cardParams.bottomMargin = dp(4);
        card.setLayoutParams(cardParams);

        TextView chip = new TextView(this);
        chip.setText(CommandPolicy.label(type));
        chip.setTextColor(CommandPolicy.color(type));
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextSize(11f);

        TextView label = new TextView(this);
        label.setText("onay bekliyor:");
        label.setTextColor(COLOR_DIM);
        label.setTextSize(11f);

        TextView cmd = new TextView(this);
        cmd.setText(command);
        cmd.setTextColor(COLOR_GREEN);
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setTextSize(13f);
        cmd.setPadding(0, dp(4), 0, dp(8));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        final Button run = new Button(this);
        run.setText("Çalıştır");
        final Button skip = new Button(this);
        skip.setText("Atla");

        run.setOnClickListener(v -> {
            run.setEnabled(false);
            skip.setEnabled(false);
            agent.provideDecision(true);
        });
        skip.setOnClickListener(v -> {
            run.setEnabled(false);
            skip.setEnabled(false);
            agent.provideDecision(false);
        });

        buttons.addView(run);
        buttons.addView(skip);

        card.addView(chip);
        card.addView(label);
        card.addView(cmd);
        card.addView(buttons);
        transcript.addView(card);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    public void onAssistantMessage(String text) {
        runOnUiThread(() -> addBubble(text, COLOR_ASSISTANT, COLOR_TEXT, false));
    }

    @Override
    public void onCommandProposed(String command, CommandPolicy.ActionType type) {
        runOnUiThread(() -> addCommandCard(command, type));
    }

    @Override
    public void onCommandAutoRun(String command, CommandPolicy.ActionType type) {
        runOnUiThread(() -> addBubble("[" + CommandPolicy.label(type) + "] $ " + command, COLOR_COMMAND, COLOR_GREEN, true));
    }

    @Override
    public void onCommandRunning(String command) {
        runOnUiThread(() -> addBubble("Çalışıyor: " + command, COLOR_OUTPUT, COLOR_DIM, true));
    }

    @Override
    public void onCommandOutput(String command, int exitCode, String output) {
        runOnUiThread(() -> {
            String shown = output == null ? "" : output;
            if (shown.length() > 4000) {
                shown = shown.substring(0, 4000) + "\n... (kısaltıldı)";
            }
            addBubble("exit=" + exitCode + "\n" + shown, COLOR_OUTPUT, COLOR_TEXT, true);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> addBubble(message, COLOR_ERROR, COLOR_TEXT, false));
    }

    @Override
    public void onBusyChanged(boolean busy) {
        runOnUiThread(() -> {
            sendButton.setEnabled(!busy);
            sendButton.setText(busy ? "..." : "Gönder");
        });
    }

    @Override
    public void onDone() {
        // No-op: final assistant message already shown.
    }
}
