package domini;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value-object view of a book's identifying/business data.
 * Carries a 16-field subset of {@link Llibre} (identity, authors, title,
 * year, descripcio, valoracio, preu, llegit, imatge, pagines, editorial,
 * serie, volum, idioma, format, paisOrigen). The remaining Llibre state
 * (notes, paginesLlegides, dataCompra, dataLectura, estat, desitjat,
 * nomCa/Es/En, exemplars, llenguaOriginal, cover blob) is consumed in
 * place from the mutable Llibre. Future refactor: Llibre composes a
 * LlibreCore + extension state.
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

    /** Bulk projection — equivalent to {@code source.stream().map(LlibreCore::from).toList()}
     *  but avoids the stream lambda allocation. */
    public static List<LlibreCore> extractCores(List<Llibre> source) {
        List<LlibreCore> out = new ArrayList<>(source.size());
        for (Llibre l : source) out.add(from(l));
        return out;
    }
}
