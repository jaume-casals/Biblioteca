package presentacio.listener;

@FunctionalInterface
public interface OnLlibreBlobChanged {
    void onBlobChanged(long isbn, boolean teBlob);
}
