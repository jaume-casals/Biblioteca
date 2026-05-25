package presentacio.listener;

import domini.Llibre;

@FunctionalInterface
public interface OnLlibreAdded {
    void onAdded(Llibre l);
}
