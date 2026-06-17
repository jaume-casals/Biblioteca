package domini;

public class LlibreFilter {
    private String autor;
    private String nom;
    private Long isbn;
    private Integer anyMin;
    private Integer anyMax;
    private Double valoracioMin;
    private Double valoracioMax;
    private Double preuMin;
    private Double preuMax;
    private Boolean llegit;
    private Integer tagId;
    private String editorial;
    private String serie;
    private String format;
    private String idioma;
    private Integer llistaId;
    private EspecificacioOrdenacio sort = EspecificacioOrdenacio.defaultAsc();

    public LlibreFilter() {}

    public static LlibreFilter empty() { return new LlibreFilter(); }

    public String obtenirAutor() { return autor; }
    public String obtenirNom() { return nom; }
    public Long obtenirIsbn() { return isbn; }
    public Integer obtenirAnyMin() { return anyMin; }
    public Integer obtenirAnyMax() { return anyMax; }
    public Double obtenirValoracioMin() { return valoracioMin; }
    public Double obtenirValoracioMax() { return valoracioMax; }
    public Double obtenirPreuMin() { return preuMin; }
    public Double obtenirPreuMax() { return preuMax; }
    public Boolean obtenirLlegit() { return llegit; }
    public Integer obtenirTagId() { return tagId; }
    public String obtenirEditorial() { return editorial; }
    public String obtenirSerie() { return serie; }
    public String getFormat() { return format; }
    public String obtenirIdioma() { return idioma; }
    public Integer obtenirLlistaId() { return llistaId; }
    public EspecificacioOrdenacio obtenirSort() { return sort; }

    public LlibreFilter withAutor(String v) { this.autor = v; return this; }
    public LlibreFilter withNom(String v) { this.nom = v; return this; }
    public LlibreFilter withIsbn(Long v) { this.isbn = v; return this; }
    public LlibreFilter withAnyMin(Integer v) { this.anyMin = v; return this; }
    public LlibreFilter withAnyMax(Integer v) { this.anyMax = v; return this; }
    public LlibreFilter withValoracioMin(Double v) { this.valoracioMin = v; return this; }
    public LlibreFilter withValoracioMax(Double v) { this.valoracioMax = v; return this; }
    public LlibreFilter withPreuMin(Double v) { this.preuMin = v; return this; }
    public LlibreFilter withPreuMax(Double v) { this.preuMax = v; return this; }
    public LlibreFilter withLlegit(Boolean v) { this.llegit = v; return this; }
    public LlibreFilter withTagId(Integer v) { this.tagId = v; return this; }
    public LlibreFilter withEditorial(String v) { this.editorial = v; return this; }
    public LlibreFilter withSerie(String v) { this.serie = v; return this; }
    public LlibreFilter withFormat(String v) { this.format = v; return this; }
    public LlibreFilter withIdioma(String v) { this.idioma = v; return this; }
    public LlibreFilter withLlistaId(Integer v) { this.llistaId = v; return this; }
    public LlibreFilter withSort(EspecificacioOrdenacio sort) {
        this.sort = (sort != null) ? sort : EspecificacioOrdenacio.defaultAsc();
        return this;
    }

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
        f.sort = (this.sort != null) ? this.sort : EspecificacioOrdenacio.defaultAsc();
        return f;
    }

    public boolean teAnyFilter() {
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
