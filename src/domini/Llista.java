package domini;

import java.util.Map;

public class Llista extends EntitatNomenada {
    private int ordre;
    private String color;
    // S'estableix només quan es recupera en el context d'un llibre concret
    private Double valoracioLlibre;
    private Boolean llegitLlibre;

    public Llista(int id, String nom) { super(id, nom); }

    @Override
    protected String claveNomBlank() { return "val_llista_blank"; }

    public int obtenirOrdre() { return ordre; }
    public void posarOrdre(int ordre) { this.ordre = ordre; }
    public String obtenirColor() { return color; }

    /** Font única de veritat per a la validació de colors; null significa "sense color / netejar". */
    public static boolean esValidColor(String color) {
        return color == null || color.matches("#[0-9a-fA-F]{3}") || color.matches("#[0-9a-fA-F]{6}");
    }

    public void posarColor(String color) {
        if (!esValidColor(color))
            throw new BibliotecaException.Validacio(herramienta.i18n.I18n.t("val_color_invalid", color));
        this.color = color;
    }

    public Double obtenirValoracioLlibre() { return valoracioLlibre; }
    public void posarValoracioLlibre(Double v) { valoracioLlibre = v; }
    public Boolean obtenirLlegitLlibre() { return llegitLlibre; }
    public void posarLlegitLlibre(Boolean l) { llegitLlibre = l; }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("ordre", ordre);
        m.put("color", color);
        return m;
    }

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
