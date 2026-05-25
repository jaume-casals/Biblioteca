package interficie;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import java.util.List;

/**
 * Read-only library view for export/backup — no mutation methods.
 * Implementations typically wrap {@link BibliotecaReader}.
 */
public interface ExportSnapshot {
    List<Llibre> books();
    List<Llista> shelves();
    List<Tag> tags();
    long dbSizeBytes();
}
