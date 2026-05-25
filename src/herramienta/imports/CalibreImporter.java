package herramienta.imports;

import herramienta.BookImporter;
import herramienta.BookImporter.ImportResult;
import interficie.BibliotecaWriter;

import java.io.File;

public final class CalibreImporter {
    private CalibreImporter() {}
    public static ImportResult run(File metadataDb, String sqlite3, BibliotecaWriter cd) throws Exception {
        return BookImporter.importCalibre(metadataDb, sqlite3, cd);
    }
}
