package herramienta.export;

import domini.Llibre;
import herramienta.ExportadorLlibres;
import interficie.ShelfReader;

import java.io.File;
import java.util.List;

public final class HtmlExporter {
    private HtmlExporter() {}
    public static void export(File f, List<Llibre> view, ShelfReader cd, boolean groupByShelf, boolean tableView) throws Exception {
        ExportadorLlibres.exportarHTML(f, view, cd, groupByShelf, tableView);
    }
}
