package api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class HttpCtx {

    private final HttpExchange ex;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private String cachedBody;
    private int status = 200;
    private byte[] responseBytes;
    private String contentType = "application/json; charset=utf-8";
    private final Map<String, String> responseHeaders = new LinkedHashMap<>();
    private boolean committed = false;

    HttpCtx(HttpExchange ex, Map<String, String> pathParams) {
        this.ex = ex;
        this.pathParams = pathParams;
        this.queryParams = parseQuery(ex.getRequestURI().getRawQuery());
    }

    public String method() { return ex.getRequestMethod(); }

    public String pathParam(String key) { return pathParams.getOrDefault(key, ""); }

    public String queryParam(String key) { return queryParams.get(key); }

    public String body() {
        if (cachedBody != null) return cachedBody;
        try {
            cachedBody = new String(ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) { cachedBody = ""; }
        return cachedBody;
    }

    public byte[] bodyBytes() {
        try { return ex.getRequestBody().readAllBytes(); } catch (IOException e) { return new byte[0]; }
    }

    public String header(String name) { return ex.getRequestHeaders().getFirst(name); }

    public HttpCtx status(int code) { this.status = code; return this; }

    public void json(Object obj) {
        this.contentType = "application/json; charset=utf-8";
        this.responseBytes = JsonMapper.gson().toJson(obj).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public void result(byte[] data) { this.responseBytes = data; }

    public HttpCtx contentType(String ct) { this.contentType = ct; return this; }

    public void responseHeader(String name, String value) { responseHeaders.put(name, value); }

    private Exception ex_stored;
    void _setException(Exception e) { this.ex_stored = e; }
    public Exception getException() { return ex_stored; }

    void commit() throws IOException {
        if (committed) return;
        committed = true;
        byte[] body = responseBytes != null ? responseBytes : new byte[0];
        ex.getResponseHeaders().set("Content-Type", contentType);
        responseHeaders.forEach((k, v) -> ex.getResponseHeaders().set(k, v));
        ex.sendResponseHeaders(status, body.length == 0 && status == 204 ? -1 : body.length);
        if (body.length > 0) {
            try (var os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return m;
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq < 0) { m.put(decode(part), ""); }
            else { m.put(decode(part.substring(0, eq)), decode(part.substring(eq + 1))); }
        }
        return m;
    }

    private static String decode(String s) {
        try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}
