package presentacio.listener;

import domini.Llibre;

/**
 * Listener compost per a esdeveniments d'inserció, actualització i
 * eliminació de llibres.
 *
 * <p>Fusiona els dos contractes pare ({@link EnActualitzarLlibre},
 * {@link EnEliminarLlibre}) perquè un presenter pugui subscriure's a
 * tots dos amb un sol camp. Els implementadors han de sobreescriure
 * {@link EnActualitzarLlibre#enActualitzarLlibre} i
 * {@link EnEliminarLlibre#enEliminarLlibre} (o bé usar una
 * implementació per defecte a través d'una subclasse).
 *
 * <p>Anteriorment aquesta interfície també exposava un àlies amb
 * nom en català ({@code actualitzarLlibre} / {@code eliminarLlibre})
 * els quals delegaven per defecte als noms anglesos, formant un
 * cicle auto-referencial que hauria entrat en bucle infinit si una
 * subclasse no hagués sobreescrit cap dels dos costats. L'àlies s'ha
 * eliminat: els noms anglesos són l'únic contracte, i l'únic
 * implementador al codi ({@code MainFrameControl}) ja els
 * sobreescriu.
 */
public interface EnActualitzarBBDD extends EnActualitzarLlibre, EnEliminarLlibre {
}
