package herramienta.csv;

import domini.Llibre;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class GoodreadsCsvStrategy implements CsvImportStrategy {

    @Override
    public boolean canHandle(String headerRow) {
        String[] cols = CsvUtils.parseLine(headerRow);
        return cols.length >= 5 && headerRow.contains("Book Id") && headerRow.contains("Exclusive Shelf");
    }

    @Override
    public boolean parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception {
        String isbnRaw = CsvUtils.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = CsvUtils.colVal(hMap, c, "ISBN");
        isbnRaw = CsvUtils.parseIsbn(isbnRaw);
        if (isbnRaw.isEmpty()) throw new Exception("ISBN buit");
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
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                domini.Llista llista = shelfMap.get(nomLlista);
                if (llista == null) { llista = cd.addLlista(nomLlista); shelfMap.put(nomLlista, llista); }
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, llegit);
            }
        }
        return true;
    }
}
