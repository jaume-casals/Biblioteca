package presentacio.listener;

import domini.Llibre;

/** UI callback when a book is deleted. */
public interface OnLlibreDelete {
    void eliminarLlibre(Llibre l);

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
