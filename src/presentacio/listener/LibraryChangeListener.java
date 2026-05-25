package presentacio.listener;

import domini.Llibre;

/** English-named book change callbacks for the presentation layer. */
public interface LibraryChangeListener {

    void onBookAdded(Llibre book);
    void onBookUpdated(Llibre book);
    void onBookDeleted(Llibre book);

    static LibraryChangeListener fromLegacy(presentacio.listener.OnLlibreUpdate update,
            presentacio.listener.OnLlibreDelete delete) {
        return new LibraryChangeListener() {
            @Override public void onBookAdded(Llibre book) { update.actualitzarLlibre(book, true); }
            @Override public void onBookUpdated(Llibre book) { update.actualitzarLlibre(book, false); }
            @Override public void onBookDeleted(Llibre book) { delete.eliminarLlibre(book); }
        };
    }

    default OnLlibreUpdate asOnLlibreUpdate() {
        return (book, nuevo) -> {
            if (nuevo) onBookAdded(book);
            else onBookUpdated(book);
        };
    }

    default OnLlibreDelete asOnLlibreDelete() {
        return this::onBookDeleted;
    }

    default OnLlibreAdded asOnLlibreAdded() {
        return this::onBookAdded;
    }
}
