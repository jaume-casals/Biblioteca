package presentacio.listener;

import domini.Llibre;

/** Callback de la UI quan un llibre es crea o s'actualitza. */
public interface EnActualitzarLlibre {
    void enActualitzarLlibre(Llibre l, boolean esNew);
}
