package domini;

import java.util.LinkedHashMap;
import java.util.Map;

public class Llista {
    private int id;
    private String nom;
    private int ordre;
    private String color;
    // S'estableix només quan es recupera en el context d'un llibre concret
    private Double valoracioLlibre;
    private Boolean llegitLlibre;

    public Llista(int id, String nom) { this.id = id; this.nom = nom; }

    public int obtenirOrdre() { return ordre; }
    public void posarOrdre(int ordre) { this.ordre = ordre; }
    public String obtenirColor() { return color; }

    /** Font única de veritat per a la validació de colors; null significa "sense color / netejar". */
    public static boolean esValidColor(String color) {
        return color == null || color.matches("#[0-9a-fA-F]{3}") || color.matches("#[0-9a-fA-F]{6}");
    }

    /**
     * Setter no comprovat — crida primer {@link #esValidColor(String)} quan
     * el valor provingui d'entrada de l'usuari. Els camins de càrrega del
     * DAO el fan servir directament amb valors que ja s'han validat en
     * escriure.
     */
    public void posarColor(String color) { this.color = color; }

    public int obtenirId() { return id; }
    public String obtenirNom() { return nom; }
    public void posarNom(String nom) {
        if (nom == null || nom.isBlank())
            throw new BibliotecaException.Validacio(herramienta.I18n.t("val_llista_blank"));
        this.nom = nom;
    }
    public Double obtenirValoracioLlibre() { return valoracioLlibre; }
    public void posarValoracioLlibre(Double v) { valoracioLlibre = v; }
    public Boolean obtenirLlegitLlibre() { return llegitLlibre; }
    public void posarLlegitLlibre(Boolean l) { llegitLlibre = l; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nom", nom);
        m.put("ordre", ordre);
        m.put("color", color);
        return m;
    }

    /** Retorna nom — l'usa JComboBox per visualitzar. No és una representació de depuració. */
    @Override
    public String toString() { return nom; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Llista other)) return false;
        // Les instàncies transitòries (id=0) s'identifiquen pel nom, igual
        // que a hashCode(). Sense aquesta branca, dos "new Llista(0, 'A')"
        // i "new Llista(0, 'B')" serien equals() però farien hash a cubs
        // diferents, trencant el contracte equal→hashCode als HashSet/Map.
        if (id == 0 || other.id == 0) return java.util.Objects.equals(nom, other.nom);
        return id == other.id;
    }

    @Override
    public int hashCode() {
        // Les instàncies transitòries (id=0) NO poden fer hash tot a 0 —
        // altrament un HashMap de prestatgeries en curs col·lapsaria cada
        // "new Llista(nom)" al mateix cub. Barregem el nom per a id=0
        // perquè cada prestatgeria transitòria sigui diferent. Les
        // prestatgeries persistides fan hash per id, com abans.
        return id == 0 ? nom.hashCode() : Integer.hashCode(id);
    }
}
