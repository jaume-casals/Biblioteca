package presentacio.listener;

import domini.Llibre;

/** UI callback when a book is deleted. */
@FunctionalInterface
public interface OnLlibreDelete {
    void onBookDeleted(Llibre l);

    default void onBookDeleting(EsborrarEvent e) {}

    static boolean hauriaProceed(EsborrarEvent e) {
        return !(e.esCancellable() && e.esVetoed());
    }

    /** Cancellable/vetoed wrapper used by presenters that want to ask
     *  listeners whether a delete should proceed. Created speculatively;
     *  exercised by the test suite ({@code deleteEventCancellableVeto},
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
