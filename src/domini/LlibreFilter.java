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
    /** DB column name for ORDER BY (null = default ISBN). Validated against whitelist in LlibreDao. */
    public String sortColumn;
    public boolean sortAsc = true;

    public static LlibreFilter empty() { return new LlibreFilter(); }

    // Update copy() whenever a new field is added above.
    public LlibreFilter copy() {
        LlibreFilter f = new LlibreFilter();
        f.autor = this.autor; f.nom = this.nom; f.isbn = this.isbn;
        f.anyMin = this.anyMin; f.anyMax = this.anyMax;
        f.valoracioMin = this.valoracioMin; f.valoracioMax = this.valoracioMax;
        f.preuMin = this.preuMin; f.preuMax = this.preuMax;
        f.llegit = this.llegit; f.tagId = this.tagId;
        f.editorial = this.editorial; f.serie = this.serie;
        f.format = this.format; f.idioma = this.idioma;
        f.llistaId = this.llistaId;
        f.sortColumn = this.sortColumn; f.sortAsc = this.sortAsc;
        return f;
    }

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
