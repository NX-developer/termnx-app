package com.termux.app.ai;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.runner.app.AppShell;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

public class CommandRunner {

    public static class Result {
        public final String stdout;
        public final String stderr;
        public final int exitCode;
        public final boolean started;

        public Result(String stdout, String stderr, int exitCode, boolean started) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
            this.started = started;
        }
    }

    public static Result run(@NonNull Context context, @NonNull String command, int timeoutSeconds) {
        String wrapped = "export PATH=" + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + ":$PATH\n" +
            "timeout " + timeoutSeconds + " " + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash -lc " +
            shellQuote(command) + " 2>&1\n";

        ExecutionCommand executionCommand = new ExecutionCommand(-1,
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", null, wrapped,
            TermuxConstants.TERMUX_HOME_DIR_PATH, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        executionCommand.commandLabel = "Termnx AI Command";

        AppShell appShell = AppShell.execute(context, executionCommand, null,
            new TermuxShellEnvironment(), null, true);

        if (appShell == null) {
            String err = executionCommand.resultData.stderr.toString();
            return new Result("", err.isEmpty() ? "Failed to start command" : err, -1, false);
        }

        int exit = executionCommand.resultData.exitCode != null ? executionCommand.resultData.exitCode : -1;
        return new Result(
            executionCommand.resultData.stdout.toString(),
            executionCommand.resultData.stderr.toString(),
            exit,
            true);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
