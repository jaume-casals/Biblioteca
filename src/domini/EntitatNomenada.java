package domini;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supertipus per a entitats que es modelen com a parell {@code id} + {@code nom}
 * (p. ex. {@link Llista} i {@link Tag}). Centralitza els accessors, la validació
 * del nom buit i la serialització bàsica a mapa. Les subclasses defineixen la
 * clau i18n del missatge d'error i mantenen el seu propi {@code equals}/{@code hashCode}.
 */
public abstract class EntitatNomenada {
    protected int id;
    protected String nom;

    protected EntitatNomenada(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public int obtenirId() { return id; }
    public String obtenirNom() { return nom; }

    /** Clau i18n del missatge de validació quan el nom és buit. */
    protected abstract String claveNomBlank();

    public void posarNom(String nom) {
        if (nom == null || nom.isBlank())
            throw new BibliotecaException.Validacio(herramienta.i18n.I18n.t(claveNomBlank()));
        this.nom = nom;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nom", nom);
        return m;
    }

    /** Retorna nom — l'usa JComboBox per visualitzar. No és una representació de depuració. */
    @Override
    public String toString() { return nom; }
}
