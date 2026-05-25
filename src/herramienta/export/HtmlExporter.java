package herramienta.export;

import domini.Llibre;
import herramienta.BookExporter;
import interficie.BibliotecaWriter;

import java.io.File;
import java.util.List;

public final class HtmlExporter {
    private HtmlExporter() {}
    public static void export(File f, List<Llibre> view, BibliotecaWriter cd, boolean groupByShelf, boolean tableView) throws Exception {
        BookExporter.exportHTML(f, view, cd, groupByShelf, tableView);
    }
}
