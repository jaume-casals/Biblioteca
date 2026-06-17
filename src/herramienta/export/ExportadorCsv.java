package herramienta.export;

import domini.Llibre;
import herramienta.ExportadorLlibres;
import interficie.ShelfReader;

import java.io.File;
import java.util.List;

/** Delegating thin entry point; CSV export logic lives in {@link BookExporter#exportCSV}. */
public final class ExportadorCsv {
    private ExportadorCsv() {}
    public static void export(File f, List<Llibre> view, ShelfReader cd) throws Exception {
        ExportadorLlibres.exportarCSV(f, view, cd);
    }
}
