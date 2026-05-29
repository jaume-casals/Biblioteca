package presentacio.listener;

import domini.Llibre;

public interface LibraryChangeListener {

    void onBookAdded(Llibre book);
    void onBookUpdated(Llibre book);
    void onBookDeleted(Llibre book);

    static LibraryChangeListener fromLegacy(OnLlibreUpdate update, OnLlibreDelete delete) {
        return new LibraryChangeListener() {
            @Override public void onBookAdded(Llibre book)  { update.onBookUpdated(book, true); }
            @Override public void onBookUpdated(Llibre book) { update.onBookUpdated(book, false); }
            @Override public void onBookDeleted(Llibre book) { delete.onBookDeleted(book); }
        };
    }

    default OnLlibreUpdate asOnLlibreUpdate() {
        return (book, isNew) -> {
            if (isNew) onBookAdded(book);
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
