package herramienta.imports;

import herramienta.ImportadorLlibres;
import herramienta.ImportadorLlibres.ResultatImportacio;
import interficie.BibliotecaWriter;

import java.io.File;

public final class ImportadorCsv {
    private ImportadorCsv() {}
    public static ResultatImportacio run(File f, BibliotecaWriter cd) { return ImportadorLlibres.importarCSV(f, cd); }
}
