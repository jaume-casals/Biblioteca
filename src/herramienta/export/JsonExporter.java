package herramienta.export;

import herramienta.BookExporter;
import interficie.BibliotecaWriter;

import java.io.File;

public final class JsonExporter {
    private JsonExporter() {}
    public static void export(File f, BibliotecaWriter cd) throws Exception {
        BookExporter.exportJSON(f, cd);
    }
}
