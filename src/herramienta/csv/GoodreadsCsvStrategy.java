package herramienta.csv;

import domini.Llibre;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class GoodreadsCsvStrategy implements CsvImportStrategy {

    @Override public String getName() { return "Goodreads"; }

    @Override
    public boolean canHandle(String headerRow) {
        if (headerRow == null || headerRow.isBlank()) return false;
        String[] cols = CsvUtils.parseLine(headerRow);
        // Goodreads exports typically have 30+ columns. Require both a unique Goodreads
        // sentinel ("Book Id" — not "Book Id" anywhere else in our other strategies)
        // and the "Exclusive Shelf" column used to derive the "llegit" flag.
        if (cols.length < 10) return false;
        if (!headerRow.contains("Book Id")) return false;
        if (!headerRow.contains("Exclusive Shelf")) return false;
        // Guard against accidental false positives: Title is also required to extract
        // a non-empty book name.
        if (!headerRow.contains("Title")) return false;
        return true;
    }

    @Override
    public boolean parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws domini.BibliotecaException {
        String isbnRaw = CsvUtils.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = CsvUtils.colVal(hMap, c, "ISBN");
        isbnRaw = CsvUtils.parseIsbn(isbnRaw);
        if (isbnRaw.isEmpty()) throw new domini.BibliotecaException("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        if (CsvUtils.existsInLibrary(cd, isbn)) return false;

        String nom       = CsvUtils.colVal(hMap, c, "Title");
        String autor     = CsvUtils.colVal(hMap, c, "Author");
        String additionalAuthors = CsvUtils.colVal(hMap, c, "Additional Authors");
        if (!additionalAuthors.isEmpty() && !autor.isEmpty()) autor = autor + ", " + additionalAuthors;
        else if (!additionalAuthors.isEmpty()) autor = additionalAuthors;
        String editorial = CsvUtils.colVal(hMap, c, "Publisher");
        String pagesStr  = CsvUtils.colVal(hMap, c, "Number of Pages");
        int pagines = pagesStr.isEmpty() ? 0 : (int) CsvUtils.parseDoubleOrZero(pagesStr);
        int any = 0;
        String yearStr = CsvUtils.colVal(hMap, c, "Year Published");
        if (yearStr.isEmpty()) yearStr = CsvUtils.colVal(hMap, c, "Original Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = CsvUtils.parseDoubleOrZero(CsvUtils.colVal(hMap, c, "My Rating")) * 2.0;
        String shelf    = CsvUtils.colVal(hMap, c, "Exclusive Shelf");
        boolean llegit  = "read".equalsIgnoreCase(shelf);
        String notes    = CsvUtils.colVal(hMap, c, "My Review");
        String privateNotes = CsvUtils.colVal(hMap, c, "Private Notes");
        if (!privateNotes.isEmpty()) notes = notes.isEmpty() ? privateNotes : notes + "\n" + privateNotes;
        String dataLect = CsvUtils.colVal(hMap, c, "Date Read");
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, "", valoracio, 0.0, llegit, "");
        l.setEditorial(editorial);
        l.setPagines(pagines);
        l.setNotes(notes);
        if (!dataLect.isEmpty() && dataLect.matches("\\d{4}[/\\-]\\d{2}[/\\-]\\d{2}"))
            l.setDataLectura(herramienta.DateUtils.normalizeDate(dataLect));
        cd.addLlibre(l);

        String bookshelves = CsvUtils.colVal(hMap, c, "Bookshelves");
        if (!bookshelves.isEmpty()) {
            java.util.Map<String, domini.Llista> shelfMap = new java.util.HashMap<>();
            for (domini.Llista ll : cd.getAllLlistes()) shelfMap.put(ll.getNom(), ll);
            for (String s : bookshelves.split(",")) {
                ShelvesHelper.addBookToShelf(cd, shelfMap, isbn, s.trim(), valoracio, llegit);
            }
        }
        return true;
    }
}
