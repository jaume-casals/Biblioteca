package domini;

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
    public void setColor(String color) { this.color = color; }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Double getValoracioLlibre() { return valoracioLlibre; }
    public void setValoracioLlibre(Double v) { valoracioLlibre = v; }
    public Boolean getLlegitLlibre() { return llegitLlibre; }
    public void setLlegitLlibre(Boolean l) { llegitLlibre = l; }

    @Override
    public String toString() { return nom; }
}
