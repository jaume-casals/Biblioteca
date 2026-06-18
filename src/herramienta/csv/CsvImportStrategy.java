package herramienta.csv;

import interficie.EscritorBiblioteca;
import java.util.Map;

public interface CsvImportStrategy {
    /** Nom curt per a logs i missatges d'error (p.ex. "Goodreads", "LibraryThing", "Natiu"). */
    String obtenirNom();
    boolean potHandle(String headerRow);
    /** Retorna cert si la fila s'ha importat, fals si s'ha omès (p.ex. duplicat). Llança en error d'anàlisi. */
    boolean analitzarLine(String[] cols, Map<String, Integer> hMap, EscritorBiblioteca cd) throws domini.BibliotecaException;
}
