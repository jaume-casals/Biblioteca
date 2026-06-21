package herramienta.io.csv;

import domini.Llibre;
import herramienta.text.ValidadorLlibre;
import persistencia.contract.EscritorBiblioteca;

import java.util.Map;

public class GoodreadsCsvStrategy implements CsvImportStrategy {

    /** Cache de shelfName → Llista, compartida entre files per evitar
     *  re-construir-la per a cada llibre (10k llibres = 10k HashMap
     *  innecessaris a la versió anterior). */
    private java.util.Map<String, domini.Llista> shelfCache;

    @Override public String obtenirNom() { return "Goodreads"; }

    @Override
    public boolean potHandle(String headerRow) {
        if (headerRow == null || headerRow.isBlank()) return false;
        String[] cols = UtilitatsCsv.analitzarLine(headerRow);
        // Les exportacions de Goodreads solen tenir més de 30 columnes.
        // Requerim tant un sentinella únic de Goodreads ("Book Id" — que
        // no apareix en cap de les altres estratègies) com la columna
        // "Exclusive Shelf" usada per derivar el flag "llegit".
        if (cols.length < 10) return false;
        if (!headerRow.contains("Book Id")) return false;
        if (!headerRow.contains("Exclusive Shelf")) return false;
        // Protecció contra falsos positius accidentals: Title també és
        // necessari per extreure un nom de llibre no buit.
        if (!headerRow.contains("Title")) return false;
        return true;
    }

    @Override
    public boolean analitzarLine(String[] c, Map<String, Integer> hMap, EscritorBiblioteca cd) throws domini.BibliotecaException {
        String isbnRaw = UtilitatsCsv.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = UtilitatsCsv.colVal(hMap, c, "ISBN");
        isbnRaw = UtilitatsCsv.analitzarIsbn(isbnRaw);
        if (isbnRaw.isEmpty()) throw new domini.BibliotecaException("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        if (UtilitatsCsv.existsInLibrary(cd, isbn)) return false;

        String nom       = UtilitatsCsv.colVal(hMap, c, "Title");
        String autor     = UtilitatsCsv.colVal(hMap, c, "Author");
        String additionalAuthors = UtilitatsCsv.colVal(hMap, c, "Additional Authors");
        if (!additionalAuthors.isEmpty() && !autor.isEmpty()) autor = autor + ", " + additionalAuthors;
        else if (!additionalAuthors.isEmpty()) autor = additionalAuthors;
        String editorial = UtilitatsCsv.colVal(hMap, c, "Publisher");
        String pagesStr  = UtilitatsCsv.colVal(hMap, c, "Number of Pages");
        int pagines = pagesStr.isEmpty() ? 0 : (int) UtilitatsCsv.analitzarDoubleOrZero(pagesStr);
        int any = 0;
        String yearStr = UtilitatsCsv.colVal(hMap, c, "Year Published");
        if (yearStr.isEmpty()) yearStr = UtilitatsCsv.colVal(hMap, c, "Original Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = UtilitatsCsv.analitzarDoubleOrZero(UtilitatsCsv.colVal(hMap, c, "My Rating")) * 2.0;
        String shelf    = UtilitatsCsv.colVal(hMap, c, "Exclusive Shelf");
        boolean llegit  = "read".equalsIgnoreCase(shelf);
        String notes    = UtilitatsCsv.colVal(hMap, c, "My Review");
        String privateNotes = UtilitatsCsv.colVal(hMap, c, "Private Notes");
        if (!privateNotes.isEmpty()) notes = notes.isEmpty() ? privateNotes : notes + "\n" + privateNotes;
        String dataLect = UtilitatsCsv.colVal(hMap, c, "Date Read");
        Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, "", valoracio, 0.0, llegit, "");
        l.posarEditorial(editorial);
        l.posarPagines(pagines);
        l.posarNotes(notes);
        if (!dataLect.isEmpty() && dataLect.matches("\\d{4}[/\\-]\\d{2}[/\\-]\\d{2}"))
            l.posarDataLectura(herramienta.text.UtilitatsData.normalizeDate(dataLect));
        cd.afegirLlibre(l);

        String bookshelves = UtilitatsCsv.colVal(hMap, c, "Bookshelves");
        if (!bookshelves.isEmpty()) {
            for (String s : bookshelves.split(",")) {
                domini.Llista llista = ShelvesHelper.cercarOCrearPrestatge(cd, shelfCache, s.trim());
                if (llista != null) cd.afegirLlibreToLlista(isbn, llista.obtenirId(), valoracio, llegit);
            }
        }
        return true;
    }
}
