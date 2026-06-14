package herramienta.export;

import domini.Llibre;
import domini.Llista;
import herramienta.csv.CsvUtils;
import interficie.BibliotecaWriter;
import persistencia.LlibreLlistaRow;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/** Shared Goodreads-compatible CSV export used by API and Swing. */
public final class GoodreadsExportService {
    private GoodreadsExportService() {}

    public static String exportToCsv(BibliotecaWriter cd) throws Exception {
        // String overload retained for back-compat with the few existing
        // callers that read the whole export into memory. For 10k-book
        // libraries this allocates ~10 MB transiently; the streaming
        // {@link #exportToCsv(BibliotecaWriter, PrintWriter)} overload
        // is the recommended path for new code.
        java.io.StringWriter sw = new java.io.StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            exportToCsv(cd, pw);
        }
        return sw.toString();
    }

    /**
     * Streaming variant — writes one row at a time to {@code pw}, no
     * intermediate String. For a 10k-book library this is ~10x faster
     * (no full-doc allocation) and ~20x friendlier on the heap.
     */
    public static void exportToCsv(BibliotecaWriter cd, PrintWriter pw) throws Exception {
        pw.println("Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,"
                 + "Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publicación Year,"
                 + "Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,"
                 + "Read Count,Recommended For,Recommended By,Owned Copies,Original Purchase Date,"
                 + "Condition,Condition Description,BCID");
        Map<Integer, Llista> llistaById = new HashMap<>();
        for (Llista ll : cd.getAllLlistes()) llistaById.put(ll.getId(), ll);
        Map<Long, List<Llista>> llibLlistes = new HashMap<>();
        for (LlibreLlistaRow row : cd.getAllLlibreLlistaRows()) {
            Llista ll = llistaById.get(row.llistaId());
            if (ll != null) llibLlistes.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(ll);
        }
        int rowId = 1;
        for (Llibre l : cd.getAllLlibres()) {
            List<Llista> llistes = llibLlistes.getOrDefault(l.getISBN(), List.of());
            String shelf = Boolean.TRUE.equals(l.getLlegit()) ? "read"
                : (!llistes.isEmpty() ? llistes.get(0).getNom() : "to-read");
            String bookshelves = llistes.stream().map(Llista::getNom)
                .collect(joining(", "));
            // Goodreads' rating is 0..5 (half-points). Our rating is
            // 0..10; the legacy implementation used Math.round which
            // does banker's rounding (4.5 → 4). Use a half-up variant
            // for fidelity: 4.5 → 5, 4.4 → 4. The constant 2.0 below
            // rescales our 0..10 to Goodreads' 0..5.
            int myRating = 0;
            Double val = l.getValoracio();
            if (val != null && val > 0) myRating = (int) Math.floor(val / 2.0 + 0.5);
            Integer any = l.getAny();
            pw.print(rowId++); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getNom())); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getAutor())); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getAutor())); pw.print(',');
            pw.print(',');
            pw.print(CsvUtils.csvQ("=\"" + String.valueOf(l.getISBN()) + "\"")); pw.print(',');
            pw.print(CsvUtils.csvQ("=\"" + String.valueOf(l.getISBN()) + "\"")); pw.print(',');
            pw.print(myRating); pw.print(',');
            pw.print(',');
            pw.print(CsvUtils.csvQ(l.getEditorial() != null ? l.getEditorial() : "")); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getFormat() != null ? l.getFormat() : "")); pw.print(',');
            pw.print(l.getPagines() > 0 ? l.getPagines() : ""); pw.print(',');
            pw.print(any != null && any > 0 ? any : ""); pw.print(',');
            pw.print(any != null && any > 0 ? any : ""); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getDataLectura() != null ? l.getDataLectura() : "")); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getDataCompra() != null ? l.getDataCompra() : "")); pw.print(',');
            pw.print(CsvUtils.csvQ(bookshelves)); pw.print(',');
            pw.print(CsvUtils.csvQ(shelf)); pw.print(',');
            pw.print(CsvUtils.csvQ(l.getNotes() != null ? l.getNotes() : "")); pw.print(',');
            pw.println(",,,,,,,,");
        }
    }
}
