package persistencia;

/**
 * Schema constants shared across DAOs and the backup path.
 */
public final class Schema {
    private Schema() {}

    /**
     * Correct DELETE order to satisfy foreign keys: child tables first
     * (those with FKs to others), parent tables last.  Shared between
     * {@link LlibreDaoCore#clearAllData()}, {@link herramienta.BackupService#backupToSQL}
     * and any bulk reset — the order must stay in sync or the FOREIGN KEY
     * constraints will fail in strict mode.
     */
    public static final String[] CLEAR_ORDER = {
        "lectura", "prestec", "llibre_llista", "llista",
        "llibre_autor", "llibre_tag", "tag", "autor", "llibre"
    };
}
