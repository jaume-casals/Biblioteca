package presentacio;

import domini.Llista;
import herramienta.DialegError;
import herramienta.I18n;
import interficie.ShelfWriter;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shelf (llista) combo, drag-to-shelf, and list membership actions. */
class ControladorPrestatgeria {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final ControladorMostrarBiblioteca coordinator;

    ControladorPrestatgeria(LibraryViewState state, LibraryScreenHost host, ControladorMostrarBiblioteca coordinator) {
        this.state = state;
        this.host = host;
        this.coordinator = coordinator;
    }

    void wireListeners() {
        state.vista.obtenirComboLlistes().addActionListener(e -> onLlistaSelected());
        state.vista.obtenirBtnGestioLlistes().addActionListener(e -> obrirGestioLlistes());
    }

    private void inlineRenameShelf(Llista l) {
        Object raw = JOptionPane.showInputDialog(state.vista,
            I18n.t("dlg_rename_llista_prompt", l.obtenirNom()),
            I18n.t("dlg_rename_llista_title"), JOptionPane.PLAIN_MESSAGE, null, null, l.obtenirNom());
        String newNom = raw != null ? raw.toString() : null;
        if (newNom == null || newNom.isBlank()) return;
        try {
            state.cd.reanomenarLlista(l.obtenirId(), newNom.trim());
            refrescarComboLlistes();
        } catch (Exception ex) { new herramienta.DialegError(ex).mostrarErrorMessage(); }
    }

    private void onDragToShelf(int shelfId, List<Long> isbns) {
        for (long isbn : isbns) {
            try { state.cd.afegirLlibreToLlista(isbn, shelfId, 0.0, false); }
            catch (Exception e) {
                java.util.logging.Logger.getLogger(ControladorPrestatgeria.class.getName())
                    .warning("Drag-to-shelf failed isbn=" + isbn + ": " + e.getMessage());
            }
        }
        refrescarComboLlistes();
    }

    void onLlistaSelected() {
        Object sel = state.vista.obtenirComboLlistes().getSelectedItem();
        if (sel instanceof Llista) {
            state.currentLlistaId = ((Llista) sel).obtenirId();
            state.biblio = new ArrayList<>(state.cd.obtenirLlibresInLlista(state.currentLlistaId));
            host.pageCtrl().posarUseDBPagination(false);
        } else {
            state.currentLlistaId = null;
            state.biblio = new ArrayList<>(state.cd.obtenirAllLlibres());
            host.pageCtrl().posarUseDBPagination(state.cd.esLargeLibrary());
        }
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    void refrescarComboLlistes() {
        javax.swing.JComboBox<Object> combo = state.vista.obtenirComboLlistes();
        java.awt.event.ActionListener[] listeners = combo.getActionListeners();
        for (java.awt.event.ActionListener al : listeners) combo.removeActionListener(al);
        combo.removeAllItems();
        combo.addItem(I18n.t("lbl_all_lists") + " (...)");

        new javax.swing.SwingWorker<DadesRefresc, Void>() {
            @Override protected DadesRefresc doInBackground() {
                Map<Integer, Integer> counts = state.cd.obtenirAllCountsInLlistes();
                int total = state.cd.comptarLlibresDB();
                java.util.List<Llista> llistes = new ArrayList<>(state.cd.obtenirAllLlistes());
                java.util.List<domini.Llibre> biblio = state.currentLlistaId != null
                    ? new ArrayList<>(state.cd.obtenirLlibresInLlista(state.currentLlistaId))
                    : new ArrayList<>(state.cd.obtenirAllLlibres());
                boolean largeLib = state.cd.esLargeLibrary();
                return new DadesRefresc(counts, total, llistes, biblio, largeLib);
            }
            @Override protected void done() {
                try {
                    DadesRefresc data = get();
                    Map<Integer, Integer> counts = data.counts();
                    int total = data.total();
                    java.util.List<Llista> llistes = data.llistes();
                    java.util.List<domini.Llibre> biblio = data.biblio();
                    boolean largeLib = data.large();

                    combo.removeAllItems();
                    combo.addItem(I18n.t("lbl_all_lists") + " (" + total + ")");
                    for (Llista l : llistes) combo.addItem(l);
                    if (comboRenderer == null) {
                        comboRenderer = new RenderitzadorCellaPrestatgeria(counts);
                        combo.setRenderer(comboRenderer);
                    } else {
                        comboRenderer.actualitzarCounts(counts);
                    }
                    int selectIdx = 0;
                    if (state.currentLlistaId != null) {
                        for (int i = 1; i < combo.getItemCount(); i++) {
                            Object item = combo.getItemAt(i);
                            if (item instanceof Llista ll && ll.obtenirId() == state.currentLlistaId) {
                                selectIdx = i;
                                break;
                            }
                        }
                        if (selectIdx == 0) state.currentLlistaId = null;
                    }
                    combo.setSelectedIndex(selectIdx);
                    for (java.awt.event.ActionListener al : listeners) combo.addActionListener(al);
                    state.vista.rebuildSidebarShelves(llistes, counts, ControladorPrestatgeria.this::onDragToShelf, ControladorPrestatgeria.this::inlineRenameShelf);
                    state.biblio = biblio;
                    host.pageCtrl().posarUseDBPagination(state.currentLlistaId == null && largeLib);
                    host.pageCtrl().posarCurrentPage(0);
                    host.mostrarPage(0);
                } catch (Exception ex) {
                    new herramienta.DialegError(ex).mostrarErrorMessage();
                }
            }
        }.execute();
    }

    private record DadesRefresc(Map<Integer, Integer> counts, int total,
                               java.util.List<Llista> llistes, java.util.List<domini.Llibre> biblio,
                               boolean large) {}

    private static final class RenderitzadorCellaPrestatgeria extends javax.swing.DefaultListCellRenderer {
        private static final java.util.Map<java.awt.Color, javax.swing.Icon> COLOR_ICON_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();
        private Map<Integer, Integer> counts;
        RenderitzadorCellaPrestatgeria(Map<Integer, Integer> counts) { this.counts = counts; }
        void actualitzarCounts(Map<Integer, Integer> c) { this.counts = c; }
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            javax.swing.Icon icon = null;
            if (value instanceof Llista ll) {
                if (ll.getColor() != null) {
                    try {
                        final java.awt.Color c = java.awt.Color.decode(ll.getColor());
                        icon = COLOR_ICON_CACHE.computeIfAbsent(c, color -> new javax.swing.Icon() {
                            public int getIconWidth()  { return 12; }
                            public int getIconHeight() { return 12; }
                            public void paintIcon(java.awt.Component cp, java.awt.Graphics g, int x, int y) {
                                g.setColor(color);
                                g.fillRoundRect(x, y + 1, 10, 10, 3, 3);
                                g.setColor(color.darker());
                                g.drawRoundRect(x, y + 1, 10, 10, 3, 3);
                            }
                        });
                    } catch (Exception ignored) {}
                }
                value = ll.obtenirNom() + " (" + counts.getOrDefault(ll.obtenirId(), 0) + ")";
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setIcon(icon);
            return this;
        }
    }
    private RenderitzadorCellaPrestatgeria comboRenderer;

    void refrescarComboTags() {
        javax.swing.JComboBox<Object> combo = state.vista.obtenirComboTagFilter();
        combo.removeAllItems();
        combo.addItem(I18n.t("lbl_all_tags"));
        for (domini.Tag t : state.cd.obtenirAllTags()) combo.addItem(t);
    }

    Llista pickLlista(String prompt) {
        java.util.List<Llista> llistes = state.cd.obtenirAllLlistes();
        Object[] options = new Object[llistes.size() + 1];
        for (int i = 0; i < llistes.size(); i++) options[i] = llistes.get(i);
        options[llistes.size()] = I18n.t("menu_create_list");
        Object sel = JOptionPane.showInputDialog(state.vista, prompt, I18n.t("menu_add_to_list_title"),
            JOptionPane.QUESTION_MESSAGE, null, options,
            options.length > 1 ? options[0] : options[0]);
        if (sel == null) return null;
        if (sel instanceof String) {
            String name = JOptionPane.showInputDialog(state.vista, I18n.t("dlg_new_list_title"),
                I18n.t("dlg_new_list_title"), JOptionPane.QUESTION_MESSAGE);
            if (name == null || name.isBlank()) return null;
            try {
                Llista nova = state.cd.afegirLlista(name.trim());
                refrescarComboLlistes();
                return nova;
            } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); return null; }
        }
        return (Llista) sel;
    }

    void afegirSeleccionatsALlista(int[] rows) {
        Llista sel = pickLlista(I18n.t("dlg_add_to_list_msg", rows.length));
        if (sel == null) return;
        int ok = 0, skip = 0;
        JTable t = state.vista.getjTableBilio();
        for (int row : rows) {
            try {
                long isbn = Long.parseLong((String) t.getValueAt(row, BibliotecaTableModel.COL_ISBN));
                state.cd.afegirLlibreToLlista(isbn, sel.obtenirId(), 0.0, false);
                ok++;
            } catch (Exception ignored) { skip++; }
        }
        String msg = I18n.t("dlg_books_added_to_list", ok, sel.obtenirNom());
        if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
        JOptionPane.showMessageDialog(state.vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
        refrescarComboLlistes();
    }

    void afegirLlibresGaleriaALlista(List<domini.Llibre> llibres) {
        Llista sel = pickLlista(I18n.t("dlg_add_to_list_msg", llibres.size()));
        if (sel == null) return;
        int ok = 0, skip = 0;
        for (domini.Llibre l : llibres) {
            try {
                state.cd.afegirLlibreToLlista(l.obtenirISBN(), sel.obtenirId(), 0.0, false);
                ok++;
            } catch (Exception ignored) { skip++; }
        }
        String msg = I18n.t("dlg_books_added_to_list", ok, sel.obtenirNom());
        if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
        JOptionPane.showMessageDialog(state.vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
        refrescarComboLlistes();
    }

    void obrirGestioLlistes() {
        new DialegGestioLlistes(SwingUtilities.getWindowAncestor(state.vista), coordinator, (ShelfWriter) state.cd)
            .setVisible(true);
    }
}
