package domini;

public class LlibreFilter {
    public String autor;
    public String nom;
    public Long isbn;
    public Integer anyMin;
    public Integer anyMax;
    public Double valoracioMin;
    public Double valoracioMax;
    public Double preuMin;
    public Double preuMax;
    public Boolean llegit;
    public Integer tagId;
    public String editorial;
    public String serie;
    public String format;
    public String idioma;
    public Integer llistaId;

    public static LlibreFilter empty() { return new LlibreFilter(); }

    public boolean hasAnyFilter() {
        return autor != null || nom != null || isbn != null
            || anyMin != null || anyMax != null
            || valoracioMin != null || valoracioMax != null
            || preuMin != null || preuMax != null
            || llegit != null || tagId != null
            || editorial != null || serie != null
            || format != null || idioma != null
            || llistaId != null;
    }
}
