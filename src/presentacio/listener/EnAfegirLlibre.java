package presentacio.listener;

import domini.Llibre;

@FunctionalInterface
public interface EnAfegirLlibre {
    void enAfegit(Llibre l);
}
