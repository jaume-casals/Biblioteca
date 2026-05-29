package presentacio.listener;

import domini.Llibre;

public interface EnActualizarBBDD extends OnLlibreUpdate, OnLlibreDelete {
    default void actualitzarLlibre(Llibre l, boolean nuevo) { onBookUpdated(l, nuevo); }
    default void eliminarLlibre(Llibre l) { onBookDeleted(l); }

    @Override default void onBookUpdated(Llibre l, boolean isNew) { actualitzarLlibre(l, isNew); }
    @Override default void onBookDeleted(Llibre l) { eliminarLlibre(l); }
}
