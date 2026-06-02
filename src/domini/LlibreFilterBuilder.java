package domini;

/**
 * Fluent builder for {@link LlibreFilter}. Les noves crides haurien de passar pel
 * builder en lloc d'escriure directament als camps — els camps de {@code LlibreFilter}
 * són privats. Mètodes com {@code withX} de {@link LlibreFilter} permeten mutar
 * in-place; el builder és útil quan es construeix des de zero amb valors dinàmics.
 */
public final class LlibreFilterBuilder {
    private final LlibreFilter f = LlibreFilter.empty();

    public LlibreFilterBuilder isbn(Long v)         { f.withIsbn(v);         return this; }
    public LlibreFilterBuilder autor(String v)      { f.withAutor(v);        return this; }
    public LlibreFilterBuilder nom(String v)        { f.withNom(v);          return this; }
    public LlibreFilterBuilder anyMin(Integer v)    { f.withAnyMin(v);       return this; }
    public LlibreFilterBuilder anyMax(Integer v)    { f.withAnyMax(v);       return this; }
    public LlibreFilterBuilder valoracioMin(Double v){f.withValoracioMin(v); return this; }
    public LlibreFilterBuilder valoracioMax(Double v){f.withValoracioMax(v); return this; }
    public LlibreFilterBuilder preuMin(Double v)    { f.withPreuMin(v);      return this; }
    public LlibreFilterBuilder preuMax(Double v)    { f.withPreuMax(v);      return this; }
    public LlibreFilterBuilder llegit(Boolean v)    { f.withLlegit(v);       return this; }
    public LlibreFilterBuilder tagId(Integer v)     { f.withTagId(v);        return this; }
    public LlibreFilterBuilder llistaId(Integer v)  { f.withLlistaId(v);     return this; }
    public LlibreFilterBuilder editorial(String v)  { f.withEditorial(v);    return this; }
    public LlibreFilterBuilder serie(String v)      { f.withSerie(v);        return this; }
    public LlibreFilterBuilder format(String v)     { f.withFormat(v);       return this; }
    public LlibreFilterBuilder idioma(String v)     { f.withIdioma(v);       return this; }
    public LlibreFilterBuilder sort(String column, boolean asc) {
        f.withSort(new SortSpec(column, asc)); return this;
    }

    public LlibreFilter build() { return f; }

    public static LlibreFilterBuilder of() { return new LlibreFilterBuilder(); }
}
