package herramienta.csv;

import interficie.BibliotecaWriter;
import java.util.Map;

public interface CsvImportStrategy {
    boolean canHandle(String headerRow);
    void parseLine(String[] cols, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception;
}
