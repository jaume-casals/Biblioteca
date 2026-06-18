package herramienta;

/**
 * Marcador de posició per a constructors de printf per taula de còpia de seguretat.
 * {@link ServeiCopiaSeguretat#backupToSQL} actualment inlinea un printf de 26 args
 * per a la taula llibre; una refactorització futura mourà un constructor per taula
 * aquí (llibre, llista, llibre_llista, tag, llibre_tag, prestec, lectura,
 * autor, llibre_autor).
 *
 * <p>Avui, els consumidors haurien de continuar usant {@code ServeiCopiaSeguretat.backupToSQL};
 * aquesta classe és la costura amb nom on viuran els futurs constructors per taula.
 */
public final class ImpressorTaulaCopiaSeguretat {
    private ImpressorTaulaCopiaSeguretat() {}
}
