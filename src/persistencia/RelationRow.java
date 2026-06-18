package persistencia;

/**
 * Marcador per a registres de files de relacions de llibres: {@link LlibreLlistaRow}, {@link LlibreTagRow},
 * {@link LlibreAutorRow}. Permet que els gestors transversals (exportació/importació, buidats d'auditoria)
 * facin el despatx de manera genèrica.
 */
public interface RelationRow {
    long isbn();
}
