package herramienta.io.csv;

import domini.Llibre;
import herramienta.text.ValidadorLlibre;
import persistencia.contract.EscritorBiblioteca;

import java.util.Map;

public class LibraryThingCsvStrategy implements CsvImportStrategy {

    /** Caches per a prestatges i tags, compartides entre files per evitar
     *  re-construir-les per a cada llibre (10k llibres = 10k HashMap
     *  innecessaris). */
    private java.util.Map<String, domini.Llista> shelfCache;
    private java.util.Map<String, domini.Tag> tagMap;

    @Override public String obtenirNom() { return "LibraryThing"; }

    @Override
    public boolean potHandle(String headerRow) {
        if (headerRow == null || headerRow.isBlank()) return false;
        // BCID és l'identificador de llibre específic de LibraryThing;
        // apareix a cada exportació i és únic d'aquesta estratègia.
        // Requerim unes quantes columnes canòniques més per evitar
        // coincidir amb una subcadena esporàdica.
        if (!headerRow.contains("BCID")) return false;
        String[] cols = UtilitatsCsv.analitzarLine(headerRow);
        if (cols.length < 8) return false;
        if (!headerRow.contains("Title")) return false;
        if (!headerRow.contains("Authors")) return false;
        return true;
    }

    @Override
    public boolean analitzarLine(String[] c, Map<String, Integer> hMap, EscritorBiblioteca cd) throws domini.BibliotecaException {
        // Els valors d'ISBN de LibraryThing poden arribar entre
        // claudàtors com [978...]; parseIsbn elimina tots els caràcters
        // no numèrics, de manera que els claudàtors es treuen
        // automàticament.
        String isbnRaw = UtilitatsCsv.colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = UtilitatsCsv.colVal(hMap, c, "ISBN");
        isbnRaw = UtilitatsCsv.analitzarIsbn(isbnRaw);
        if (isbnRaw.isEmpty()) throw new domini.BibliotecaException("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        if (UtilitatsCsv.existsInLibrary(cd, isbn)) return false;

        String nom   = UtilitatsCsv.colVal(hMap, c, "Title");
        String autor = UtilitatsCsv.colVal(hMap, c, "Authors");
        // Inverteix "Cognom, Nom" → "Nom Cognom". Només funciona amb una
        // sola coma; els cognoms compostos (p. ex. "van der Berg, Jan")
        // es gestionen incorrectament. Limitació coneguda: quan hi ha
        // diversos autors separats per comes (en lloc de punts i coma),
        // la divisió produeix inversions "Nom Cognom" incorrectes.
        if (autor.contains(",") && !autor.contains(";")) {
            String[] parts = autor.split(",", 2);
            if (parts.length > 1) autor = parts[1].trim() + " " + parts[0].trim();
        }
        int any = 0;
        String yearStr = UtilitatsCsv.colVal(hMap, c, "Original Publication Year");
        if (yearStr.isEmpty()) yearStr = UtilitatsCsv.colVal(hMap, c, "Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = UtilitatsCsv.analitzarDoubleOrZero(UtilitatsCsv.colVal(hMap, c, "Rating")) * 2.0;
        String desc  = UtilitatsCsv.colVal(hMap, c, "Summary");
        String notes = UtilitatsCsv.colVal(hMap, c, "Comments");
        if (notes.isEmpty()) notes = UtilitatsCsv.colVal(hMap, c, "Review");
        Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, desc, valoracio, 0.0, false, "");
        if (!notes.isEmpty()) l.posarNotes(notes);
        cd.afegirLlibre(l);

        String collections = UtilitatsCsv.colVal(hMap, c, "Collections");
        if (!collections.isEmpty()) {
            for (String s : collections.split(",")) {
                domini.Llista llista = ShelvesHelper.cercarOCrearPrestatge(cd, shelfCache, s.trim());
                if (llista != null) cd.afegirLlibreToLlista(isbn, llista.obtenirId(), valoracio, false);
            }
        }

        String tags = UtilitatsCsv.colVal(hMap, c, "Tags");
        if (!tags.isEmpty()) {
            if (tagMap == null) {
                tagMap = new java.util.HashMap<>();
                for (domini.Tag tg : cd.obtenirAllTags()) tagMap.put(tg.obtenirNom(), tg);
            }
            for (String t : tags.split(",")) {
                String nomTag = t.trim();
                if (nomTag.isEmpty()) continue;
                domini.Tag tag = tagMap.get(nomTag);
                if (tag == null) { tag = cd.afegirTag(nomTag); tagMap.put(nomTag, tag); }
                cd.afegirLlibreToTag(isbn, tag.obtenirId());
            }
        }
        return true;
    }
}
