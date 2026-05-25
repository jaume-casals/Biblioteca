package persistencia;

/** Either an {@link H2Config} (file/in-mem) or a {@link MariaDbConfig} (remote). */
public sealed interface DbProfile permits H2Config, MariaDbConfig {
    String dbType();
}
