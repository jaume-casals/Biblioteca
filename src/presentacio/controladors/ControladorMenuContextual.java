package presentacio.controladors;

import domini.Llibre;
import herramienta.i18n.I18n;
import herramienta.text.FormatOptions;
import herramienta.ui.DialegError;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import presentacio.detalles.control.ControladorDialegDesarLlibres;
import presentacio.detalles.control.ControladorPanellDetallsLlibre;
import presentacio.detalles.vista.DialegDesarLlibres;
import presentacio.listener.EnEliminarLlibre;
import presentacio.models.ModelTaulaBiblioteca;
import presentacio.util.AmfitrioPantallaBiblioteca;
import presentacio.util.EstatVistaBiblioteca;


/** Menús contextuals de taula i galeria, edició per lots, duplicar, prèstec. */
public class ControladorMenuContextual {

    private final EstatVistaBiblioteca state;
    private final AmfitrioPantallaBiblioteca host;
    private final ControladorPrestatgeria shelfCtrl;
    private final ControladorAccionsLlibre bookActionsCtrl;

    ControladorMenuContextual(EstatVistaBiblioteca state, AmfitrioPantallaBiblioteca host,
                          ControladorPrestatgeria shelfCtrl, ControladorAccionsLlibre bookActionsCtrl) {
        this.state = state;
        this.host = host;
        this.shelfCtrl = shelfCtrl;
        this.bookActionsCtrl = bookActionsCtrl;
    }

    private void addMenuItem(JPopupMenu menu, String labelKey, boolean enabled, Runnable action) {
        addItem(menu, I18n.t(labelKey), enabled, action);
    }

    private void addItem(JPopupMenu menu, String label, boolean enabled, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setEnabled(enabled);
        item.addActionListener(ev -> action.run());
        menu.add(item);
    }

    private void copyIsbnToClipboard(String isbn) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(isbn), null);
    }

    MouseAdapter contextMenu() {
        return new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                JTable table = state.vista.obtenirTaulaLlibres();
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                Object isbnVal = table.getValueAt(row, ModelTaulaBiblioteca.COL_ISBN);
                if (!(isbnVal instanceof String)) return;
                String isbnStr = (String) isbnVal;

                JPopupMenu menu = new JPopupMenu();
                int[] selectedRows = table.getSelectedRows();

                addMenuItem(menu, "menu_open_details", selectedRows.length == 1,
                    () -> bookActionsCtrl.abrirDetallesLlibres());
                addItem(menu,
                    selectedRows.length > 1 ? I18n.t("menu_delete_n", selectedRows.length) : I18n.t("menu_delete_one"),
                    true, () -> bookActionsCtrl.eliminarFilaSeleccionada());
                addItem(menu,
                    selectedRows.length > 1 ? I18n.t("menu_add_to_list_n", selectedRows.length) : I18n.t("menu_add_to_list"),
                    true, () -> shelfCtrl.afegirSeleccionatsALlista(selectedRows));
                addMenuItem(menu, "menu_duplicate", selectedRows.length == 1,
                    () -> duplicarLlibre(isbnStr));

                if (selectedRows.length > 1) {
                    JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selectedRows.length));
                    List<Long> batchIsbns = new ArrayList<>();
                    for (int r : selectedRows) {
                        Object v = table.getValueAt(r, ModelTaulaBiblioteca.COL_ISBN);
                        if (v instanceof String s) {
                            try { batchIsbns.add(Long.parseLong(s)); } catch (NumberFormatException nfe) { /* salta la fila mal formada */ }
                        }
                    }
                    itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
                    menu.add(itemBatchEdit);
                }

                menu.addSeparator();

                long isbnLong;
                try { isbnLong = Long.parseLong(isbnStr); }
                catch (NumberFormatException nfe) { return; }
                boolean loaned = state.loanedISBNs.contains(isbnLong);
                if (selectedRows.length == 1 && loaned) {
                    JMenuItem itemRetornar = new JMenuItem(I18n.t("menu_return_book"));
                    itemRetornar.addActionListener(ev -> {
                        try {
                            state.cd.retornarLlibre(isbnLong);
                            state.loanedISBNs = state.cd.obtenirLoanedISBNs();
                            host.tableCtrl().posarLoanedISBNs(state.loanedISBNs);
                            state.vista.obtenirTaulaLlibres().repaint();
                        } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
                    });
                    menu.add(itemRetornar);
                } else if (selectedRows.length == 1) {
                    JMenuItem itemPrestar = new JMenuItem(I18n.t("menu_loan_book"));
                    itemPrestar.addActionListener(ev -> prestarLlibre(isbnLong));
                    menu.add(itemPrestar);
                }

                menu.addSeparator();

                addItem(menu, I18n.t("menu_copy_isbn"), selectedRows.length == 1,
                    () -> copyIsbnToClipboard(isbnStr));

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        };
    }

    void mostrarGaleriaContextMenu(MouseEvent e, List<Llibre> selected) {
        if (selected.isEmpty()) return;
        JPopupMenu menu = new JPopupMenu();

        addItem(menu, I18n.t("ctx_open_details"), selected.size() == 1, () -> {
            try {
                ControladorPanellDetallsLlibre d = new ControladorPanellDetallsLlibre(
                    selected.get(0), state.enActualizarBBDD, state.cd);
                d.obtenirDetallesLlibrePanel().setLocationRelativeTo(state.vista);
                d.obtenirDetallesLlibrePanel().setVisible(true);
            } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
        });
        addItem(menu,
            selected.size() > 1 ? I18n.t("ctx_delete_n", selected.size()) : I18n.t("ctx_delete_one"),
            true, () -> eliminarLlibresGaleria(selected));
        addItem(menu,
            selected.size() > 1 ? I18n.t("menu_add_to_list_n", selected.size()) : I18n.t("menu_add_to_list"),
            true, () -> shelfCtrl.afegirLlibresGaleriaALlista(selected));

        if (selected.size() > 1) {
            JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selected.size()));
            List<Long> batchIsbns = selected.stream().map(Llibre::obtenirISBN).collect(Collectors.toList());
            itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
            menu.add(itemBatchEdit);
        }

        menu.addSeparator();

        addItem(menu, I18n.t("ctx_copy_isbn"), selected.size() == 1,
            () -> copyIsbnToClipboard(String.valueOf(selected.get(0).obtenirISBN())));

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    void eliminarLlibresGaleria(List<Llibre> llibres) {
        if (llibres.isEmpty()) return;
        String msg = llibres.size() == 1
            ? I18n.t("dlg_confirm_galeria_delete_one", llibres.get(0).obtenirNom())
            : I18n.t("dlg_confirm_galeria_delete_n", llibres.size());
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        final List<Long> isbns = llibres.stream().map(Llibre::obtenirISBN).collect(Collectors.toList());
        new SwingWorker<List<Llibre>, Void>() {
            @Override protected List<Llibre> doInBackground() {
                List<Llibre> toPush = new java.util.ArrayList<>();
                for (long isbn : isbns) {
                    try {
                        Llibre l = state.cd.obtenirLlibre(isbn);
                        EnEliminarLlibre.EsborrarEvent ev = new EnEliminarLlibre.EsborrarEvent(l, true);
                        state.enActualizarBBDD.enEliminantLlibre(ev);
                        if (!EnEliminarLlibre.hauriaProceed(ev)) continue;
                        toPush.add(l);
                        state.cd.eliminarLlibre(l);
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> new DialegError(e).mostrarErrorMessage());
                    }
                }
                return toPush;
            }
            @Override protected void done() {
                if (isCancelled()) return;
                List<Llibre> toPush;
                try { toPush = get(); }
                catch (Exception e) { new DialegError(e).mostrarErrorMessage(); return; }
                bookActionsCtrl.finalizeDeleteWithUndo(toPush, isbns);
                ArrayList<Llibre> toShow = host.pageCtrl().esPaginatedMode()
                    ? new ArrayList<>(state.biblio.subList(
                        host.pageCtrl().obtenirCurrentPage() * ControladorPaginaTaula.PAGE_SIZE,
                        Math.min((host.pageCtrl().obtenirCurrentPage() + 1) * ControladorPaginaTaula.PAGE_SIZE, state.biblio.size())))
                    : new ArrayList<>(state.biblio);
                state.vista.obtenirGaleria().actualitzarLlibres(toShow);
            }
        }.execute();
    }

    void batchEdit(List<Long> isbns) {
        String noChange = I18n.t("batch_no_change");
        String[] formatejarOpts = FormatOptions.withNoChange();
        String[] llegitOpts = {noChange, I18n.t("filter_llegit_chk"), I18n.t("filter_no_llegit_chk")};
        javax.swing.JComboBox<String> comboFormat = new javax.swing.JComboBox<>(formatejarOpts);
        javax.swing.JComboBox<String> comboLlegit = new javax.swing.JComboBox<>(llegitOpts);
        final List<domini.Llista> llistes;
        final String[] llistaOpts;
        {
            List<domini.Llista> tmp = state.cd.obtenirAllLlistes();
            llistes = tmp;
            llistaOpts = new String[tmp.size() + 1];
            llistaOpts[0] = I18n.t("batch_no_add_list");
            for (int i = 0; i < tmp.size(); i++) llistaOpts[i + 1] = tmp.get(i).obtenirNom();
        }
        javax.swing.JComboBox<String> comboLlista = new javax.swing.JComboBox<>(llistaOpts);
        Object[] fields = {
            I18n.t("batch_field_format"), comboFormat,
            I18n.t("batch_field_llegit"), comboLlegit,
            I18n.t("batch_field_add_list"), comboLlista
        };
        int result = JOptionPane.showConfirmDialog(state.vista, fields,
            I18n.t("dlg_batch_edit_title", isbns.size()), JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        final String selFormat = (String) comboFormat.getSelectedItem();
        final int llegitIdx = comboLlegit.getSelectedIndex();
        final int selLlistaIdx = comboLlista.getSelectedIndex();
        final domini.Llista selLlista = selLlistaIdx > 0 ? llistes.get(selLlistaIdx - 1) : null;
        final List<Long> toEdit = isbns;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                for (long isbn : toEdit) {
                    try {
                        Llibre l = state.cd.obtenirLlibre(isbn);
                        if (l == null) continue;
                        if (!noChange.equals(selFormat)) l.posarFormat(selFormat);
                        if (llegitIdx == 1) l.posarLlegit(true);
                        else if (llegitIdx == 2) l.posarLlegit(false);
                        state.cd.actualitzarLlibre(l);
                        if (selLlista != null) {
                            try { state.cd.afegirLlibreToLlista(isbn, selLlista.obtenirId(), 0.0, false); }
                            catch (Exception ignored) {}
                        }
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> new DialegError(e).mostrarErrorMessage());
                    }
                }
                return null;
            }
            @Override protected void done() {
                if (isCancelled()) return;
                host.refrescarAll();
            }
        }.execute();
    }

    void duplicarLlibre(String isbnStr) {
        new SwingWorker<Llibre, Void>() {
            @Override protected Llibre doInBackground() {
                return state.cd.obtenirLlibre(Long.parseLong(isbnStr));
            }
            @Override protected void done() {
                if (isCancelled()) return;
                Llibre src;
                try { src = get(); }
                catch (Exception e) {
                    new DialegError(e).mostrarErrorMessage();
                    return;
                }
                Llibre copy = Llibre.copyOf(src);
                DialegDesarLlibres dialeg = new DialegDesarLlibres();
                new ControladorDialegDesarLlibres(dialeg, null, state.cd);
                dialeg.obtenirTextNom().setText(copy.obtenirNom() != null ? copy.obtenirNom() : "");
                dialeg.obtenirTextAutor().setText(copy.obtenirAutor() != null ? copy.obtenirAutor() : "");
                dialeg.obtenirTextAny().setText(copy.obtenirAny() != null && copy.obtenirAny() != 0 ? String.valueOf(copy.obtenirAny()) : "");
                dialeg.obtenirTextDescripcio().setText(copy.obtenirDescripcio() != null ? copy.obtenirDescripcio() : "");
                dialeg.obtenirTextValoracio().setText(copy.obtenirValoracio() != null && copy.obtenirValoracio() != 0.0 ? String.valueOf(copy.obtenirValoracio()) : "");
                dialeg.obtenirTextPreu().setText(copy.obtenirPreu() != null && copy.obtenirPreu() != 0.0 ? String.valueOf(copy.obtenirPreu()) : "");
                dialeg.obtenirTextEditorial().setText(copy.obtenirEditorial());
                dialeg.obtenirTextSerie().setText(copy.obtenirSerie());
                dialeg.obtenirTextVolum().setText(copy.obtenirVolum() > 0 ? String.valueOf(copy.obtenirVolum()) : "");
                dialeg.obtenirTextIdioma().setText(copy.obtenirIdioma() != null ? copy.obtenirIdioma() : "");
                dialeg.obtenirChckLlegit().setSelected(Boolean.TRUE.equals(copy.obtenirLlegit()));
                dialeg.obtenirChckDesitjat().setSelected(copy.esDesitjat());
                dialeg.obtenirTextISBN().setText("");
                dialeg.obtenirTextISBN().requestFocusInWindow();
                dialeg.setLocationRelativeTo(state.vista);
                dialeg.setVisible(true);
                host.refrescarAll();
            }
        }.execute();
    }

    void prestarLlibre(long isbn) {
        String nom = JOptionPane.showInputDialog(state.vista,
            I18n.t("dlg_loan_msg"), I18n.t("dlg_loan_dialog_title"), JOptionPane.QUESTION_MESSAGE);
        if (nom == null || nom.isBlank()) return;
        try {
            state.cd.prestarLlibre(isbn, nom.trim());
            state.loanedISBNs = state.cd.obtenirLoanedISBNs();
            host.tableCtrl().posarLoanedISBNs(state.loanedISBNs);
            state.vista.obtenirTaulaLlibres().repaint();
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_loan_done", nom.trim()),
                I18n.t("dlg_loan_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
    }
}



