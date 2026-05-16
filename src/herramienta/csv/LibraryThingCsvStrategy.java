package herramienta.csv;

import domini.Llibre;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class LibraryThingCsvStrategy implements CsvImportStrategy {

    @Override
    public boolean canHandle(String headerRow) {
        return headerRow.contains("BCID");
    }

    @Override
    public boolean parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception {
        String isbnRaw = CsvUtils.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = CsvUtils.colVal(hMap, c, "ISBN");
        isbnRaw = CsvUtils.parseIsbn(isbnRaw);
        if (isbnRaw.isEmpty()) throw new Exception("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        if (CsvUtils.existsInLibrary(cd, isbn)) return false;

        String nom   = CsvUtils.colVal(hMap, c, "Title");
        String autor = CsvUtils.colVal(hMap, c, "Authors");
        // Invert "Lastname, Firstname" → "Firstname Lastname". Only works for a single comma;
        // multi-word lastnames (e.g. "van der Berg, Jan") are handled incorrectly.
        if (autor.contains(",") && !autor.contains(";")) {
            String[] parts = autor.split(",", 2);
            autor = parts[1].trim() + " " + parts[0].trim();
        }
        int any = 0;
        String yearStr = CsvUtils.colVal(hMap, c, "Original Publication Year");
        if (yearStr.isEmpty()) yearStr = CsvUtils.colVal(hMap, c, "Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = CsvUtils.parseDoubleOrZero(CsvUtils.colVal(hMap, c, "Rating")) * 2.0;
        String desc  = CsvUtils.colVal(hMap, c, "Summary");
        String notes = CsvUtils.colVal(hMap, c, "Comments");
        if (notes.isEmpty()) notes = CsvUtils.colVal(hMap, c, "Review");
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, desc, valoracio, 0.0, false, "");
        if (!notes.isEmpty()) l.setNotes(notes);
        cd.addLlibre(l);

        String collections = CsvUtils.colVal(hMap, c, "Collections");
        if (!collections.isEmpty()) {
            java.util.Map<String, domini.Llista> shelfMap = new java.util.HashMap<>();
            for (domini.Llista ll : cd.getAllLlistes()) shelfMap.put(ll.getNom(), ll);
            for (String s : collections.split(",")) {
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                domini.Llista llista = shelfMap.get(nomLlista);
                if (llista == null) { llista = cd.addLlista(nomLlista); shelfMap.put(nomLlista, llista); }
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, false);
            }
        }

        String tags = CsvUtils.colVal(hMap, c, "Tags");
        if (!tags.isEmpty()) {
            java.util.Map<String, domini.Tag> tagMap = new java.util.HashMap<>();
            for (domini.Tag tg : cd.getAllTags()) tagMap.put(tg.getNom(), tg);
            for (String t : tags.split(",")) {
                String nomTag = t.trim();
                if (nomTag.isEmpty()) continue;
                domini.Tag tag = tagMap.get(nomTag);
                if (tag == null) { tag = cd.addTag(nomTag); tagMap.put(nomTag, tag); }
                cd.addLlibreToTag(isbn, tag.getId());
            }
        }
        return true;
    }
}
