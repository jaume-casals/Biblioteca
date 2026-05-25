package domini;

import java.util.List;

/**
 * Immutable value-object view of a book's identifying/business data.
 * Excludes cache state (hasBlob, imatgeBlob) and free-text fields (notes) — those live on the
 * mutable {@link Llibre}. Future refactor: Llibre composes a LlibreCore + extension state.
 */
public record LlibreCore(
    long isbn,
    String nom,
    List<String> autors,
    Integer any,
    String descripcio,
    Double valoracio,
    Double preu,
    Boolean llegit,
    String imatge,
    int pagines,
    String editorial,
    String serie,
    int volum,
    String idioma,
    String format,
    String paisOrigen
) {
    /** Project a {@link Llibre} into its immutable core. */
    public static LlibreCore from(Llibre l) {
        return new LlibreCore(
            l.getISBN(), l.getNom(), l.getAutors(), l.getAny(), l.getDescripcio(),
            l.getValoracio(), l.getPreu(), l.getLlegit(), l.getImatge(),
            l.getPagines(), l.getEditorial(), l.getSerie(), l.getVolum(),
            l.getIdioma(), l.getFormat(), l.getPaisOrigen()
        );
    }
}
