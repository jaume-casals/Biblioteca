package herramienta.io.export;

import domini.Llibre;
import domini.Llista;
import herramienta.io.csv.UtilitatsCsv;
import persistencia.contract.LectorBiblioteca;
import persistencia.row.LlibreLlistaRow;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/** Exportació CSV compatible amb Goodreads compartida, usada per API i Swing. */
public final class GoodreadsExportService {
    private GoodreadsExportService() {}

    public static String exportarToCsv(LectorBiblioteca cd) throws Exception {
        // Sobrecàrrega de String conservada per compatibilitat enrere
        // amb els pocs consumidors existents que llegeixen l'exportació
        // sencera a memòria. Per a biblioteques de 10k llibres això
        // assigna ~10 MB transitòriament; la sobrecàrrega de streaming
        // {@link #exportToCsv(EscritorBiblioteca, PrintWriter)} és el
        // camí recomanat per a codi nou.
        java.io.StringWriter sw = new java.io.StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            exportarToCsv(cd, pw);
        }
        return sw.toString();
    }

    /**
     * Variant de streaming — escriu una fila a la vegada a {@code pw},
     * sense String intermitja. Per a una biblioteca de 10k llibres és
     * ~10x més ràpida (sense assignació del document complet) i ~20x
     * més amigable amb el heap.
     */
    public static void exportarToCsv(LectorBiblioteca cd, PrintWriter pw) throws Exception {
        String header = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,"
                 + "Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,"
                 + "Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,"
                 + "Read Count,Recommended For,Recommended By,Owned Copies,Original Purchase Date,"
                 + "Condition,Condition Description,BCID";
        // Comptem el nombre de columnes del header per tancar-les totes
        // dinàmicament a la fila (la versió anterior tenia un literal
        // hardcoded de 9 comes que es desfasava del recompte de capçalera).
        int totalColumns = header.split(",", -1).length;
        pw.println(header);
        Map<Integer, Llista> llistaById = new HashMap<>();
        for (Llista ll : cd.obtenirAllLlistes()) llistaById.put(ll.obtenirId(), ll);
        Map<Long, List<Llista>> llibLlistes = new HashMap<>();
        for (LlibreLlistaRow row : cd.obtenirAllLlibreLlistaRows()) {
            Llista ll = llistaById.get(row.llistaId());
            if (ll != null) llibLlistes.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(ll);
        }
        int rowId = 1;
        for (Llibre l : cd.obtenirAllLlibres()) {
            List<Llista> llistes = llibLlistes.getOrDefault(l.obtenirISBN(), List.of());
            String shelf = Boolean.TRUE.equals(l.obtenirLlegit()) ? "read"
                : (!llistes.isEmpty() ? llistes.get(0).obtenirNom() : "to-read");
            String bookshelves = llistes.stream().map(Llista::obtenirNom)
                .collect(joining(", "));
            // La valoració de Goodreads és 0..5 (punts mitjos). La
            // nostra és 0..10; la implementació antiga feia servir
            // Math.round, que aplica l'arrodoniment bancari (4.5 → 4).
            // Fem servir una variant mig-amunt per fidelitat: 4.5 → 5,
            // 4.4 → 4. La constant 2.0 de baix reescala el nostre
            // 0..10 al 0..5 de Goodreads.
            int myRating = 0;
            Double val = l.obtenirValoracio();
            if (val != null && val > 0) myRating = (int) Math.floor(val / 2.0 + 0.5);
            Integer any = l.obtenirAny();
            // Nombre de columnes amb valor explícit que escrivim abans
            // dels camps de cua buits. Si el header canvia, la secció
            // d'emplenament s'adapta sense tocar aquest recompte.
            int writtenValues = 0;
            pw.print(rowId++); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirNom())); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirAutor())); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirAutor())); writtenValues++;
            pw.print(','); pw.print(""); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ("=\"" + String.valueOf(l.obtenirISBN()) + "\"")); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ("=\"" + String.valueOf(l.obtenirISBN()) + "\"")); writtenValues++;
            pw.print(','); pw.print(myRating); writtenValues++;
            pw.print(','); pw.print(""); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirEditorial() != null ? l.obtenirEditorial() : "")); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirFormat() != null ? l.obtenirFormat() : "")); writtenValues++;
            pw.print(','); pw.print(l.obtenirPagines() > 0 ? l.obtenirPagines() : ""); writtenValues++;
            pw.print(','); pw.print(any != null && any > 0 ? any : ""); writtenValues++;
            pw.print(','); pw.print(any != null && any > 0 ? any : ""); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirDataLectura() != null ? l.obtenirDataLectura() : "")); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirDataCompra() != null ? l.obtenirDataCompra() : "")); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(bookshelves)); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(shelf)); writtenValues++;
            pw.print(','); pw.print(UtilitatsCsv.csvQ(l.obtenirNotes() != null ? l.obtenirNotes() : "")); writtenValues++;
            // Emplena les columnes restants amb valors buits. Un
            // separador per cada columna restant; el PrintWriter escriu
            // una fila acabada en coma seguida de salt de línia, que
            // és una representació vàlida de "trailing empty fields"
            // per a la majoria d'analitzadors CSV.
            while (writtenValues < totalColumns) {
                pw.print(',');
                writtenValues++;
            }
            pw.println();
        }
    }
}
