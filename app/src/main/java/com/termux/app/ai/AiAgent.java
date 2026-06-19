package com.termux.app.ai;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiAgent {

    public interface Listener {
        void onAssistantMessage(String text);

        void onCommandProposed(String command, CommandPolicy.ActionType type);

        void onCommandAutoRun(String command, CommandPolicy.ActionType type);

        void onCommandRunning(String command);

        void onCommandOutput(String command, int exitCode, String output);

        void onError(String message);

        void onBusyChanged(boolean busy);

        void onDone();
    }

    private static final int MAX_STEPS = 12;
    private static final int COMMAND_TIMEOUT_SECONDS = 120;
    private static final int OUTPUT_LIMIT_CHARS = 4000;

    private final Context context;
    private final TermnxAiPrefs prefs;
    private final Listener listener;
    private final List<OpenRouterClient.Message> messages = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final BlockingQueue<Boolean> decisionQueue = new ArrayBlockingQueue<>(1);

    public AiAgent(@NonNull Context context, @NonNull Listener listener, String terminalContext) {
        this.context = context.getApplicationContext();
        this.prefs = new TermnxAiPrefs(context);
        this.listener = listener;
        messages.add(new OpenRouterClient.Message("system", buildSystemPrompt()));
        if (terminalContext != null && !terminalContext.trim().isEmpty()) {
            String trimmed = terminalContext.length() > OUTPUT_LIMIT_CHARS
                ? terminalContext.substring(terminalContext.length() - OUTPUT_LIMIT_CHARS)
                : terminalContext;
            messages.add(new OpenRouterClient.Message("system",
                "Current terminal transcript (most recent at the bottom):\n" + trimmed));
        }
    }

    private String buildSystemPrompt() {
        return "You are Termnx AI, an assistant embedded inside Termnx, a Termux-based Android terminal. "
            + "You can run shell commands in the user's Termux environment to inspect, install, configure and test things. "
            + "The environment is Termux with the prefix /data/data/com.termux/files/usr and the package manager 'pkg' (apt). "
            + "To run a command, output a single line that starts with 'RUN:' followed by exactly one shell command, "
            + "and put nothing else on that line. Output at most one RUN command per reply. "
            + "After the command runs you will receive a user message with its output and exit code, then you continue. "
            + "Make installs non-interactive (use 'pkg install -y' or 'apt -y'). Avoid destructive commands unless explicitly asked. "
            + "When the task is finished, reply with a normal message (no RUN line) that explains the result. "
            + "Always reply to the user in Turkish unless they write in another language. Keep answers concise.";
    }

    public boolean isBusy() {
        return busy.get();
    }

    public void sendUserMessage(final String text) {
        if (busy.get()) {
            listener.onError("Asistan meşgul, lütfen bekleyin.");
            return;
        }
        String apiKey = prefs.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            listener.onError("OpenRouter API anahtarı ayarlı değil. Ayarlardan ekleyin.");
            return;
        }
        messages.add(new OpenRouterClient.Message("user", text));
        busy.set(true);
        listener.onBusyChanged(true);
        executor.execute(this::runLoop);
    }

    public void provideDecision(boolean approved) {
        decisionQueue.offer(approved);
    }

    private void runLoop() {
        OpenRouterClient client = new OpenRouterClient(prefs.getApiKey(), prefs.getModel(), prefs.getBaseUrl());
        try {
            for (int step = 0; step < MAX_STEPS; step++) {
                String assistant;
                try {
                    assistant = client.complete(messages);
                } catch (Exception e) {
                    listener.onError("Model hatası: " + e.getMessage());
                    return;
                }
                messages.add(new OpenRouterClient.Message("assistant", assistant));

                String command = parseRunCommand(assistant);
                String display = stripRunLines(assistant);
                if (!display.isEmpty()) {
                    listener.onAssistantMessage(display);
                }

                if (command == null) {
                    listener.onDone();
                    return;
                }

                boolean approved;
                CommandPolicy.ActionType type = CommandPolicy.classify(command);
                if (CommandPolicy.autoApprove(type, prefs.isFullAccess(), prefs.isEditMode())) {
                    listener.onCommandAutoRun(command, type);
                    approved = true;
                } else {
                    decisionQueue.clear();
                    listener.onCommandProposed(command, type);
                    try {
                        approved = decisionQueue.take();
                    } catch (InterruptedException e) {
                        listener.onError("Onay beklenirken kesildi.");
                        return;
                    }
                }

                if (!approved) {
                    messages.add(new OpenRouterClient.Message("user", "OUTPUT: (command skipped by user)"));
                    continue;
                }

                listener.onCommandRunning(command);
                CommandRunner.Result result = CommandRunner.run(context, command, COMMAND_TIMEOUT_SECONDS);
                String combined = result.stdout;
                if (result.stderr != null && !result.stderr.isEmpty()) {
                    combined = combined.isEmpty() ? result.stderr : combined + "\n" + result.stderr;
                }
                listener.onCommandOutput(command, result.exitCode, combined);

                String forModel = combined.length() > OUTPUT_LIMIT_CHARS
                    ? combined.substring(0, OUTPUT_LIMIT_CHARS) + "\n... (truncated)"
                    : combined;
                messages.add(new OpenRouterClient.Message("user",
                    "OUTPUT (exit=" + result.exitCode + "):\n" + forModel));
            }
            listener.onError("Maksimum adım sayısına ulaşıldı.");
        } finally {
            busy.set(false);
            listener.onBusyChanged(false);
        }
    }

    private String parseRunCommand(String assistant) {
        if (assistant == null) return null;
        for (String line : assistant.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("RUN:")) {
                String command = trimmed.substring(4).trim();
                if (!command.isEmpty()) {
                    return command;
                }
            }
        }
        return null;
    }

    private String stripRunLines(String assistant) {
        if (assistant == null) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : assistant.split("\n")) {
            if (!line.trim().startsWith("RUN:")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }
}
