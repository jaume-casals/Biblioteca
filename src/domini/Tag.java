package domini;

import java.util.LinkedHashMap;
import java.util.Map;

import persistencia.dao.TagDao;
public class Tag {
    private int id;
    private String nom;

    // Actualment les etiquetes no tenen camp de color. Si en el futur
    // s'afegeix suport de color a les etiquetes, cal afegir un camp
    // privat String color + getColor()/setColor() seguint el patró de
    // Llista, i actualitzar TagDao, TagRouter i l'esquema de la BBDD
    // (migració).

    public Tag(int id, String nom) { this.id = id; this.nom = nom; }

    public int obtenirId() { return id; }
    public String obtenirNom() { return nom; }
    public void posarNom(String nom) {
        if (nom == null || nom.isBlank())
            throw new BibliotecaException.Validacio(herramienta.i18n.I18n.t("val_tag_blank"));
        this.nom = nom;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nom", nom);
        return m;
    }

    /** Retorna nom — s'usa per a visualitzar a JComboBox i a toString(). No és una representació de depuració. */
    @Override
    public String toString() { return nom; }

    // Identitat entre bases de dades: equals compara només per l'id
    // generat a la BBDD, de manera que dues etiquetes amb el mateix
    // nom però de bases de dades diferents mai no seran iguals. Les
    // etiquetes transitòries (id=0) mai no són iguals entre elles —
    // representen creacions en curs l'id de les quals encara no es
    // coneix.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        if (this.id == 0 || other.id == 0) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
