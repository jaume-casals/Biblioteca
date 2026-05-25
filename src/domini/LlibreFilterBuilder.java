package domini;

/**
 * Fluent builder for {@link LlibreFilter}. Tolerated additive API alongside the public-fields
 * style — eventual goal is to make LlibreFilter immutable; meanwhile this lets new code avoid
 * touching the public fields directly.
 */
public final class LlibreFilterBuilder {
    private final LlibreFilter f = LlibreFilter.empty();

    public LlibreFilterBuilder isbn(Long v)         { f.isbn = v;         return this; }
    public LlibreFilterBuilder autor(String v)      { f.autor = v;        return this; }
    public LlibreFilterBuilder nom(String v)        { f.nom = v;          return this; }
    public LlibreFilterBuilder anyMin(Integer v)    { f.anyMin = v;       return this; }
    public LlibreFilterBuilder anyMax(Integer v)    { f.anyMax = v;       return this; }
    public LlibreFilterBuilder valoracioMin(Double v){f.valoracioMin = v; return this; }
    public LlibreFilterBuilder valoracioMax(Double v){f.valoracioMax = v; return this; }
    public LlibreFilterBuilder preuMin(Double v)    { f.preuMin = v;      return this; }
    public LlibreFilterBuilder preuMax(Double v)    { f.preuMax = v;      return this; }
    public LlibreFilterBuilder llegit(Boolean v)    { f.llegit = v;       return this; }
    public LlibreFilterBuilder tagId(Integer v)     { f.tagId = v;        return this; }
    public LlibreFilterBuilder llistaId(Integer v)  { f.llistaId = v;     return this; }
    public LlibreFilterBuilder editorial(String v)  { f.editorial = v;    return this; }
    public LlibreFilterBuilder serie(String v)      { f.serie = v;        return this; }
    public LlibreFilterBuilder format(String v)     { f.format = v;       return this; }
    public LlibreFilterBuilder idioma(String v)     { f.idioma = v;       return this; }
    public LlibreFilterBuilder sort(String column, boolean asc) {
        f.sortColumn = column; f.sortAsc = asc; return this;
    }

    public LlibreFilter build() { return f; }

    public static LlibreFilterBuilder of() { return new LlibreFilterBuilder(); }
}
