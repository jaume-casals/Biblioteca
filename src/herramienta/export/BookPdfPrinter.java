package herramienta.export;

import domini.Llibre;
import herramienta.BookExporter;

import java.util.List;

/**
 * Lifted from the 50+ line inline lambda in {@code BookExporter.exportPDF}. Today this is a
 * thin delegator so callers can address the printer by name; future refactor will move the
 * paging/layout code from BookExporter here for testability.
 */
public final class BookPdfPrinter {
    private BookPdfPrinter() {}
    public static void print(List<Llibre> view) { BookExporter.exportPDF(view); }
}
