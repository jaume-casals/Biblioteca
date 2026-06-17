package herramienta.csv;

import interficie.BibliotecaWriter;
import java.util.Map;

public interface CsvImportStrategy {
    /** Nom curt per a logs i missatges d'error (p.ex. "Goodreads", "LibraryThing", "Natiu"). */
    String getName();
    boolean potHandle(String headerRow);
    /** Returns true if the row was imported, false if skipped (e.g. duplicate). Throws on parse error. */
    boolean analitzarLine(String[] cols, Map<String, Integer> hMap, BibliotecaWriter cd) throws domini.BibliotecaException;
}
