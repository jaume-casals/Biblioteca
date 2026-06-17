package domini;

/**
 * Fluent builder for {@link LlibreFilter}. Les noves crides haurien de passar pel
 * builder en lloc d'escriure directament als camps — els camps de {@code LlibreFilter}
 * són privats. Mètodes com {@code withX} de {@link LlibreFilter} permeten mutar
 * in-place; el builder és útil quan es construeix des de zero amb valors dinàmics.
 */
public final class ConstructorFiltreLlibre {
    private final LlibreFilter f = LlibreFilter.empty();

    public ConstructorFiltreLlibre isbn(Long v)         { f.withIsbn(v);         return this; }
    public ConstructorFiltreLlibre autor(String v)      { f.withAutor(v);        return this; }
    public ConstructorFiltreLlibre nom(String v)        { f.withNom(v);          return this; }
    public ConstructorFiltreLlibre anyMin(Integer v)    { f.withAnyMin(v);       return this; }
    public ConstructorFiltreLlibre anyMax(Integer v)    { f.withAnyMax(v);       return this; }
    public ConstructorFiltreLlibre valoracioMin(Double v){f.withValoracioMin(v); return this; }
    public ConstructorFiltreLlibre valoracioMax(Double v){f.withValoracioMax(v); return this; }
    public ConstructorFiltreLlibre preuMin(Double v)    { f.withPreuMin(v);      return this; }
    public ConstructorFiltreLlibre preuMax(Double v)    { f.withPreuMax(v);      return this; }
    public ConstructorFiltreLlibre llegit(Boolean v)    { f.withLlegit(v);       return this; }
    public ConstructorFiltreLlibre tagId(Integer v)     { f.withTagId(v);        return this; }
    public ConstructorFiltreLlibre llistaId(Integer v)  { f.withLlistaId(v);     return this; }
    public ConstructorFiltreLlibre editorial(String v)  { f.withEditorial(v);    return this; }
    public ConstructorFiltreLlibre serie(String v)      { f.withSerie(v);        return this; }
    public ConstructorFiltreLlibre format(String v)     { f.withFormat(v);       return this; }
    public ConstructorFiltreLlibre idioma(String v)     { f.withIdioma(v);       return this; }
    public ConstructorFiltreLlibre sort(String column, boolean asc) {
        f.withSort(new EspecificacioOrdenacio(column, asc)); return this;
    }

    public LlibreFilter build() { return f; }

    public static ConstructorFiltreLlibre of() { return new ConstructorFiltreLlibre(); }
}
