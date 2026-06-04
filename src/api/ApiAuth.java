package api;

import herramienta.Config;

/**
 * Local API token (stored in {@code ~/.biblioteca/config.properties}).
 * Required on mutating requests to mitigate CSRF from other browser tabs/origins.
 */
public final class ApiAuth {

    public static final String HEADER = "X-Biblioteca-Token";

    private ApiAuth() {}

    /** GET /api/auth/token — no auth required; same-origin clients read this once. */
    public static void registerRoutes(HttpRouter app) {
        app.get("/api/auth/token", ctx -> ctx.json(java.util.Map.of("token", Config.getApiToken())));
    }

    public static void requireToken(HttpCtx ctx) {
        String expected = Config.getApiToken();
        String provided = ctx.header(HEADER);
        if (provided == null || provided.isBlank() || !expected.equals(provided.trim())) {
            throw new UnauthorizedException();
        }
    }

    public static boolean isMutating(String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    public static final class UnauthorizedException extends RuntimeException {
        public UnauthorizedException() { super("Unauthorized"); }
    }
}
