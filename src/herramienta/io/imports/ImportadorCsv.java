package herramienta.io.imports;

import herramienta.ImportadorLlibres;
import herramienta.ImportadorLlibres.ResultatImportacio;
import persistencia.contract.EscritorBiblioteca;

import java.io.File;

public final class ImportadorCsv {
    private ImportadorCsv() {}
    public static ResultatImportacio run(File f, EscritorBiblioteca cd) { return ImportadorLlibres.importarCSV(f, cd); }
}
