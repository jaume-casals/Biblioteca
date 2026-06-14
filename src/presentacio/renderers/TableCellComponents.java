package presentacio.renderers;

import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import interficie.BibliotecaWriter;

/**
 * Facade class re-exporting the split renderer/editor classes.
 * Each renderer/editor now lives in its own file; this class keeps
 * backward-compatible imports and provides shared utilities.
 */
public class TableCellComponents {

    // ── Re-exports for backward compatibility ─────────────────────────────────

    public static class CoverCellRenderer extends presentacio.renderers.CoverCellRenderer {
        public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
                Set<Long> coverLoading, BibliotecaWriter cd) {
            super(table, coverCache, coverLoading, cd);
        }
        public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
                Set<Long> coverLoading, BibliotecaWriter cd,
                LongFunction<Integer> isbnToRow) {
            super(table, coverCache, coverLoading, cd, isbnToRow);
        }
    }

    public static class SearchHighlightRenderer extends presentacio.renderers.SearchHighlightRenderer {
        public SearchHighlightRenderer(Supplier<Set<Long>> loanedISBNs) {
            super(loanedISBNs);
        }
    }

    public static class ProgressBarRenderer extends presentacio.renderers.ProgressBarRenderer {}

    public static class LlegitCheckBoxRenderer extends presentacio.renderers.LlegitCheckBoxRenderer {}

    public static class LlegitCheckBoxEditor extends presentacio.renderers.LlegitCheckBoxEditor {
        public LlegitCheckBoxEditor(BibliotecaWriter cd, Consumer<Llibre> onUpdated) {
            super(cd, onUpdated);
        }
    }

    // ── Shared utility methods ────────────────────────────────────────────────

    public static byte[] loadCoverBytes(Llibre l, BibliotecaWriter cd) {
        byte[] blob = l.getImatgeBlob();
        if (blob == null && l.hasBlob())
            blob = cd.getLlibreBlob(l.getISBN());
        if (blob != null) return blob;
        String path = l.getImatge();
        if (path != null && !path.isEmpty()) {
            try { return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
        }
        return null;
    }

    public static ImageIcon scaledCover(byte[] data) {
        if (data == null) return null;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return null;
            int h = 46;
            int w = Math.max(1, (int)(img.getWidth() * (h / (double) img.getHeight())));
            return new ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }
}