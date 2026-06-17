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
    private SortSpec sort = SortSpec.defaultAsc();

    public LlibreFilter() {}

    public static LlibreFilter empty() { return new LlibreFilter(); }

    public String getAutor() { return autor; }
    public String getNom() { return nom; }
    public Long getIsbn() { return isbn; }
    public Integer getAnyMin() { return anyMin; }
    public Integer getAnyMax() { return anyMax; }
    public Double getValoracioMin() { return valoracioMin; }
    public Double getValoracioMax() { return valoracioMax; }
    public Double getPreuMin() { return preuMin; }
    public Double getPreuMax() { return preuMax; }
    public Boolean getLlegit() { return llegit; }
    public Integer getTagId() { return tagId; }
    public String getEditorial() { return editorial; }
    public String getSerie() { return serie; }
    public String getFormat() { return format; }
    public String getIdioma() { return idioma; }
    public Integer getLlistaId() { return llistaId; }
    public SortSpec getSort() { return sort; }

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
    public LlibreFilter withSort(SortSpec sort) {
        this.sort = (sort != null) ? sort : SortSpec.defaultAsc();
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
        f.sort = (this.sort != null) ? this.sort : SortSpec.defaultAsc();
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
