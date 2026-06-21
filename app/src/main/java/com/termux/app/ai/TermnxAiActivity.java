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
        input.setHint("Ask something or give a task...");
        input.setHintTextColor(COLOR_DIM);
        input.setTextColor(COLOR_TEXT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMaxLines(4);
        input.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        sendButton = new Button(this);
        sendButton.setText("Send");
        sendButton.setOnClickListener(v -> onSend());

        inputRow.addView(input);
        inputRow.addView(sendButton);

        root.addView(scrollView);
        root.addView(inputRow);
        setContentView(root);

        String terminalContext = getIntent() != null ? getIntent().getStringExtra(EXTRA_TRANSCRIPT) : null;
        agent = new AiAgent(this, this, terminalContext);

        addBubble("Termnx AI is ready. Example: \"check if python is installed and test it works\".", COLOR_ASSISTANT, COLOR_TEXT, false);
        if (prefs.getApiKey().isEmpty()) {
            addBubble("Open Settings from the menu and add your OpenRouter API key first.", COLOR_ERROR, COLOR_TEXT, false);
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
        menu.add(Menu.NONE, 1, Menu.NONE, "Settings");
        fullAccessItem = menu.add(Menu.NONE, 2, Menu.NONE, "Full access");
        fullAccessItem.setCheckable(true);
        fullAccessItem.setChecked(prefs.isFullAccess());
        editModeItem = menu.add(Menu.NONE, 3, Menu.NONE, "Edit mode (Ctrl+A)");
        editModeItem.setCheckable(true);
        editModeItem.setChecked(prefs.isEditMode());
        menu.add(Menu.NONE, 4, Menu.NONE, "Check models");
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
                ? "Full access on: the AI runs every command without asking (including delete). Be careful."
                : "Full access off.", COLOR_OUTPUT, COLOR_DIM, true);
            return true;
        } else if (item.getItemId() == 3) {
            toggleEditMode();
            return true;
        } else if (item.getItemId() == 4) {
            checkModels();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleEditMode() {
        boolean newValue = !prefs.isEditMode();
        prefs.setEditMode(newValue);
        if (editModeItem != null) editModeItem.setChecked(newValue);
        addBubble(newValue
            ? "Edit mode on: file edit commands run without asking. Delete still asks for approval."
            : "Edit mode off: file edits also ask for approval.", COLOR_OUTPUT, COLOR_DIM, true);
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
        apiKeyField.setHint("OpenRouter API key");
        apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKeyField.setText(prefs.getApiKey());

        final EditText modelField = new EditText(this);
        modelField.setHint("Model");
        modelField.setText(prefs.getModel());

        final EditText baseUrlField = new EditText(this);
        baseUrlField.setHint("API URL");
        baseUrlField.setText(prefs.getBaseUrl());

        layout.addView(labelView("API key"));
        layout.addView(apiKeyField);
        layout.addView(labelView("Model"));
        layout.addView(modelField);
        layout.addView(labelView("API URL"));
        layout.addView(baseUrlField);

        new AlertDialog.Builder(this)
            .setTitle("Termnx AI Settings")
            .setView(layout)
            .setPositiveButton("Save", (dialog, which) -> {
                prefs.setApiKey(apiKeyField.getText().toString());
                prefs.setModel(modelField.getText().toString());
                prefs.setBaseUrl(baseUrlField.getText().toString());
                addBubble("Settings saved.", COLOR_OUTPUT, COLOR_DIM, true);
                checkModels();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkModels() {
        addBubble("Checking available models...", COLOR_OUTPUT, COLOR_DIM, true);
        new Thread(() -> {
            CommandRunner.Result probe = CommandRunner.run(this,
                "command -v python || command -v python3", 20);
            boolean hasPython = probe.started && probe.exitCode == 0
                && probe.stdout != null && probe.stdout.trim().length() > 0;
            if (hasPython) {
                runModelScript();
            } else {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Python required")
                    .setMessage("Checking which models are free needs Python. Install it now? "
                        + "(pkg install python)")
                    .setPositiveButton("Install", (d, w) -> installPythonThenCheck())
                    .setNegativeButton("Cancel", (d, w) -> addBubble(
                        "Skipped. You can install Python later with: pkg install python",
                        COLOR_OUTPUT, COLOR_DIM, true))
                    .show());
            }
        }).start();
    }

    private void installPythonThenCheck() {
        addBubble("Installing Python, this can take a minute...", COLOR_OUTPUT, COLOR_DIM, true);
        new Thread(() -> {
            CommandRunner.Result res = CommandRunner.run(this, "pkg install -y python", 600);
            runOnUiThread(() -> addBubble(res.exitCode == 0
                ? "Python installed."
                : "Python install finished with code " + res.exitCode + ".",
                COLOR_OUTPUT, COLOR_DIM, true));
            runModelScript();
        }).start();
    }

    private void runModelScript() {
        try {
            StringBuilder s = new StringBuilder();
            s.append("import json, urllib.request\n");
            s.append("try:\n");
            s.append("    req = urllib.request.Request(\"https://openrouter.ai/api/v1/models\", headers={\"User-Agent\": \"Termnx\"})\n");
            s.append("    data = json.load(urllib.request.urlopen(req, timeout=30))\n");
            s.append("    models = data.get(\"data\", [])\n");
            s.append("    free = []\n");
            s.append("    paid = 0\n");
            s.append("    for m in models:\n");
            s.append("        p = m.get(\"pricing\", {})\n");
            s.append("        pr = str(p.get(\"prompt\", \"0\"))\n");
            s.append("        co = str(p.get(\"completion\", \"0\"))\n");
            s.append("        if pr in (\"0\", \"0.0\") and co in (\"0\", \"0.0\"):\n");
            s.append("            free.append(m.get(\"id\"))\n");
            s.append("        else:\n");
            s.append("            paid += 1\n");
            s.append("    print(\"Total models:\", len(models))\n");
            s.append("    print(\"Free models:\", len(free))\n");
            s.append("    print(\"Paid models:\", paid)\n");
            s.append("    print(\"\")\n");
            s.append("    print(\"Free models (first 40):\")\n");
            s.append("    for f in free[:40]:\n");
            s.append("        print(\" - \" + str(f))\n");
            s.append("except Exception as e:\n");
            s.append("    print(\"Check failed: \" + str(e))\n");

            java.io.File file = new java.io.File(
                com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH, ".termnx_model_check.py");
            try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
                out.write(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
            }

            String path = file.getAbsolutePath();
            CommandRunner.Result res = CommandRunner.run(this,
                "python " + path + " || python3 " + path, 90);
            String output = res.stdout != null ? res.stdout.trim() : "";
            if (output.isEmpty()) output = "No output (exit " + res.exitCode + ").";
            final String shown = output;
            runOnUiThread(() -> addBubble(shown, COLOR_OUTPUT, COLOR_DIM, true));
        } catch (Exception e) {
            runOnUiThread(() -> addBubble("Model check failed: " + e.getMessage(),
                COLOR_OUTPUT, COLOR_DIM, true));
        }
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
        label.setText("awaiting approval:");
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
        run.setText("Run");
        final Button skip = new Button(this);
        skip.setText("Skip");

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
        runOnUiThread(() -> addBubble("Running: " + command, COLOR_OUTPUT, COLOR_DIM, true));
    }

    @Override
    public void onCommandOutput(String command, int exitCode, String output) {
        runOnUiThread(() -> {
            String shown = output == null ? "" : output;
            if (shown.length() > 4000) {
                shown = shown.substring(0, 4000) + "\n... (truncated)";
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
            sendButton.setText(busy ? "..." : "Send");
        });
    }

    @Override
    public void onDone() {
        // No-op: final assistant message already shown.
    }
}
