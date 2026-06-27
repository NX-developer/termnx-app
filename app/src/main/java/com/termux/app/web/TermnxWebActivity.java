package com.termux.app.web;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class TermnxWebActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;
    private static final int COLOR_WARN = 0xFFE3B341;
    private static final int PORT = 8080;

    private TextView statusView;
    private Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Web Dashboard");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView warning = new TextView(this);
        warning.setText("Security: while the server is on, anyone on your network who has the URL and the "
            + "access code can run commands on this device. Turn it off when you are done.");
        warning.setTextColor(COLOR_WARN);
        warning.setTextSize(12f);
        warning.setPadding(0, 0, 0, dp(10));
        root.addView(warning);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        statusView = new TextView(this);
        statusView.setTextColor(COLOR_TEXT);
        statusView.setTextSize(14f);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scroll.addView(statusView);
        root.addView(scroll);

        toggleButton = new Button(this);
        toggleButton.setOnClickListener(v -> toggle());
        toggleButton.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(toggleButton);

        setContentView(root);
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (TermnxWebServer.isRunning()) {
            TermnxWebServer.stop();
            Toast.makeText(this, "Server stopped.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                TermnxWebServer.start(this, PORT);
                Toast.makeText(this, "Server started.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not start: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        refresh();
    }

    private void refresh() {
        if (TermnxWebServer.isRunning()) {
            StringBuilder text = new StringBuilder();
            text.append("Status: ON\n\n");
            text.append("Access code: ").append(TermnxWebServer.getToken()).append("\n\n");
            text.append("Open in a browser on the same network:\n\n");
            List<String> addresses = localAddresses();
            if (addresses.isEmpty()) {
                text.append("(no network address found)\n");
            } else {
                for (String address : addresses) {
                    text.append("http://").append(address).append(":").append(TermnxWebServer.getPort())
                        .append("/?token=").append(TermnxWebServer.getToken()).append("\n\n");
                }
            }
            statusView.setText(text.toString());
            toggleButton.setText("Stop server");
        } else {
            statusView.setText("Status: OFF\n\nStart the server to control this terminal from a browser "
                + "on the same Wi-Fi network. An access code is generated each time you start it.");
            toggleButton.setText("Start server");
        }
    }

    private List<String> localAddresses() {
        java.util.List<String> result = new java.util.ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    if (host != null && host.indexOf(':') < 0) {
                        result.add(host);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
