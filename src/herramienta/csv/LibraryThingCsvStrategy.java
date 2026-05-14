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
    public void parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception {
        String isbnRaw = CsvUtils.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = CsvUtils.colVal(hMap, c, "ISBN");
        isbnRaw = isbnRaw.replaceAll("[^0-9]", "");
        if (isbnRaw.isEmpty()) throw new Exception("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        try { cd.getLlibre(isbn); return; } catch (Exception ignored) {}

        String nom   = CsvUtils.colVal(hMap, c, "Title");
        String autor = CsvUtils.colVal(hMap, c, "Authors");
        if (autor.contains(",") && !autor.contains(";")) {
            String[] parts = autor.split(",", 2);
            autor = parts[1].trim() + " " + parts[0].trim();
        }
        int any = 0;
        String yearStr = CsvUtils.colVal(hMap, c, "Original Publication Year");
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
            for (String s : collections.split(",")) {
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                domini.Llista llista = cd.getAllLlistes().stream()
                    .filter(ll -> ll.getNom().equals(nomLlista)).findFirst().orElse(null);
                if (llista == null) llista = cd.addLlista(nomLlista);
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, false);
            }
        }

        String tags = CsvUtils.colVal(hMap, c, "Tags");
        if (!tags.isEmpty()) {
            for (String t : tags.split(",")) {
                String nomTag = t.trim();
                if (nomTag.isEmpty()) continue;
                domini.Tag tag = cd.getAllTags().stream()
                    .filter(tg -> tg.getNom().equals(nomTag)).findFirst().orElse(null);
                if (tag == null) tag = cd.addTag(nomTag);
                cd.addLlibreToTag(isbn, tag.getId());
            }
        }
    }
}
