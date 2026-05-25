package herramienta.imports;

import herramienta.BookImporter;
import herramienta.BookImporter.ImportResult;
import interficie.BibliotecaWriter;

import java.io.File;

public final class JsonImporter {
    private JsonImporter() {}
    public static ImportResult run(File f, BibliotecaWriter cd) throws Exception { return BookImporter.importJSON(f, cd); }
}
