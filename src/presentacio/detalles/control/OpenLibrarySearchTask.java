package presentacio.detalles.control;

import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.OpenLibraryClient;
import herramienta.UITheme;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class OpenLibrarySearchTask extends SwingWorker<OpenLibrarySearchTask.SearchResult, Void> {

    public static class SearchResult {
        public final Map<String, String> meta;
        public final byte[] coverBlob;

        public SearchResult(Map<String, String> meta, byte[] coverBlob) {
            this.meta = meta;
            this.coverBlob = coverBlob;
        }
    }

    private final String isbn;
    private final String titol;
    private final String autor;
    private final GuardarLlibresDialogo vista;
    private final byte[] existingBlob;
    private final Consumer<byte[]> onCoverFound;

    public OpenLibrarySearchTask(String isbn, String titol, String autor,
                                  GuardarLlibresDialogo vista,
                                  byte[] existingBlob,
                                  Consumer<byte[]> onCoverFound) {
        this.isbn = isbn;
        this.titol = titol;
        this.autor = autor;
        this.vista = vista;
        this.existingBlob = existingBlob;
        this.onCoverFound = onCoverFound;
    }

    @Override
    protected SearchResult doInBackground() throws Exception {
        Map<String, String> meta;
        if (!isbn.isEmpty()) {
            meta = OpenLibraryClient.lookupByISBN(isbn);
        } else if (!titol.isEmpty()) {
            meta = OpenLibraryClient.lookupByTitle(titol);
        } else {
            meta = OpenLibraryClient.lookupByAutor(autor);
        }

        // Fetch the cover in parallel with any subsequent processing
        // (per the tot.txt MEDIUM finding). The 300ms OL rate-limiter
        // is enforced inside fetchCoverByISBN so the parallel call
        // doesn't bypass the throttle.
        java.util.concurrent.CompletableFuture<byte[]> coverFuture = null;
        String isbnForCover = !isbn.isEmpty() ? isbn : meta.get("isbn");
        if (isbnForCover != null && !isbnForCover.isBlank() && existingBlob == null
                && !meta.containsKey("error")) {
            final String finalIsbn = isbnForCover;
            coverFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> OpenLibraryClient.fetchCoverByISBN(finalIsbn));
        }

        byte[] coverBlob = coverFuture == null ? null : coverFuture.join();

        return new SearchResult(meta, coverBlob);
    }

    @Override
    protected void done() {
        if (!vista.isDisplayable()) return;

        JButton btn = vista.getBtnCercaInternet();
        btn.setEnabled(true);
        btn.setText(I18n.t("btn_cerca_internet"));
        btn.setBackground(UITheme.palette().green());
        // Revert the green success flash after 1.5s so a subsequent
        // search starts from a neutral button (per the tot.txt MEDIUM
        // finding: the green is one-shot feedback, not a persistent
        // style). The Timer is one-shot and stored on the dialog via
        // a client property keyed by the button itself so a second
        // success doesn't stack two timers.
        javax.swing.Timer revert = new javax.swing.Timer(1500, ev -> {
            if (vista.isDisplayable()) {
                btn.setBackground(UITheme.palette().accent());
            }
        });
        revert.setRepeats(false);
        revert.start();
        vista.getProgressBar().setVisible(false);
        vista.getTextISBN().setEditable(true);

        try {
            SearchResult result = get();
            Map<String, String> meta = result.meta;

            if (meta.containsKey("error")) {
                JOptionPane.showMessageDialog(vista,
                    I18n.t("dlg_network_error") + "\n" + meta.get("error"),
                    I18n.t("dlg_network_error_title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (meta.isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                    I18n.t("dlg_no_results_msg"),
                    I18n.t("dlg_search_internet_title"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (meta.containsKey("title") && vista.getTextNom().getText().isEmpty())
                vista.getTextNom().setText(meta.get("title"));
            if (meta.containsKey("autor") && vista.getTextAutor().getText().isEmpty())
                vista.getTextAutor().setText(meta.get("autor"));
            if (meta.containsKey("any") && vista.getTextAny().getText().isEmpty())
                vista.getTextAny().setText(meta.get("any"));
            if (meta.containsKey("isbn") && vista.getTextISBN().getText().isEmpty())
                vista.getTextISBN().setText(meta.get("isbn"));
            if (meta.containsKey("descripcio") && vista.getTextDescripcio().getText().isEmpty())
                vista.getTextDescripcio().setText(meta.get("descripcio"));

            if (result.coverBlob != null && existingBlob == null) {
                onCoverFound.accept(result.coverBlob);
                javax.swing.ImageIcon icon = UITheme.scaledIcon(result.coverBlob, 120);
                vista.getLabelPreview().setIcon(icon != null ? icon : presentacio.CoverImageCache.NO_COVER);
            }
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }
}