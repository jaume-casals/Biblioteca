package presentacio;

/** Explicit callback interface for shelf-management actions (replaces raw Runnable). */
public interface GestioLlistesListener {
    default void onShelvesChanged() {}
    default void onShelfRenamed(int id, String newName) {}
    default void onShelfDeleted(int id) {}
}
