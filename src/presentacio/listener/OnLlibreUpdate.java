package presentacio.listener;

import domini.Llibre;

/** UI callback after a book is created or updated. */
public interface OnLlibreUpdate {
    void actualitzarLlibre(Llibre l, boolean nuevo);
}
