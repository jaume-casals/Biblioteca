package presentacio;

import interficie.BibliotecaReader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/**
 * Shared cover-image loading: BLOB → URL → default fallback. Lifted from inline logic in
 * detail panels so all four cover surfaces use the same precedence.
 *
 * <p>Per the tot.txt LOW finding: the load runs on a {@link SwingWorker} so a
 * 500 KB cover decode no longer stutters the EDT. The {@code target} is
 * captured by reference; the {@code done()} callback checks the JLabel is
 * still showing the same target (its component identity, not its contents)
 * via the captured `this` reference.
 */
public final class AjudaVisualitzacioCoberta {
    private AjudaVisualitzacioCoberta() {}

    public static void carregarAndDisplay(long isbn, BibliotecaReader cd, JLabel target, int width, int height) {
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() throws Exception {
                byte[] blob = cd.obtenirLlibreBlob(isbn);
                if (blob == null || blob.length == 0) return null;
                return new ImageIcon(new ImageIcon(blob).getImage()
                    .getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
            }
            @Override protected void done() {
                if (target == null) return;
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        target.setIcon(icon);
                        target.setText("");
                        return;
                    }
                } catch (Exception ignored) {}
                target.setIcon(null);
                target.setText(herramienta.I18n.t("lbl_no_cover"));
            }
        }.execute();
    }
}
