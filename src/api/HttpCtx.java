package api;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;

public class HttpCtx {

    private final HttpExchange ex;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private byte[] cachedBodyBytes;
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

    public long pathParamLong(String key) throws Exception {
        String s = pathParams.getOrDefault(key, "");
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { throw new Exception("Invalid path param '" + key + "': " + s); }
    }

    public int pathParamInt(String key) throws Exception {
        String s = pathParams.getOrDefault(key, "");
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw new Exception("Invalid path param '" + key + "': " + s); }
    }

    public String queryParam(String key) { return queryParams.get(key); }

    public String queryParamOrNull(String key) {
        String v = queryParams.get(key);
        return (v != null && !v.isBlank()) ? v : null;
    }

    public Integer queryParamInt(String key) {
        String v = queryParamOrNull(key);
        if (v == null) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }

    public Double queryParamDbl(String key) {
        String v = queryParamOrNull(key);
        if (v == null) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    public Boolean queryParamBool(String key) {
        String v = queryParamOrNull(key);
        return v == null ? null : Boolean.parseBoolean(v);
    }

    public byte[] bodyBytes() {
        if (cachedBodyBytes != null) return cachedBodyBytes;
        try { cachedBodyBytes = ex.getRequestBody().readAllBytes(); }
        catch (IOException e) { cachedBodyBytes = new byte[0]; }
        return cachedBodyBytes;
    }

    public String body() {
        if (cachedBody != null) return cachedBody;
        cachedBody = new String(bodyBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return cachedBody;
    }

    public String header(String name) { return ex.getRequestHeaders().getFirst(name); }

    public HttpCtx status(int code) { this.status = code; return this; }

    public void json(Object obj) {
        this.contentType = "application/json; charset=utf-8";
        this.responseBytes = JsonMapper.gson().toJson(obj).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public void result(byte[] data) { this.responseBytes = data; }

    public HttpCtx contentType(String ct) { this.contentType = ct; return this; }

    public HttpCtx responseHeader(String name, String value) { responseHeaders.put(name, value); return this; }

    private Exception storedEx;
    void setException(Exception e) { this.storedEx = e; }
    public Exception getException() { return storedEx; }

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

    // Duplicate query keys: last value wins. Multi-value params (checkboxes) are not supported.
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
