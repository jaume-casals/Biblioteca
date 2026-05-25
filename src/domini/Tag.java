package domini;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tag {
    private int id;
    private String nom;

    // Tags currently have no color field. If color-tag support is added in the future,
    // add a private String color field + getColor()/setColor() mirroring Llista's pattern,
    // and update TagDao, TagRouter, and the DB schema (migration).

    public Tag(int id, String nom) { this.id = id; this.nom = nom; }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("El nom del tag no pot estar buit");
        this.nom = nom;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nom", nom);
        return m;
    }

    /** Returns nom — used for display in JComboBox and toString(). Not a debug representation. */
    @Override
    public String toString() { return nom; }

    // Cross-db identity concern: equals compares only by database-generated id,
    // so two Tags with the same nom but from different DB instances will never be equal.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        return this.id == ((Tag) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
