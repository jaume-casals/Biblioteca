package herramienta.io.export;

import herramienta.ExportadorLlibres;
import persistencia.contract.LectorBiblioteca;

import java.io.File;

public final class JsonExporter {
    private JsonExporter() {}
    public static void export(File f, LectorBiblioteca cd) throws Exception {
        ExportadorLlibres.exportarJSON(f, cd);
    }
}
