package persistencia;

/** Fila JDBC per a {@code autor}; usada per {@link AutorDao} i l'exportació de còpia de seguretat. */
public record AutorRow(int id, String nom) {
    public static AutorRow fromArray(Object[] a) { return new AutorRow((int) a[0], (String) a[1]); }
}
