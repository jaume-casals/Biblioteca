package interficie;

import domini.Llibre;
import presentacio.listener.OnLlibreDelete;
import presentacio.listener.OnLlibreUpdate;

public interface LibraryEvents extends OnLlibreUpdate, OnLlibreDelete {
    default void onBlobChanged(long isbn, boolean hasBlob) {}
    default void onMembershipChanged(long isbn, int shelfId, boolean added) {}
    default void onLlibreDeleted(Llibre l) { onBookDeleted(l); }
}