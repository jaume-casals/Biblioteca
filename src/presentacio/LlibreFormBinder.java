package presentacio;

import domini.Llibre;

import javax.swing.JTextField;

/**
 * Shared field-binding for the new-book ({@code GuardarLlibresDialogo}) and edit
 * ({@code DetallesLlibrePanel}) dialogs. Currently a thin helper exposing getText/setText
 * mappings; future refactor will host the full 27-field bind code.
 */
public final class LlibreFormBinder {
    private LlibreFormBinder() {}

    public static void bindString(JTextField field, String value) {
        field.setText(value == null ? "" : value);
    }

    public static String readString(JTextField field) {
        String s = field.getText();
        return s == null ? "" : s.trim();
    }

    /** Populate the four core text fields from a Llibre. Reduces copy-paste in dialogs. */
    public static void loadCore(Llibre l, JTextField isbn, JTextField nom, JTextField autor, JTextField any) {
        bindString(isbn, String.valueOf(l.getISBN()));
        bindString(nom, l.getNom());
        bindString(autor, l.getAutor());
        bindString(any, l.getAny() != null ? String.valueOf(l.getAny()) : "");
    }
}
