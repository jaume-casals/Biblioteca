package persistencia;

/**
 * Constants d'esquema compartides entre els DAO i el camí de còpia de seguretat.
 */
public final class Schema {
    private Schema() {}

    /**
     * Ordre DELETE correcte per satisfer les claus foranes: primer les
     * taules filles (les que tenen FK a altres), les taules pares les
     * últimes. Compartit entre {@link LlibreDaoCore#netejarAllData()},
     * {@link herramienta.ServeiCopiaSeguretat#backupToSQL} i qualsevol
     * reinicialització massiva — l'ordre ha de mantenir-se sincronitzat
     * o les restriccions FOREIGN KEY fallaran en mode estricte.
     */
    public static final String[] CLEAR_ORDER = {
        "lectura", "prestec", "llibre_llista", "llista",
        "llibre_autor", "llibre_tag", "tag", "autor", "llibre"
    };
}
