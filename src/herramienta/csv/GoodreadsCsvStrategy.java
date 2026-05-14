package herramienta.csv;

import domini.Llibre;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class GoodreadsCsvStrategy implements CsvImportStrategy {

    @Override
    public boolean canHandle(String headerRow) {
        return headerRow.contains("Book Id") || headerRow.contains("Exclusive Shelf");
    }

    @Override
    public void parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception {
        String isbnRaw = CsvUtils.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = CsvUtils.colVal(hMap, c, "ISBN");
        isbnRaw = isbnRaw.replaceAll("[^0-9]", "");
        if (isbnRaw.isEmpty()) throw new Exception("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        try { cd.getLlibre(isbn); return; } catch (Exception ignored) {}

        String nom       = CsvUtils.colVal(hMap, c, "Title");
        String autor     = CsvUtils.colVal(hMap, c, "Author");
        String editorial = CsvUtils.colVal(hMap, c, "Publisher");
        String pagesStr  = CsvUtils.colVal(hMap, c, "Number of Pages");
        int pagines = pagesStr.isEmpty() ? 0 : (int) CsvUtils.parseDoubleOrZero(pagesStr);
        int any = 0;
        String yearStr = CsvUtils.colVal(hMap, c, "Year Published");
        if (yearStr.isEmpty()) yearStr = CsvUtils.colVal(hMap, c, "Original Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = CsvUtils.parseDoubleOrZero(CsvUtils.colVal(hMap, c, "My Rating"));
        String shelf    = CsvUtils.colVal(hMap, c, "Exclusive Shelf");
        boolean llegit  = "read".equalsIgnoreCase(shelf);
        String notes    = CsvUtils.colVal(hMap, c, "My Review");
        String dataLect = CsvUtils.colVal(hMap, c, "Date Read");
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, "", valoracio, 0.0, llegit, "");
        l.setEditorial(editorial);
        l.setPagines(pagines);
        l.setNotes(notes);
        if (!dataLect.isEmpty()) l.setDataLectura(dataLect);
        cd.addLlibre(l);

        String bookshelves = CsvUtils.colVal(hMap, c, "Bookshelves");
        if (!bookshelves.isEmpty()) {
            for (String s : bookshelves.split(",")) {
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                domini.Llista llista = cd.getAllLlistes().stream()
                    .filter(ll -> ll.getNom().equals(nomLlista)).findFirst().orElse(null);
                if (llista == null) llista = cd.addLlista(nomLlista);
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, llegit);
            }
        }
    }
}
