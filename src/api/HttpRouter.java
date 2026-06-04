package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class HttpRouter {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(HttpRouter.class.getName());

    @FunctionalInterface
    public interface Handler {
        void handle(HttpCtx ctx) throws Exception;
    }

    public static class Route {
        final String method;
        final String[] parts;
        final Handler handler;

        Route(String method, String pattern, Handler handler) {
            this.method  = method;
            this.parts   = pattern.split("/", -1);
            this.handler = handler;
        }

        boolean matches(String method, String[] pathParts) {
            if (!this.method.equals(method)) return false;
            return pathMatches(pathParts);
        }

        boolean pathMatches(String[] pathParts) {
            if (parts.length != pathParts.length) return false;
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].startsWith("{") && !parts[i].equals(pathParts[i])) return false;
            }
            return true;
        }

        Map<String, String> extractParams(String[] pathParts) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith("{")) {
                    m.put(parts[i].substring(1, parts[i].length() - 1), pathParts[i]);
                }
            }
            return m;
        }
    }

    private final List<Route> routes = new java.util.concurrent.CopyOnWriteArrayList<>();
    private Handler exceptionHandler;
    private HttpServer server;
    private int listenPort = 7070;

    public void get(String path, Handler h)    { routes.add(new Route("GET",    path, h)); }
    public void post(String path, Handler h)   { routes.add(new Route("POST",   path, h)); }
    public void put(String path, Handler h)    { routes.add(new Route("PUT",    path, h)); }
    public void patch(String path, Handler h)  { routes.add(new Route("PATCH",  path, h)); }
    public void delete(String path, Handler h) { routes.add(new Route("DELETE", path, h)); }

    public void exception(Handler h) { this.exceptionHandler = h; }

    public void start(int port) throws IOException {
        listenPort = port;
        server = HttpServer.create(new InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), port), 0);
        server.setExecutor(Executors.newFixedThreadPool(32, r -> {
            Thread t = new Thread(r, "api-http");
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/", this::dispatch);
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    public void stop() { if (server != null) server.stop(0); }

    public Route findRoute(String method, String path) {
        String[] pathParts = path.split("/", -1);
        for (Route route : routes) {
            if (route.matches(method.toUpperCase(java.util.Locale.ROOT), pathParts)) return route;
        }
        return null;
    }

    static boolean isAllowedOrigin(String origin, int port) {
        if (origin == null || origin.isBlank()) return false;
        return origin.equals("http://localhost:" + port) || origin.equals("http://127.0.0.1:" + port);
    }

    private void dispatch(HttpExchange ex) {
        String rawPath = ex.getRequestURI().getPath();
        String method  = ex.getRequestMethod();

        String origin = ex.getRequestHeaders().getFirst("Origin");
        boolean allowedOrigin = isAllowedOrigin(origin, listenPort);
        if ("OPTIONS".equalsIgnoreCase(method)) {
            try {
                if (allowedOrigin) {
                    ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                    ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
                    ex.getResponseHeaders().set("Access-Control-Allow-Headers",
                        "Content-Type," + ApiAuth.HEADER);
                }
                ex.sendResponseHeaders(204, -1);
                ex.close();
            } catch (IOException e) { LOG.warning("CORS preflight send failed: " + e); }
            return;
        }

        String upperMethod = method.toUpperCase(java.util.Locale.ROOT);
        String[] pathParts = rawPath.split("/", -1);
        Route matchedPath = null;
        for (Route route : routes) {
            if (!route.pathMatches(pathParts)) continue;
            if (route.matches(upperMethod, pathParts)) {
                HttpCtx ctx = new HttpCtx(ex, route.extractParams(pathParts));
                ctx.corsOrigin = allowedOrigin ? origin : null;
                try {
                    if (ApiAuth.isMutating(upperMethod)) ApiAuth.requireToken(ctx);
                    route.handler.handle(ctx);
                    ctx.commit();
                } catch (Exception e) {
                    handleRouteError(ex, allowedOrigin, origin, upperMethod, rawPath, e);
                }
                return;
            }
            if (route.parts.length == pathParts.length) {
                boolean pathMatches = true;
                for (int i = 0; i < route.parts.length; i++) {
                    if (!route.parts[i].startsWith("{") && !route.parts[i].equals(pathParts[i])) { pathMatches = false; break; }
                }
                if (pathMatches) matchedPath = route;
            }
        }

        if (rawPath.startsWith("/api/")) {
            if (matchedPath != null) {
                String allow = matchedPath.method + ",OPTIONS";
                ex.getResponseHeaders().set("Allow", allow);
                try { ex.sendResponseHeaders(405, 0); ex.close(); } catch (IOException ignored) {}
                return;
            }
            try { ex.sendResponseHeaders(404, 0); ex.close(); } catch (IOException ignored) {}
            return;
        }
        try {
            serveStatic(ex, rawPath, allowedOrigin ? origin : null);
        } catch (IOException e) {
            LOG.warning("Static file error for " + rawPath + ": " + e.getMessage());
            try {
                if (!ex.getResponseHeaders().containsKey("Content-Type")) {
                    ex.sendResponseHeaders(500, -1);
                    ex.close();
                }
            } catch (IOException ignored) {}
        }
    }

    private void handleRouteError(HttpExchange ex, boolean allowedOrigin, String origin,
            String method, String rawPath, Exception e) {
        LOG.warning("[dispatch] " + method + " " + rawPath + ": " + e);
        try {
            if (exceptionHandler != null) {
                HttpCtx errCtx = new HttpCtx(ex, Collections.emptyMap());
                if (errCtx.isCommitted()) return;
                errCtx.corsOrigin = allowedOrigin ? origin : null;
                errCtx.setException(e);
                exceptionHandler.handle(errCtx);
                if (!errCtx.isCommitted()) errCtx.commit();
            } else {
                ex.sendResponseHeaders(500, -1);
                ex.close();
            }
        } catch (Exception secondary) {
            LOG.fine("Error handler failed: " + secondary.getMessage());
            try { ex.close(); } catch (Exception ignored) {}
        }
    }

    private void serveStatic(HttpExchange ex, String rawPath, String corsOrigin) throws IOException {
        String resourcePath = resolveWebResource(rawPath);
        if (resourcePath == null) {
            ex.sendResponseHeaders(404, -1);
            ex.close();
            return;
        }
        InputStream rawIn = HttpRouter.class.getResourceAsStream(resourcePath);
        if (rawIn == null) {
            rawIn = HttpRouter.class.getResourceAsStream("/web/index.html");
            if (rawIn == null) {
                ex.sendResponseHeaders(404, -1);
                ex.close();
                return;
            }
            resourcePath = "/web/index.html";
        }
        byte[] data;
        try (InputStream in = rawIn) { data = in.readAllBytes(); }
        String ct = guessMime(resourcePath);
        ex.getResponseHeaders().set("Content-Type", ct);
        if (corsOrigin != null) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", corsOrigin);
        }
        ex.sendResponseHeaders(200, data.length);
        try (var os = ex.getResponseBody()) { os.write(data); }
        ex.close();
    }

    static String resolveWebResource(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return "/web/index.html";
        String path = rawPath.replace('\\', '/');
        if (path.contains("..") || path.contains("\0")) return null;
        if (!path.startsWith("/")) path = "/" + path;
        if (path.equals("/")) return "/web/index.html";
        String rel = path.substring(1);
        if (rel.isEmpty() || rel.startsWith("/")) return null;
        for (String seg : rel.split("/")) {
            if (seg.isEmpty() || ".".equals(seg) || "..".equals(seg)) return null;
        }
        return "/web/" + rel;
    }

    private static final Map<String, String> MIME_MAP = Map.of(
        ".html", "text/html; charset=utf-8",
        ".css",  "text/css; charset=utf-8",
        ".js",   "application/javascript; charset=utf-8",
        ".png",  "image/png",
        ".jpg",  "image/jpeg",
        ".jpeg", "image/jpeg",
        ".svg",  "image/svg+xml",
        ".ico",  "image/x-icon",
        ".woff2","font/woff2"
    );

    private static String guessMime(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot).toLowerCase(java.util.Locale.ROOT);
            String mime = MIME_MAP.get(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }
}