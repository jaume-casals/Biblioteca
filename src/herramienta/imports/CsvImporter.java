package herramienta.imports;

import herramienta.BookImporter;
import herramienta.BookImporter.ImportResult;
import interficie.BibliotecaWriter;

import java.io.File;

public final class CsvImporter {
    private CsvImporter() {}
    public static ImportResult run(File f, BibliotecaWriter cd) { return BookImporter.importCSV(f, cd); }
}
