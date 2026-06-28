package com.termux.app.github;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.shared.termux.TermuxConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TermnxGitHubActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private static final String PREFS_NAME = "termnx_github";
    private static final String BEGIN = "# >>> termnx-github";
    private static final String END = "# <<< termnx-github";

    private SharedPreferences prefs;
    private EditText userField;
    private EditText emailField;
    private EditText tokenField;
    private android.widget.CheckBox envCheck;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        setTitle("GitHub");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(content);

        TextView info = new TextView(this);
        info.setText("Connect your GitHub account with a Personal Access Token. The token is stored only "
            + "in a private file (owner-only, chmod 600); git and your shell read it at runtime, so the raw "
            + "token is never written into .bashrc or .git-credentials. This lets git and tools like Claude "
            + "Code, opencode and codex use your account without exposing the token. If a token was already "
            + "flagged as exposed, create a new one at github.com/settings/tokens and connect with that.");
        info.setTextColor(COLOR_DIM);
        info.setTextSize(12f);
        info.setPadding(0, 0, 0, dp(8));
        content.addView(info);

        statusView = new TextView(this);
        statusView.setTextColor(COLOR_TEXT);
        statusView.setTextSize(14f);
        statusView.setPadding(0, dp(4), 0, dp(8));
        content.addView(statusView);

        content.addView(label("Sign in with GitHub CLI (gh)"));
        TextView ghHint = new TextView(this);
        ghHint.setText("Recommended. Installs gh if needed and starts an interactive browser-based login "
            + "in the terminal, then sets git to use it. No token to paste. Follow the one-time code and URL "
            + "shown in the terminal.");
        ghHint.setTextColor(COLOR_DIM);
        ghHint.setTextSize(11f);
        ghHint.setPadding(0, 0, 0, dp(4));
        content.addView(ghHint);

        Button ghButton = new Button(this);
        ghButton.setText("Sign in with gh");
        ghButton.setOnClickListener(v -> startGhLogin());
        ghButton.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(ghButton);

        TextView divider = new TextView(this);
        divider.setText("Or connect with a Personal Access Token");
        divider.setTextColor(COLOR_DIM);
        divider.setTextSize(13f);
        divider.setPadding(0, dp(16), 0, dp(2));
        content.addView(divider);

        content.addView(label("GitHub username"));
        userField = field("e.g. NX-developer", prefs.getString("user", ""));
        content.addView(userField);

        content.addView(label("Email (optional)"));
        emailField = field("name@example.com", prefs.getString("email", ""));
        content.addView(emailField);

        content.addView(label("Personal Access Token"));
        tokenField = field("ghp_...", "");
        tokenField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(tokenField);

        envCheck = new android.widget.CheckBox(this);
        envCheck.setText("Also set GITHUB_TOKEN environment variable");
        envCheck.setTextColor(COLOR_TEXT);
        envCheck.setChecked(prefs.getBoolean("env", false));
        content.addView(envCheck);

        TextView envHint = new TextView(this);
        envHint.setText("Off by default. Git authenticates through a credential helper without exposing the "
            + "raw token, so AI tools that scan the environment will not see it. Turn this on only if a tool "
            + "(like gh) needs the GITHUB_TOKEN variable.");
        envHint.setTextColor(COLOR_DIM);
        envHint.setTextSize(11f);
        envHint.setPadding(0, 0, 0, dp(4));
        content.addView(envHint);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);

        Button connect = new Button(this);
        connect.setText("Connect");
        connect.setOnClickListener(v -> connect());
        connect.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button disconnect = new Button(this);
        disconnect.setText("Disconnect");
        disconnect.setOnClickListener(v -> disconnect());
        disconnect.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        actions.addView(connect);
        actions.addView(disconnect);
        content.addView(actions);

        root.addView(scroll);
        setContentView(root);
        refreshStatus();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startGhLogin() {
        String command = "pkg install -y gh && gh auth login && gh auth setup-git";
        Intent intent = new Intent(this, com.termux.app.TermuxActivity.class);
        intent.putExtra(com.termux.app.quick.TermnxWidgetProvider.EXTRA_RUN_COMMAND, command);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        Toast.makeText(this, "Starting gh login in the terminal. Follow the prompts there.",
            Toast.LENGTH_LONG).show();
    }

    private void refreshStatus() {
        boolean connected = prefs.getBoolean("connected", false);
        if (connected) {
            statusView.setText("Connected as " + prefs.getString("user", "") + ".");
        } else {
            statusView.setText("Not connected.");
        }
    }

    private void connect() {
        String user = userField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String token = tokenField.getText().toString().trim();
        if (user.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Username and token are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
            if (!home.exists()) home.mkdirs();
            File dir = new File(home, ".termnx");
            if (!dir.exists()) dir.mkdirs();
            chmod(dir, 0700);

            File tokenFile = new File(dir, ".github_token");
            writeFile(tokenFile, token + "\n");
            chmod(tokenFile, 0600);

            String shPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
            File helper = new File(dir, "git-credential-termnx");
            StringBuilder script = new StringBuilder();
            script.append("#!").append(shPath).append("\n");
            script.append("if [ \"$1\" = \"get\" ]; then\n");
            script.append("  echo username=").append(user).append("\n");
            script.append("  echo password=$(cat \"").append(tokenFile.getAbsolutePath()).append("\" 2>/dev/null)\n");
            script.append("fi\n");
            writeFile(helper, script.toString());
            chmod(helper, 0700);

            File credentials = new File(home, ".git-credentials");
            if (credentials.exists()) {
                String filtered = filterOutGithub(readFileSafe(credentials));
                if (filtered.trim().isEmpty()) {
                    credentials.delete();
                } else {
                    writeFile(credentials, filtered);
                }
            }

            StringBuilder gitConfig = new StringBuilder();
            gitConfig.append("[credential \"https://github.com\"]\n\thelper = ")
                .append(helper.getAbsolutePath()).append("\n");
            gitConfig.append("[user]\n\tname = ").append(user).append("\n");
            if (!email.isEmpty()) {
                gitConfig.append("\temail = ").append(email).append("\n");
            }
            writeManagedBlock(new File(home, ".gitconfig"), gitConfig.toString());

            boolean exposeEnv = envCheck.isChecked();
            if (exposeEnv) {
                String tokenPath = tokenFile.getAbsolutePath();
                StringBuilder bashrc = new StringBuilder();
                bashrc.append("if [ -f \"").append(tokenPath).append("\" ]; then\n");
                bashrc.append("  export GITHUB_TOKEN=\"$(cat \"").append(tokenPath).append("\")\"\n");
                bashrc.append("  export GH_TOKEN=\"$GITHUB_TOKEN\"\n");
                bashrc.append("fi\n");
                writeManagedBlock(new File(home, ".bashrc"), bashrc.toString());
            } else {
                removeManagedBlock(new File(home, ".bashrc"));
            }

            prefs.edit()
                .putString("user", user)
                .putString("email", email)
                .putBoolean("connected", true)
                .putBoolean("env", exposeEnv)
                .apply();

            tokenField.setText("");
            refreshStatus();
            Toast.makeText(this, "Connected. Token stored privately. Open a new session or run: source ~/.bashrc",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void chmod(File file, int mode) {
        try {
            android.system.Os.chmod(file.getAbsolutePath(), mode);
        } catch (Exception ignored) {
        }
    }

    private void disconnect() {
        try {
            File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
            File dir = new File(home, ".termnx");
            new File(dir, ".github_token").delete();
            new File(dir, "git-credential-termnx").delete();

            File credentials = new File(home, ".git-credentials");
            if (credentials.exists()) {
                String filtered = filterOutGithub(readFileSafe(credentials));
                if (filtered.trim().isEmpty()) {
                    credentials.delete();
                } else {
                    writeFile(credentials, filtered);
                }
            }
            removeManagedBlock(new File(home, ".gitconfig"));
            removeManagedBlock(new File(home, ".bashrc"));

            prefs.edit().putBoolean("connected", false).apply();
            refreshStatus();
            Toast.makeText(this, "Disconnected. Open a new session to apply.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String filterOutGithub(String content) {
        if (content.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : content.split("\n")) {
            if (!line.contains("@github.com") && !line.trim().isEmpty()) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }

    private void writeManagedBlock(File file, String blockBody) throws Exception {
        String existing = readFileSafe(file);
        String cleaned = stripManagedBlock(existing);
        StringBuilder result = new StringBuilder(cleaned);
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append("\n");
        }
        result.append(BEGIN).append("\n").append(blockBody);
        if (blockBody.length() > 0 && blockBody.charAt(blockBody.length() - 1) != '\n') {
            result.append("\n");
        }
        result.append(END).append("\n");
        writeFile(file, result.toString());
    }

    private void removeManagedBlock(File file) throws Exception {
        if (!file.exists()) return;
        String cleaned = stripManagedBlock(readFileSafe(file));
        writeFile(file, cleaned);
    }

    private String stripManagedBlock(String content) {
        if (content.isEmpty()) return "";
        int begin = content.indexOf(BEGIN);
        if (begin < 0) return content;
        int end = content.indexOf(END, begin);
        if (end < 0) {
            return content.substring(0, begin);
        }
        String before = content.substring(0, begin);
        String after = content.substring(end + END.length());
        if (after.startsWith("\n")) after = after.substring(1);
        return before + after;
    }

    private String readFileSafe(File file) {
        if (!file.exists()) return "";
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(file)) {
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void writeFile(File file, String content) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
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

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
