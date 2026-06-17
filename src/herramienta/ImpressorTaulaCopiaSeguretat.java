package herramienta;

/**
 * Place-holder for per-table backup-printf builders. {@link BackupService#backupToSQL}
 * currently inlines a 26-arg printf for the llibre table; future refactor will move one
 * builder per table here (llibre, llista, llibre_llista, tag, llibre_tag, prestec, lectura,
 * autor, llibre_autor).
 *
 * <p>Today, callers should continue using {@code BackupService.backupToSQL}; this class is the
 * named seam where future per-table builders will live.
 */
public final class ImpressorTaulaCopiaSeguretat {
    private ImpressorTaulaCopiaSeguretat() {}
}
