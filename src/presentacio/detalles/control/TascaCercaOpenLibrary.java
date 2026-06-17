package presentacio.detalles.control;

import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import herramienta.DialegError;
import herramienta.I18n;
import herramienta.ClientOpenLibrary;
import herramienta.UITheme;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class TascaCercaOpenLibrary extends SwingWorker<TascaCercaOpenLibrary.ResultatCerca, Void> {

    public static class ResultatCerca {
        public final Map<String, String> meta;
        public final byte[] coverBlob;

        public ResultatCerca(Map<String, String> meta, byte[] coverBlob) {
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

    public TascaCercaOpenLibrary(String isbn, String titol, String autor,
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
    protected ResultatCerca doInBackground() throws Exception {
        Map<String, String> meta;
        if (!isbn.isEmpty()) {
            meta = ClientOpenLibrary.lookupByISBN(isbn);
        } else if (!titol.isEmpty()) {
            meta = ClientOpenLibrary.lookupByTitle(titol);
        } else {
            meta = ClientOpenLibrary.lookupByAutor(autor);
        }

        // Obtenim la coberta en paral·lel amb qualsevol processament
        // posterior (segons el finding MEDIUM de tot.txt). El limitador
        // de 300ms d'OL s'aplica dins de fetchCoverByISBN, de manera
        // que la crida paral·lela no se salta el limitador.
        java.util.concurrent.CompletableFuture<byte[]> coverFuture = null;
        String isbnForCover = !isbn.isEmpty() ? isbn : meta.get("isbn");
        if (isbnForCover != null && !isbnForCover.isBlank() && existingBlob == null
                && !meta.containsKey("error")) {
            final String finalIsbn = isbnForCover;
            coverFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> ClientOpenLibrary.fetchCoverByISBN(finalIsbn));
        }

        byte[] coverBlob = coverFuture == null ? null : coverFuture.join();

        return new ResultatCerca(meta, coverBlob);
    }

    @Override
    protected void done() {
        if (!vista.isDisplayable()) return;

        JButton btn = vista.obtenirBtnCercaInternet();
        btn.setEnabled(true);
        btn.setText(I18n.t("btn_cerca_internet"));
        btn.setBackground(UITheme.palette().green());
        // Reverteix el verd del flash d'èxit després d'1,5s perquè una
        // cerca posterior comenci des d'un botó neutre (segons el
        // finding MEDIUM de tot.txt: el verd és un feedback d'un sol ús,
        // no un estil persistent). El Timer és d'un sol ús i es desa al
        // diàleg mitjançant una client property indexada pel botó, de
        // manera que un segon èxit no acumula dos timers.
        javax.swing.Timer revert = new javax.swing.Timer(1500, ev -> {
            if (vista.isDisplayable()) {
                btn.setBackground(UITheme.palette().accent());
            }
        });
        revert.setRepeats(false);
        revert.start();
        vista.obtenirProgressBar().setVisible(false);
        vista.obtenirTextISBN().setEditable(true);

        try {
            ResultatCerca result = get();
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

            if (meta.containsKey("title") && vista.obtenirTextNom().getText().isEmpty())
                vista.obtenirTextNom().setText(meta.get("title"));
            if (meta.containsKey("autor") && vista.obtenirTextAutor().getText().isEmpty())
                vista.obtenirTextAutor().setText(meta.get("autor"));
            if (meta.containsKey("any") && vista.obtenirTextAny().getText().isEmpty())
                vista.obtenirTextAny().setText(meta.get("any"));
            if (meta.containsKey("isbn") && vista.obtenirTextISBN().getText().isEmpty())
                vista.obtenirTextISBN().setText(meta.get("isbn"));
            if (meta.containsKey("descripcio") && vista.obtenirTextDescripcio().getText().isEmpty())
                vista.obtenirTextDescripcio().setText(meta.get("descripcio"));

            if (result.coverBlob != null && existingBlob == null) {
                onCoverFound.accept(result.coverBlob);
                javax.swing.ImageIcon icon = UITheme.scaledIcon(result.coverBlob, 120);
                vista.obtenirLabelPreview().setIcon(icon != null ? icon : presentacio.MemoriaImatgesCoberta.NO_COVER);
            }
        } catch (Exception e) {
            new DialegError(e).mostrarErrorMessage();
        }
    }
}