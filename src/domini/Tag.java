package domini;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tag {
    private int id;
    private String nom;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        return this.id == ((Tag) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
