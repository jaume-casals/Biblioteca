package herramienta.csv;

import interficie.BibliotecaWriter;
import java.util.Map;

public interface CsvImportStrategy {
    boolean canHandle(String headerRow);
    /** Returns true if the row was imported, false if skipped (e.g. duplicate). Throws on parse error. */
    boolean parseLine(String[] cols, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception;
}
