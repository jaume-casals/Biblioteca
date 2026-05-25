package persistencia;

/** H2-specific connection details. {@code profile} drives the file path under ~/.biblioteca/. */
public record H2Config(String profile, String testUrl) implements DbProfile {
    @Override public String dbType() { return "h2"; }
}
