package presentacio.listener;

import domini.Llibre;

/** UI callback when a book is deleted. */
@FunctionalInterface
public interface OnLlibreDelete {
    void onBookDeleted(Llibre l);

    /** Cancellable/vetoed wrapper used by presenters that want to ask
     *  listeners whether a delete should proceed. Created speculatively;
     *  exercised by the test suite ({@code deleteEventCancellableVeto},
     *  {@code deleteEventNonCancellable}). */
    class DeleteEvent {
        private final Llibre book;
        private final boolean cancellable;
        private boolean vetoed;

        public DeleteEvent(Llibre book, boolean cancellable) {
            this.book = book;
            this.cancellable = cancellable;
        }

        public Llibre getBook() { return book; }
        public boolean isCancellable() { return cancellable; }
        public boolean isVetoed() { return vetoed; }
        public void veto() { vetoed = true; }
    }
}
