package domini;

import java.util.LinkedHashMap;
import java.util.Map;

public class Llista {
    private int id;
    private String nom;
    private int ordre;
    private String color;
    // Set only when fetched in the context of a specific book
    private Double valoracioLlibre;
    private Boolean llegitLlibre;

    public Llista(int id, String nom) { this.id = id; this.nom = nom; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public String getColor() { return color; }

    /** Single source of truth for color validation; null means "no color / clear". */
    public static boolean isValidColor(String color) {
        return color == null || color.matches("#[0-9a-fA-F]{3}") || color.matches("#[0-9a-fA-F]{6}");
    }

    /**
     * Unchecked setter — call {@link #isValidColor(String)} first when the value
     * comes from user input. DAO load paths use this directly with values that
     * have already been validated on write.
     */
    public void setColor(String color) { this.color = color; }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("El nom de la llista no pot estar buit");
        this.nom = nom;
    }
    public Double getValoracioLlibre() { return valoracioLlibre; }
    public void setValoracioLlibre(Double v) { valoracioLlibre = v; }
    public Boolean getLlegitLlibre() { return llegitLlibre; }
    public void setLlegitLlibre(Boolean l) { llegitLlibre = l; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nom", nom);
        m.put("ordre", ordre);
        m.put("color", color);
        return m;
    }

    /** Returns nom — used by JComboBox for display. Not a debug representation. */
    @Override
    public String toString() { return nom; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Llista)) return false;
        return this.id == ((Llista) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
