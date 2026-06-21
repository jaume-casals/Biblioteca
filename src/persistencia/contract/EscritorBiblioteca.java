package persistencia.contract;

/**
 * Superfície d'escriptura completa de la biblioteca. Agrega les
 * interfícies d'escriptura específiques (llibre, prestatgeria, etiqueta,
 * prèstec) i, per compatibilitat, les interfícies de lectura i les
 * operacions d'administració. Els mètodes d'escriptura ja no es
 * redeclaren aquí — la font canònica és cada sub-interval
 * ({@link EscritorLlibre}, {@link EscritorPrestatgeria},
 * {@link EscritorEtiqueta}, {@link EscritorPrestec}).
 *
 * <p>El perquè del canvi: abans cada mètode d'escriptura estava
 * declarat a dos llocs (aquí i al sub-interval). Qualsevol mètode
 * d'escriptura nou s'havia d'afegir dues vegades o els contractes
 * divergien silenciosament. Ara n'hi ha prou amb afegir-lo al
 * sub-interval corresponent.
 */
public interface EscritorBiblioteca
        extends LectorBiblioteca, AdministradorBiblioteca,
                EscritorLlibre, EscritorPrestatgeria,
                EscritorEtiqueta, EscritorPrestec {
}
