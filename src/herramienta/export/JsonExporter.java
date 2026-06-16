package herramienta.export;

import herramienta.BookExporter;
import interficie.BibliotecaReader;

import java.io.File;

public final class JsonExporter {
    private JsonExporter() {}
    public static void export(File f, BibliotecaReader cd) throws Exception {
        BookExporter.exportJSON(f, cd);
    }
}
