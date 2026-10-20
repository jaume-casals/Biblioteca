package presentacio.util;

import domini.Llibre;

import java.util.List;

/**
 * Aplica un resultat de consulta a la vista.
 *
 * <p>Transporta a més l'indicació de quantos llibres coincideixen amb
 * l'àmbit i el filtre vigents. sense això, el comptador visible s'ha de
 * deduir del contingut renderitzat i deixa de coincidir amb el recompte
 * real quan la vista està paginada.
 */
@FunctionalInterface
public interface AplicadorResultat {

    /**
     * @param fulla generació del resultat, o null si s'aplica directament
     * @param llibres llibres que es mostren
     * @param ambit llibres que coincideixen amb l'àmbit i el filtre
     */
    void accept(CoordinadorGeneracions.Fulla<?> fulla, List<Llibre> llibres, int ambit);
}
