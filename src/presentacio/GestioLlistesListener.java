package presentacio;

/** Interfície explícita de callbacks per a accions de gestió de prestatgeries (substitueix Runnable en brut). */
public interface GestioLlistesListener {
    default void enCanviarPrestatgeries() {}
    default void enReanomenarPrestatge(int id, String newName) {}
    default void enEliminarPrestatge(int id) {}
}
