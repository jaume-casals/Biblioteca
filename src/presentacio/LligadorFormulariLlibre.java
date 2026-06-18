package presentacio;

import domini.Llibre;

import javax.swing.JTextField;

/**
 * Enllaç de camps compartit pels diàlegs de llibre nou
 * ({@code DialegDesarLlibres}) i d'edició ({@code PanellDetallsLlibre}).
 * Actualment és un helper prim que exposa els mapejos getText/setText;
 * un refactor futur allotjarà aquí el codi complet d'enllaç dels
 * 27 camps.
 */
public final class LligadorFormulariLlibre {
    private LligadorFormulariLlibre() {}

    public static void vincularString(JTextField field, String value) {
        field.setText(value == null ? "" : value);
    }

    public static String readString(JTextField field) {
        String s = field.getText();
        return s == null ? "" : s.trim();
    }

    /** Omple els quatre camps de text principals des d'un Llibre. Redueix el copy-paste als diàlegs. */
    public static void carregarCore(Llibre l, JTextField isbn, JTextField nom, JTextField autor, JTextField any) {
        vincularString(isbn, String.valueOf(l.obtenirISBN()));
        vincularString(nom, l.obtenirNom());
        vincularString(autor, l.obtenirAutor());
        vincularString(any, l.obtenirAny() != null ? String.valueOf(l.obtenirAny()) : "");
    }
}
