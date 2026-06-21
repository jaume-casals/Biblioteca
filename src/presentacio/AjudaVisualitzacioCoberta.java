package presentacio;

import interficie.LectorBiblioteca;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/**
 * Càrrega compartida d'imatges de coberta: BLOB → URL → fallback per defecte.
 * Extreta de la lògica en línia dels panells de detalls perquè les quatre
 * superfícies de coberta facin servir la mateixa precedència.
 *
 * <p>Segons la troballa LOW de tot.txt: la càrrega s'executa en un
 * {@link SwingWorker} perquè una descodificació de coberta de 500 KB ja no
 * entrebanqui l'EDT. El {@code target} es captura per referència; el
 * callback {@code done()} comprova que el JLabel encara mostri el mateix
 * target (la identitat del component, no el seu contingut) mitjançant la
 * referència `this` capturada.
 */
public final class AjudaVisualitzacioCoberta {
    private AjudaVisualitzacioCoberta() {}

    public static void carregarAndDisplay(long isbn, LectorBiblioteca cd, JLabel target, int width, int height) {
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() throws Exception {
                byte[] blob = cd.obtenirLlibreBlob(isbn);
                if (blob == null || blob.length == 0) return null;
                return new ImageIcon(new ImageIcon(blob).getImage()
                    .getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
            }
            @Override protected void done() {
                if (isCancelled() || target == null) return;
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
