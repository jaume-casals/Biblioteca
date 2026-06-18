package presentacio.listener;

@FunctionalInterface
public interface EnCanviarMembreLlista {
    void enCanviarMembre(long isbn, int llistaId, boolean added);
}
