package domini;

public final class ConstructorFiltreLlibre {
    private final LlibreFilter filter;

    private ConstructorFiltreLlibre() {
        this.filter = new LlibreFilter();
    }

    public static ConstructorFiltreLlibre of() {
        return new ConstructorFiltreLlibre();
    }

    public ConstructorFiltreLlibre isbn(Long value) {
        filter.withIsbn(value); return this;
    }

    public ConstructorFiltreLlibre autor(String value) {
        filter.withAutor(value); return this;
    }

    public ConstructorFiltreLlibre nom(String value) {
        filter.withNom(value); return this;
    }

    public ConstructorFiltreLlibre anyMin(Integer value) {
        filter.withAnyMin(value); return this;
    }

    public ConstructorFiltreLlibre anyMax(Integer value) {
        filter.withAnyMax(value); return this;
    }

    public ConstructorFiltreLlibre valoracioMin(Double value) {
        filter.withValoracioMin(value); return this;
    }

    public ConstructorFiltreLlibre valoracioMax(Double value) {
        filter.withValoracioMax(value); return this;
    }

    public ConstructorFiltreLlibre preuMin(Double value) {
        filter.withPreuMin(value); return this;
    }

    public ConstructorFiltreLlibre preuMax(Double value) {
        filter.withPreuMax(value); return this;
    }

    public ConstructorFiltreLlibre llegit(Boolean value) {
        filter.withLlegit(value); return this;
    }

    public ConstructorFiltreLlibre tagId(Integer value) {
        filter.withTagId(value); return this;
    }

    public ConstructorFiltreLlibre llistaId(Integer value) {
        filter.withLlistaId(value); return this;
    }

    public ConstructorFiltreLlibre editorial(String value) {
        filter.withEditorial(value); return this;
    }

    public ConstructorFiltreLlibre serie(String value) {
        filter.withSerie(value); return this;
    }

    public ConstructorFiltreLlibre format(String value) {
        filter.withFormat(value); return this;
    }

    public ConstructorFiltreLlibre idioma(String value) {
        filter.withIdioma(value); return this;
    }

    public ConstructorFiltreLlibre sort(String column, boolean ascending) {
        filter.withSort(new EspecificacioOrdenacio(column, ascending));
        return this;
    }

    public ConstructorFiltreLlibre sort(EspecificacioOrdenacio value) {
        filter.withSort(value); return this;
    }

    public LlibreFilter build() {
        return filter.copy();
    }
}
