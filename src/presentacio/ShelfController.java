package presentacio;

import domini.Llista;
import herramienta.DialogoError;
import herramienta.I18n;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shelf (llista) combo, drag-to-shelf, and list membership actions. */
class ShelfController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final MostrarBibliotecaControl coordinator;

    ShelfController(LibraryViewState state, LibraryScreenHost host, MostrarBibliotecaControl coordinator) {
        this.state = state;
        this.host = host;
        this.coordinator = coordinator;
    }

    void wireListeners() {
        state.vista.getComboLlistes().addActionListener(e -> onLlistaSelected());
        state.vista.getBtnGestioLlistes().addActionListener(e -> obrirGestioLlistes());
    }

    private void inlineRenameShelf(Llista l) {
        Object raw = JOptionPane.showInputDialog(state.vista,
            I18n.t("dlg_rename_llista_prompt", l.getNom()),
            I18n.t("dlg_rename_llista_title"), JOptionPane.PLAIN_MESSAGE, null, null, l.getNom());
        String newNom = raw != null ? raw.toString() : null;
        if (newNom == null || newNom.isBlank()) return;
        try {
            state.cd.renameLlista(l.getId(), newNom.trim());
            refreshComboLlistes();
        } catch (Exception ex) { new herramienta.DialogoError(ex).showErrorMessage(); }
    }

    private void onDragToShelf(int shelfId, List<Long> isbns) {
        for (long isbn : isbns) {
            try { state.cd.addLlibreToLlista(isbn, shelfId, 0.0, false); }
            catch (Exception e) { System.err.println("Drag-to-shelf failed isbn=" + isbn + ": " + e.getMessage()); }
        }
        refreshComboLlistes();
    }

    void onLlistaSelected() {
        Object sel = state.vista.getComboLlistes().getSelectedItem();
        if (sel instanceof Llista) {
            state.currentLlistaId = ((Llista) sel).getId();
            state.biblio = new ArrayList<>(state.cd.getLlibresInLlista(state.currentLlistaId));
            host.pageCtrl().setUseDBPagination(false);
        } else {
            state.currentLlistaId = null;
            state.biblio = new ArrayList<>(state.cd.getAllLlibres());
            host.pageCtrl().setUseDBPagination(state.cd.isLargeLibrary());
        }
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    void refreshComboLlistes() {
        javax.swing.JComboBox<Object> combo = state.vista.getComboLlistes();
        java.awt.event.ActionListener[] listeners = combo.getActionListeners();
        for (java.awt.event.ActionListener al : listeners) combo.removeActionListener(al);
        combo.removeAllItems();
        combo.addItem(I18n.t("lbl_all_lists") + " (...)");

        new javax.swing.SwingWorker<java.util.List<Object>, Void>() {
            @Override protected java.util.List<Object> doInBackground() {
                java.util.List<Object> out = new java.util.ArrayList<>();
                Map<Integer, Integer> counts = state.cd.getAllCountsInLlistes();
                int total = state.cd.getAllLlibres().size();
                java.util.List<Llista> llistes = new ArrayList<>(state.cd.getAllLlistes());
                out.add(counts); out.add(total); out.add(llistes);
                if (state.currentLlistaId != null) {
                    out.add(new ArrayList<>(state.cd.getLlibresInLlista(state.currentLlistaId)));
                } else {
                    out.add(new ArrayList<>(state.cd.getAllLlibres()));
                }
                out.add(state.cd.isLargeLibrary());
                return out;
            }
            @Override protected void done() {
                try {
                    java.util.List<Object> data = get();
                    @SuppressWarnings("unchecked") Map<Integer, Integer> counts = (Map<Integer, Integer>) data.get(0);
                    int total = (Integer) data.get(1);
                    @SuppressWarnings("unchecked") java.util.List<Llista> llistes = (java.util.List<Llista>) data.get(2);
                    @SuppressWarnings("unchecked") java.util.List<domini.Llibre> biblio = (java.util.List<domini.Llibre>) data.get(3);
                    boolean largeLib = (Boolean) data.get(4);

                    combo.removeAllItems();
                    combo.addItem(I18n.t("lbl_all_lists") + " (" + total + ")");
                    for (Llista l : llistes) combo.addItem(l);
                    combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                        @Override
                        public java.awt.Component getListCellRendererComponent(
                                javax.swing.JList<?> list, Object value, int index,
                                boolean isSelected, boolean cellHasFocus) {
                            javax.swing.Icon icon = null;
                            if (value instanceof Llista ll) {
                                if (ll.getColor() != null) {
                                    try {
                                        java.awt.Color c = java.awt.Color.decode(ll.getColor());
                                        icon = new javax.swing.Icon() {
                                            public int getIconWidth()  { return 12; }
                                            public int getIconHeight() { return 12; }
                                            public void paintIcon(java.awt.Component cp, java.awt.Graphics g, int x, int y) {
                                                g.setColor(c);
                                                g.fillRoundRect(x, y + 1, 10, 10, 3, 3);
                                                g.setColor(c.darker());
                                                g.drawRoundRect(x, y + 1, 10, 10, 3, 3);
                                            }
                                        };
                                    } catch (Exception ignored) {}
                                }
                                value = ll.getNom() + " (" + counts.getOrDefault(ll.getId(), 0) + ")";
                            }
                            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            setIcon(icon);
                            return this;
                        }
                    });
                    int selectIdx = 0;
                    if (state.currentLlistaId != null) {
                        for (int i = 1; i < combo.getItemCount(); i++) {
                            Object item = combo.getItemAt(i);
                            if (item instanceof Llista ll && ll.getId() == state.currentLlistaId) {
                                selectIdx = i;
                                break;
                            }
                        }
                        if (selectIdx == 0) state.currentLlistaId = null;
                    }
                    combo.setSelectedIndex(selectIdx);
                    for (java.awt.event.ActionListener al : listeners) combo.addActionListener(al);
                    state.vista.rebuildSidebarShelves(llistes, counts, ShelfController.this::onDragToShelf, ShelfController.this::inlineRenameShelf);
                    state.biblio = biblio;
                    host.pageCtrl().setUseDBPagination(state.currentLlistaId == null && largeLib);
                    host.pageCtrl().setCurrentPage(0);
                    host.showPage(0);
                } catch (Exception ex) {
                    new herramienta.DialogoError(ex).showErrorMessage();
                }
            }
        }.execute();
    }

    void refreshComboTags() {
        javax.swing.JComboBox<Object> combo = state.vista.getComboTagFilter();
        combo.removeAllItems();
        combo.addItem(I18n.t("lbl_all_tags"));
        for (domini.Tag t : state.cd.getAllTags()) combo.addItem(t);
    }

    Llista pickLlista(String prompt) {
        java.util.List<Llista> llistes = state.cd.getAllLlistes();
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
                Llista nova = state.cd.addLlista(name.trim());
                refreshComboLlistes();
                return nova;
            } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); return null; }
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
                long isbn = Long.parseLong((String) t.getValueAt(row, TableController.COL_ISBN));
                state.cd.addLlibreToLlista(isbn, sel.getId(), 0.0, false);
                ok++;
            } catch (Exception ignored) { skip++; }
        }
        String msg = I18n.t("dlg_books_added_to_list", ok, sel.getNom());
        if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
        JOptionPane.showMessageDialog(state.vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
        refreshComboLlistes();
    }

    void afegirLlibresGaleriaALlista(List<domini.Llibre> llibres) {
        Llista sel = pickLlista(I18n.t("dlg_add_to_list_msg", llibres.size()));
        if (sel == null) return;
        int ok = 0, skip = 0;
        for (domini.Llibre l : llibres) {
            try {
                state.cd.addLlibreToLlista(l.getISBN(), sel.getId(), 0.0, false);
                ok++;
            } catch (Exception ignored) { skip++; }
        }
        String msg = I18n.t("dlg_books_added_to_list", ok, sel.getNom());
        if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
        JOptionPane.showMessageDialog(state.vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
        refreshComboLlistes();
    }

    void obrirGestioLlistes() {
        new GestioLlistesDialog(SwingUtilities.getWindowAncestor(state.vista), coordinator, state.cd)
            .setVisible(true);
    }
}
