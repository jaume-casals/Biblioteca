package presentacio;

import interficie.BibliotecaReader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Shared cover-image loading: BLOB → URL → default fallback. Lifted from inline logic in
 * detail panels so all four cover surfaces use the same precedence.
 */
public final class CoverDisplayHelper {
    private CoverDisplayHelper() {}

    public static void loadAndDisplay(long isbn, BibliotecaReader cd, JLabel target, int width, int height) {
        try {
            byte[] blob = cd.getLlibreBlob(isbn);
            if (blob != null && blob.length > 0) {
                ImageIcon icon = new ImageIcon(new ImageIcon(blob).getImage()
                    .getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
                target.setIcon(icon);
                target.setText("");
                return;
            }
        } catch (Exception ignored) {}
        target.setIcon(null);
        target.setText(herramienta.I18n.t("lbl_no_cover"));
    }
}
