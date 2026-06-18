package domini;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista inmutable (value-object) de les dades identificatives/de negoci d'un
 * llibre. Transporta un subconjunt de 16 camps de {@link Llibre} (identitat,
 * autors, títol, any, descripcio, valoracio, preu, llegit, imatge, pagines,
 * editorial, serie, volum, idioma, format, paisOrigen). La resta de l'estat
 * de Llibre (notes, paginesLlegides, dataCompra, dataLectura, estat, desitjat,
 * nomCa/Es/En, exemplars, llenguaOriginal, blob de portada) es consumeix
 * directament des del Llibre mutable. Refactor futur: Llibre compon un
 * LlibreCore + estat d'extensió.
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
    /** Proyecta un {@link Llibre} al seu nucli immutable. */
    public static LlibreCore from(Llibre l) {
        return new LlibreCore(
            l.obtenirISBN(), l.obtenirNom(), l.obtenirAutors(), l.obtenirAny(), l.obtenirDescripcio(),
            l.obtenirValoracio(), l.obtenirPreu(), l.obtenirLlegit(), l.obtenirImatge(),
            l.obtenirPagines(), l.obtenirEditorial(), l.obtenirSerie(), l.obtenirVolum(),
            l.obtenirIdioma(), l.obtenirFormat(), l.obtenirPaisOrigen()
        );
    }

/** Projecció massiva — equivalent a {@code source.stream().map(LlibreCore::from).toList()}
 *  però evita l'assignació de la lambda del stream. */
    public static List<LlibreCore> extractCores(List<Llibre> source) {
        List<LlibreCore> out = new ArrayList<>(source.size());
        for (Llibre l : source) out.add(from(l));
        return out;
    }
}
