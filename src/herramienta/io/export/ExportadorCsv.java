package herramienta.io.export;

import domini.Llibre;
import herramienta.ExportadorLlibres;
import persistencia.contract.LectorPrestatgeria;

import java.io.File;
import java.util.List;

/** Punt d'entrada prim que delega; la lògica d'exportació CSV viu a {@link ExportadorLlibres#exportarCSV}. */
public final class ExportadorCsv {
    private ExportadorCsv() {}
    public static void export(File f, List<Llibre> view, LectorPrestatgeria cd) throws Exception {
        ExportadorLlibres.exportarCSV(f, view, cd);
    }
}
