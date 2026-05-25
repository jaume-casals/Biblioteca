package main;

/**
 * Web-mode entry point. Currently a thin delegating helper to {@link Ejecutable}; future
 * refactor will lift {@code startWeb} body here so {@code Ejecutable.main} only dispatches.
 */
public final class WebLauncher {
    private WebLauncher() {}
    public static void main(String[] args) throws Exception {
        Ejecutable.main(new String[]{"--web"});
    }
}
