package herramienta.export;

import herramienta.ExportadorLlibres;
import interficie.LectorBiblioteca;

import java.io.File;

public final class JsonExporter {
    private JsonExporter() {}
    public static void export(File f, LectorBiblioteca cd) throws Exception {
        ExportadorLlibres.exportarJSON(f, cd);
    }
}
