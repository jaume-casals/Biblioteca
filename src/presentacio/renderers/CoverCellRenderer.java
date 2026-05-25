package presentacio.renderers;

import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.UITheme;
import interficie.BibliotecaWriter;
import presentacio.MainFrameControl;

public class CoverCellRenderer extends JLabel implements TableCellRenderer {
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    private static final int COLUMNA_ISBN = 1;
    private final Map<Long, ImageIcon> coverCache;
    private final Set<Long> coverLoading;
    private final BibliotecaWriter cd;
    private final JTable table;

    public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
            Set<Long> coverLoading, BibliotecaWriter cd) {
        this.table = table;
        this.coverCache = coverCache;
        this.coverLoading = coverLoading;
        this.cd = cd;
        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
        setIcon(null);
        try {
            long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
            ImageIcon icon = coverCache.get(isbn);
            if (icon != null) {
                setIcon(icon);
            } else if (!coverLoading.contains(isbn)) {
                coverLoading.add(isbn);
                final int r = row, c = col;
                LOADER.submit(() -> {
                    Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
                    ImageIcon img = l != null ? TableCellComponents.scaledCover(TableCellComponents.loadCoverBytes(l, cd)) : null;
                    if (img != null) coverCache.put(isbn, img);
                    coverLoading.remove(isbn);
                    SwingUtilities.invokeLater(() -> {
                        if (r < table.getRowCount())
                            table.repaint(table.getCellRect(r, c, false));
                    });
                });
            }
        } catch (Exception ignored) {}
        return this;
    }
}