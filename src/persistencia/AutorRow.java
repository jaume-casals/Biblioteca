package persistencia;

/** JDBC row for {@code autor}; used by {@link AutorDao} and backup export. */
public record AutorRow(int id, String nom) {
    public static AutorRow fromArray(Object[] a) { return new AutorRow((int) a[0], (String) a[1]); }
}
