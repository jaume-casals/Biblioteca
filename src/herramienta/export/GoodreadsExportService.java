package herramienta.export;

import domini.Llibre;
import domini.Llista;
import herramienta.csv.CsvUtils;
import interficie.BibliotecaWriter;
import persistencia.LlibreLlistaRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/** Shared Goodreads-compatible CSV export used by API and Swing. */
public final class GoodreadsExportService {
    private GoodreadsExportService() {}

    public static String exportToCsv(BibliotecaWriter cd) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,")
          .append("Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,")
          .append("Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,")
          .append("Read Count,Recommended For,Recommended By,Owned Copies,Original Purchase Date,")
          .append("Condition,Condition Description,BCID\n");
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
            sb.append(rowId++).append(',')
              .append(CsvUtils.csvQ(l.getNom())).append(',')
              .append(CsvUtils.csvQ(l.getAutor())).append(',')
              .append(CsvUtils.csvQ(l.getAutor())).append(',')
              .append(',')
              .append(CsvUtils.csvQ("=\"" + String.valueOf(l.getISBN()) + "\"")).append(',')
              .append(CsvUtils.csvQ("=\"" + String.valueOf(l.getISBN()) + "\"")).append(',')
              .append(l.getValoracio() > 0 ? (int) Math.round(l.getValoracio() / 2.0) : 0).append(',')
              .append(',')
              .append(CsvUtils.csvQ(l.getEditorial() != null ? l.getEditorial() : "")).append(',')
              .append(CsvUtils.csvQ(l.getFormat() != null ? l.getFormat() : "")).append(',')
              .append(l.getPagines() > 0 ? l.getPagines() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(CsvUtils.csvQ(l.getDataLectura() != null ? l.getDataLectura() : "")).append(',')
              .append(CsvUtils.csvQ(l.getDataCompra() != null ? l.getDataCompra() : "")).append(',')
              .append(CsvUtils.csvQ(bookshelves)).append(',')
              .append(CsvUtils.csvQ(shelf)).append(',')
              .append(CsvUtils.csvQ(l.getNotes() != null ? l.getNotes() : "")).append(',')
              .append(",,,,,,,,,\n");
        }
        return sb.toString();
    }
}
