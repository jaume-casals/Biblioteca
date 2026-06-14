package presentacio.listener;

import domini.Llibre;

/**
 * Composite listener for book insert/update/delete events.
 *
 * <p>Merges the two parent contracts ({@link OnLlibreUpdate},
 * {@link OnLlibreDelete}) so a presenter can subscribe to both
 * with a single field. Implementers must override
 * {@link OnLlibreUpdate#onBookUpdated} and
 * {@link OnLlibreDelete#onBookDeleted} (or use a default
 * implementation through a subclass).
 *
 * <p>Previously this interface also exposed a Catalan-named
 * alias ({@code actualitzarLlibre} / {@code eliminarLlibre})
 * whose defaults delegated back to the English names, forming
 * a self-referential cycle that would loop forever if a
 * subclass overrode neither side. The alias has been removed:
 * the English names are the only contract, and the only
 * implementer in the codebase ({@code MainFrameControl})
 * already overrides them.
 */
public interface EnActualizarBBDD extends OnLlibreUpdate, OnLlibreDelete {
}
