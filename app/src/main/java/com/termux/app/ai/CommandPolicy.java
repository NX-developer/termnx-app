package com.termux.app.ai;

import java.util.regex.Pattern;

public class CommandPolicy {

    public enum ActionType {
        DELETE,
        EDIT,
        PACKAGE,
        OTHER
    }

    private static final Pattern DELETE_PATTERN = Pattern.compile(
        "(\\brm\\b|\\brmdir\\b|\\bunlink\\b|\\bshred\\b|--delete\\b|-delete\\b|\\bmkfs|\\bdd\\b|" +
            "\\bpkg\\s+(uninstall|remove)\\b|\\bapt(-get)?\\s+(remove|purge|autoremove)\\b|" +
            "\\bdpkg\\s+(-r|-P|--remove|--purge)\\b|\\bpip[0-9]?\\s+uninstall\\b)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "(\\bpkg\\s+(install|add|upgrade|update)\\b|\\bapt(-get)?\\s+(install|upgrade|update)\\b|" +
            "\\bpip[0-9]?\\s+install\\b|\\bnpm\\s+(install|i|add)\\b|\\bproot-distro\\s+install\\b)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern EDIT_PATTERN = Pattern.compile(
        "(\\bsed\\s+-i\\b|\\btee\\b|\\bnano\\b|\\bvi\\b|\\bvim\\b|\\bnvim\\b|\\bemacs\\b|" +
            "\\bcp\\b|\\bmv\\b|\\bchmod\\b|\\bchown\\b|\\bln\\b|\\btouch\\b|\\bmkdir\\b|" +
            "\\btruncate\\b|\\bpatch\\b|>>|(^|[^0-9&])>)",
        Pattern.CASE_INSENSITIVE);

    public static ActionType classify(String command) {
        if (command == null) return ActionType.OTHER;
        String c = command.trim();
        if (DELETE_PATTERN.matcher(c).find()) return ActionType.DELETE;
        if (PACKAGE_PATTERN.matcher(c).find()) return ActionType.PACKAGE;
        if (EDIT_PATTERN.matcher(c).find()) return ActionType.EDIT;
        return ActionType.OTHER;
    }

    public static boolean autoApprove(ActionType type, boolean fullAccess, boolean editMode) {
        if (fullAccess) return true;
        return type == ActionType.EDIT && editMode;
    }

    public static String label(ActionType type) {
        switch (type) {
            case DELETE: return "Dosya silme";
            case EDIT: return "Dosya düzenleme";
            case PACKAGE: return "Paket işlemi";
            default: return "Komut";
        }
    }

    public static int color(ActionType type) {
        switch (type) {
            case DELETE: return 0xFFFF7B72;
            case EDIT: return 0xFFE3B341;
            case PACKAGE: return 0xFF58A6FF;
            default: return 0xFF768390;
        }
    }
}
