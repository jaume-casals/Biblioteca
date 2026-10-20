package presentacio.listener;

import domini.Llibre;

/** Callback de la UI quan s'elimina un llibre. */
@FunctionalInterface
public interface EnEliminarLlibre {
    void enEliminarLlibre(Llibre l);

    default void enEliminantLlibre(EsborrarEvent e) {}

    static boolean hauriaProceed(EsborrarEvent e) {
        return !(e.esCancellable() && e.esVetoed());
    }

    /** Embolcall cancellable / vetat usat pels presenters que volen
     *  preguntar als listeners si cal continuar amb una eliminació.
     *  Creat de forma especulativa; l'exercita la suite de tests
     *  ({@code deleteEventCancellableVeto},
     *  {@code deleteEventNonCancellable}). */
    class EsborrarEvent {
        private final boolean cancellable;
        private boolean vetoed;

        public EsborrarEvent(boolean cancellable) {
            this.cancellable = cancellable;
        }

        public boolean esCancellable() { return cancellable; }
        public boolean esVetoed() { return vetoed; }
        public void veto() { vetoed = true; }
    }
}
