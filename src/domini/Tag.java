package domini;

public class Tag {
    private int id;
    private String nom;

    public Tag(int id, String nom) { this.id = id; this.nom = nom; }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    @Override
    public String toString() { return nom; }
}
