package domini;

/**
 * Context per llibre d'una pertinença a prestatge: quin prestatge, quin llibre,
 * i els valors locals del prestatge (valoració, marca de llegit) que només
 * tenen sentit en el context d'aquesta parella (llibre, prestatge) concreta.
 * Fes servir això en lloc de posar els camps per-llibre directament a
 * {@link Llista} — el prestatge en si no té valoració, només la unió
 * (llibre, prestatge).
 */
public record LlibreLlistaContext(
    long isbn,
    int llistaId,
    String nom,
    int ordre,
    String color,
    Double valoracio,
    Boolean llegit
) {
    public static LlibreLlistaContext of(long isbn, int llistaId, String nom, int ordre, String color,
                                         Double valoracio, Boolean llegit) {
        return new LlibreLlistaContext(isbn, llistaId, nom, ordre, color, valoracio, llegit);
    }
}
