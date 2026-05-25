package herramienta.export;

import domini.Llibre;
import herramienta.BookExporter;

import java.util.List;

public final class PdfExporter {
    private PdfExporter() {}
    public static void export(List<Llibre> view) {
        BookExporter.exportPDF(view);
    }
}
