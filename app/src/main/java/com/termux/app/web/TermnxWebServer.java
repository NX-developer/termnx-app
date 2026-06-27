package com.termux.app.web;

import android.content.Context;

import com.termux.app.ai.CommandRunner;
import com.termux.app.theme.TermnxThemePrefs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class TermnxWebServer {

    private static TermnxWebServer sInstance;

    private final Context appContext;
    private final int port;
    private final String token;
    private ServerSocket serverSocket;
    private Thread thread;
    private volatile boolean running;

    private TermnxWebServer(Context context, int port, String token) {
        this.appContext = context.getApplicationContext();
        this.port = port;
        this.token = token;
    }

    public static synchronized boolean isRunning() {
        return sInstance != null && sInstance.running;
    }

    public static synchronized String getToken() {
        return sInstance != null ? sInstance.token : null;
    }

    public static synchronized int getPort() {
        return sInstance != null ? sInstance.port : 0;
    }

    public static synchronized void start(Context context, int port) throws Exception {
        if (sInstance != null && sInstance.running) {
            return;
        }
        String token = generateToken();
        TermnxWebServer server = new TermnxWebServer(context, port, token);
        server.startInternal();
        sInstance = server;
    }

    public static synchronized void stop() {
        if (sInstance != null) {
            sInstance.stopInternal();
            sInstance = null;
        }
    }

    private static String generateToken() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private void startInternal() throws Exception {
        serverSocket = new ServerSocket(port);
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    handle(socket);
                } catch (Exception e) {
                    if (!running) break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void stopInternal() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {
        }
        if (thread != null) thread.interrupt();
    }

    private void handle(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null) {
                socket.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "GET";
            String fullPath = parts.length > 1 ? parts[1] : "/";

            int contentLength = 0;
            String header;
            while ((header = readLine(in)) != null && !header.isEmpty()) {
                int colon = header.indexOf(':');
                if (colon > 0) {
                    String key = header.substring(0, colon).trim().toLowerCase();
                    String value = header.substring(colon + 1).trim();
                    if (key.equals("content-length")) {
                        try {
                            contentLength = Integer.parseInt(value);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            String body = "";
            if (contentLength > 0) {
                byte[] buffer = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = in.read(buffer, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                body = new String(buffer, 0, read, StandardCharsets.UTF_8);
            }

            route(method, fullPath, body, out);
            socket.close();
        } catch (Exception ignored) {
            try {
                socket.close();
            } catch (Exception ignored2) {
            }
        }
    }

    private void route(String method, String fullPath, String body, OutputStream out) throws Exception {
        String path = fullPath;
        Map<String, String> query = new HashMap<>();
        int q = fullPath.indexOf('?');
        if (q >= 0) {
            path = fullPath.substring(0, q);
            parseForm(fullPath.substring(q + 1), query);
        }
        Map<String, String> form = new HashMap<>();
        parseForm(body, form);

        if (path.equals("/")) {
            writeResponse(out, "200 OK", "text/html; charset=utf-8", pageHtml());
            return;
        }
        if (path.equals("/api/run") && method.equals("POST")) {
            if (!authorized(form, query)) {
                writeResponse(out, "401 Unauthorized", "text/plain", "Invalid token");
                return;
            }
            String command = form.get("command");
            if (command == null || command.trim().isEmpty()) {
                writeResponse(out, "400 Bad Request", "text/plain", "No command");
                return;
            }
            String stdin = form.get("stdin");
            if (stdin != null && !stdin.isEmpty() && !stdin.endsWith("\n")) {
                stdin = stdin + "\n";
            }
            CommandRunner.Result result = CommandRunner.run(appContext, command, 60, stdin);
            String output = result.stdout != null ? result.stdout : "";
            if (output.length() > 100000) {
                output = output.substring(0, 100000) + "\n... (truncated)";
            }
            writeResponse(out, "200 OK", "text/plain; charset=utf-8", output);
            return;
        }
        if (path.equals("/api/theme") && method.equals("POST")) {
            if (!authorized(form, query)) {
                writeResponse(out, "401 Unauthorized", "text/plain", "Invalid token");
                return;
            }
            TermnxThemePrefs theme = new TermnxThemePrefs(appContext);
            String bg = form.get("background");
            String fg = form.get("foreground");
            if (bg != null && bg.matches("#?[0-9a-fA-F]{6}")) {
                theme.setTerminalBackground(parseColor(bg));
            }
            if (fg != null && fg.matches("#?[0-9a-fA-F]{6}")) {
                theme.setTerminalForeground(parseColor(fg));
            }
            try {
                theme.writeTerminalColorsFile();
            } catch (Exception ignored) {
            }
            writeResponse(out, "200 OK", "text/plain", "Saved. Return to the terminal to apply.");
            return;
        }
        writeResponse(out, "404 Not Found", "text/plain", "Not found");
    }

    private boolean authorized(Map<String, String> form, Map<String, String> query) {
        String provided = form.get("token");
        if (provided == null) provided = query.get("token");
        return provided != null && provided.equals(token);
    }

    private int parseColor(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return 0xFF000000 | Integer.parseInt(clean, 16);
    }

    private void parseForm(String data, Map<String, String> target) {
        if (data == null || data.isEmpty()) return;
        for (String pair : data.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                try {
                    String key = java.net.URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                    String value = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    target.put(key, value);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') buffer.write(c);
        }
        if (c == -1 && buffer.size() == 0) return null;
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private void writeResponse(OutputStream out, String status, String contentType, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.0 ").append(status).append("\r\n");
        head.append("Content-Type: ").append(contentType).append("\r\n");
        head.append("Content-Length: ").append(bytes.length).append("\r\n");
        head.append("Connection: close\r\n\r\n");
        out.write(head.toString().getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private String pageHtml() {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<title>Termnx Dashboard</title><style>"
            + "body{background:#0b0e14;color:#d7dee8;font-family:monospace;margin:0;padding:16px}"
            + "h2{color:#39d353}input,textarea,button{font-family:monospace;font-size:14px;width:100%;box-sizing:border-box;"
            + "background:#161b22;color:#d7dee8;border:1px solid #30363d;border-radius:6px;padding:10px;margin:6px 0}"
            + "button{background:#1f6feb;color:#fff;cursor:pointer}pre{background:#161b22;padding:10px;border-radius:6px;"
            + "white-space:pre-wrap;word-break:break-word;max-height:50vh;overflow:auto}.row{display:flex;gap:8px}"
            + ".row input{flex:1}.dim{color:#768390;font-size:12px}</style></head><body>"
            + "<h2>Termnx Dashboard</h2>"
            + "<p class='dim'>Anyone with this URL and the access code can run commands on this device. Stop the server in the app when done.</p>"
            + "<input id='token' placeholder='Access code'>"
            + "<h3>Run command</h3>"
            + "<input id='cmd' placeholder='e.g. python downloader.py' onkeydown='if(event.key==\"Enter\")run()'>"
            + "<textarea id='stdin' rows='3' placeholder='Input sent to the command (stdin), one answer per line. "
            + "e.g. the links the script asks for'></textarea>"
            + "<button onclick='run()'>Run</button><pre id='out'></pre>"
            + "<h3>Terminal colors</h3>"
            + "<div class='row'><input id='bg' placeholder='Background #101418'><input id='fg' placeholder='Text #d7dee8'></div>"
            + "<button onclick='theme()'>Apply colors</button>"
            + "<script>"
            + "var p=new URLSearchParams(location.search);if(p.get('token'))document.getElementById('token').value=p.get('token');"
            + "function enc(o){return Object.keys(o).map(k=>encodeURIComponent(k)+'='+encodeURIComponent(o[k])).join('&')}"
            + "function run(){var t=document.getElementById('token').value;var c=document.getElementById('cmd').value;"
            + "var s=document.getElementById('stdin').value;"
            + "document.getElementById('out').textContent='Running...';"
            + "fetch('/api/run',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:enc({token:t,command:c,stdin:s})})"
            + ".then(r=>r.text()).then(x=>{document.getElementById('out').textContent=x}).catch(e=>{document.getElementById('out').textContent=''+e})}"
            + "function theme(){var t=document.getElementById('token').value;var bg=document.getElementById('bg').value;var fg=document.getElementById('fg').value;"
            + "fetch('/api/theme',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:enc({token:t,background:bg,foreground:fg})})"
            + ".then(r=>r.text()).then(x=>{document.getElementById('out').textContent=x})}"
            + "</script></body></html>";
    }
}
