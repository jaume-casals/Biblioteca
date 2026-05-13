package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class HttpRouter {

    @FunctionalInterface
    public interface Handler {
        void handle(HttpCtx ctx) throws Exception;
    }

    private static class Route {
        final String method;
        final String[] parts;
        final Handler handler;

        Route(String method, String pattern, Handler handler) {
            this.method  = method;
            this.parts   = pattern.split("/", -1);
            this.handler = handler;
        }

        boolean matches(String method, String[] pathParts) {
            if (!this.method.equalsIgnoreCase(method)) return false;
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

    private final List<Route> routes = new ArrayList<>();
    private Handler exceptionHandler;
    private HttpServer server;

    public void get(String path, Handler h)    { routes.add(new Route("GET",    path, h)); }
    public void post(String path, Handler h)   { routes.add(new Route("POST",   path, h)); }
    public void put(String path, Handler h)    { routes.add(new Route("PUT",    path, h)); }
    public void delete(String path, Handler h) { routes.add(new Route("DELETE", path, h)); }

    public void exception(Handler h) { this.exceptionHandler = h; }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", this::dispatch);
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    public void stop() { if (server != null) server.stop(0); }

    private void dispatch(HttpExchange ex) {
        String rawPath = ex.getRequestURI().getPath();
        String method  = ex.getRequestMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            try {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                ex.close();
            } catch (IOException ignored) {}
            return;
        }

        // Try API routes first
        String[] pathParts = rawPath.split("/", -1);
        for (Route route : routes) {
            if (route.matches(method, pathParts)) {
                HttpCtx ctx = new HttpCtx(ex, route.extractParams(pathParts));
                try {
                    route.handler.handle(ctx);
                    ctx.commit();
                } catch (Exception e) {
                    System.err.println("[dispatch ERROR] " + method + " " + rawPath + ": " + e);
                    e.printStackTrace();
                    try {
                        if (exceptionHandler != null) {
                            HttpCtx errCtx = new HttpCtx(ex, Collections.emptyMap());
                            errCtx._setException(e);
                            errCtx.status(400);
                            exceptionHandler.handle(errCtx);
                            errCtx.commit();
                        } else {
                            ex.sendResponseHeaders(500, 0);
                            ex.close();
                        }
                    } catch (Exception ignored) {}
                }
                return;
            }
        }

        // Static file fallback — serve from classpath /web/
        try {
            serveStatic(ex, rawPath);
        } catch (IOException ignored) {}
    }

    private void serveStatic(HttpExchange ex, String rawPath) throws IOException {
        String resourcePath = rawPath.equals("/") ? "/web/index.html" : "/web" + rawPath;
        InputStream rawIn = HttpRouter.class.getResourceAsStream(resourcePath);
        if (rawIn == null) {
            // SPA fallback: serve index.html for unknown paths
            rawIn = HttpRouter.class.getResourceAsStream("/web/index.html");
        }
        if (rawIn == null) {
            ex.sendResponseHeaders(404, 0);
            ex.close();
            return;
        }
        byte[] data;
        try (InputStream in = rawIn) { data = in.readAllBytes(); }
        String ct = guessMime(resourcePath);
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.sendResponseHeaders(200, data.length);
        try (var os = ex.getResponseBody()) { os.write(data); }
    }

    private String guessMime(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
