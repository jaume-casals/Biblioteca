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
        private final Llibre book;
        private final boolean cancellable;
        private boolean vetoed;

        public EsborrarEvent(Llibre book, boolean cancellable) {
            this.book = book;
            this.cancellable = cancellable;
        }

        public Llibre obtenirBook() { return book; }
        public boolean esCancellable() { return cancellable; }
        public boolean esVetoed() { return vetoed; }
        public void veto() { vetoed = true; }
    }
}
