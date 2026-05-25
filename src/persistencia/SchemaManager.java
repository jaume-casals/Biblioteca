package persistencia;

/**
 * Thin facade naming the schema-management concern of {@link ServerConect} — the actual
 * CREATE_TABLE + MIGRATIONS logic still lives there. Future refactors should migrate the
 * migration loop here without touching call sites.
 */
public final class SchemaManager {
    private SchemaManager() {}
    // Currently no operations exposed; ServerConect runs migrations on construction. This class
    // exists so callers can express intent ("schema manager") in tests and future code.
}
