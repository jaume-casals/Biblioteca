package presentacio;

import domini.Llibre;
import herramienta.DialegError;
import herramienta.FormatOptions;
import herramienta.I18n;
import presentacio.detalles.control.DetallesLlibrePanelControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;
import presentacio.listener.OnLlibreDelete;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Table and gallery context menus, batch edit, duplicate, loan. */
class ControladorMenuContextual {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final ControladorPrestatgeria shelfCtrl;
    private final ControladorAccionsLlibre bookActionsCtrl;

    ControladorMenuContextual(LibraryViewState state, LibraryScreenHost host,
                          ControladorPrestatgeria shelfCtrl, ControladorAccionsLlibre bookActionsCtrl) {
        this.state = state;
        this.host = host;
        this.shelfCtrl = shelfCtrl;
        this.bookActionsCtrl = bookActionsCtrl;
    }

    MouseAdapter contextMenu() {
        return new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                JTable table = state.vista.getjTableBilio();
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                Object isbnVal = table.getValueAt(row, BibliotecaTableModel.COL_ISBN);
                if (!(isbnVal instanceof String)) return;
                String isbnStr = (String) isbnVal;

                JPopupMenu menu = new JPopupMenu();
                int[] selectedRows = table.getSelectedRows();

                JMenuItem itemObrir = new JMenuItem(I18n.t("menu_open_details"));
                itemObrir.setEnabled(selectedRows.length == 1);
                itemObrir.addActionListener(ev -> bookActionsCtrl.abrirDetallesLlibres());
                menu.add(itemObrir);

                JMenuItem itemEliminar = new JMenuItem(
                    selectedRows.length > 1 ? I18n.t("menu_delete_n", selectedRows.length) : I18n.t("menu_delete_one"));
                itemEliminar.addActionListener(ev -> bookActionsCtrl.eliminarFilaSeleccionada());
                menu.add(itemEliminar);

                JMenuItem itemAfegirLlista = new JMenuItem(
                    selectedRows.length > 1 ? I18n.t("menu_add_to_list_n", selectedRows.length) : I18n.t("menu_add_to_list"));
                itemAfegirLlista.addActionListener(ev -> shelfCtrl.afegirSeleccionatsALlista(selectedRows));
                menu.add(itemAfegirLlista);

                JMenuItem itemDuplicar = new JMenuItem(I18n.t("menu_duplicate"));
                itemDuplicar.setEnabled(selectedRows.length == 1);
                itemDuplicar.addActionListener(ev -> duplicarLlibre(isbnStr));
                menu.add(itemDuplicar);

                if (selectedRows.length > 1) {
                    JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selectedRows.length));
                    List<Long> batchIsbns = new ArrayList<>();
                    for (int r : selectedRows) {
                        Object v = table.getValueAt(r, BibliotecaTableModel.COL_ISBN);
                        if (v instanceof String s) {
                            try { batchIsbns.add(Long.parseLong(s)); } catch (NumberFormatException nfe) { /* skip malformed row */ }
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
                            state.vista.getjTableBilio().repaint();
                        } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
                    });
                    menu.add(itemRetornar);
                } else if (selectedRows.length == 1) {
                    JMenuItem itemPrestar = new JMenuItem(I18n.t("menu_loan_book"));
                    itemPrestar.addActionListener(ev -> prestarLlibre(isbnLong));
                    menu.add(itemPrestar);
                }

                menu.addSeparator();

                JMenuItem itemCopiarISBN = new JMenuItem(I18n.t("menu_copy_isbn"));
                itemCopiarISBN.setEnabled(selectedRows.length == 1);
                itemCopiarISBN.addActionListener(ev ->
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(isbnStr), null));
                menu.add(itemCopiarISBN);

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        };
    }

    void mostrarGaleriaContextMenu(MouseEvent e, List<Llibre> selected) {
        if (selected.isEmpty()) return;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemObrir = new JMenuItem(I18n.t("ctx_open_details"));
        itemObrir.setEnabled(selected.size() == 1);
        itemObrir.addActionListener(ev -> {
            try {
                DetallesLlibrePanelControl d = new DetallesLlibrePanelControl(
                    selected.get(0), state.enActualizarBBDD, state.cd);
                d.obtenirDetallesLlibrePanel().setLocationRelativeTo(state.vista);
                d.obtenirDetallesLlibrePanel().setVisible(true);
            } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
        });
        menu.add(itemObrir);

        JMenuItem itemEliminar = new JMenuItem(
            selected.size() > 1 ? I18n.t("ctx_delete_n", selected.size()) : I18n.t("ctx_delete_one"));
        itemEliminar.addActionListener(ev -> eliminarLlibresGaleria(selected));
        menu.add(itemEliminar);

        JMenuItem itemAfegirLlista = new JMenuItem(
            selected.size() > 1 ? I18n.t("menu_add_to_list_n", selected.size()) : I18n.t("menu_add_to_list"));
        itemAfegirLlista.addActionListener(ev -> shelfCtrl.afegirLlibresGaleriaALlista(selected));
        menu.add(itemAfegirLlista);

        if (selected.size() > 1) {
            JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selected.size()));
            List<Long> batchIsbns = selected.stream().map(Llibre::obtenirISBN).collect(Collectors.toList());
            itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
            menu.add(itemBatchEdit);
        }

        menu.addSeparator();

        JMenuItem itemCopiarISBN = new JMenuItem(I18n.t("ctx_copy_isbn"));
        itemCopiarISBN.setEnabled(selected.size() == 1);
        itemCopiarISBN.addActionListener(ev ->
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(String.valueOf(selected.get(0).obtenirISBN())), null));
        menu.add(itemCopiarISBN);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    void eliminarLlibresGaleria(List<Llibre> llibres) {
        if (llibres.isEmpty()) return;
        String msg = llibres.size() == 1
            ? I18n.t("dlg_confirm_galeria_delete_one", llibres.get(0).obtenirNom())
            : I18n.t("dlg_confirm_galeria_delete_n", llibres.size());
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Long> isbns = llibres.stream().map(Llibre::obtenirISBN).collect(Collectors.toList());
        for (long isbn : isbns) {
            try {
                Llibre l = state.cd.obtenirLlibre(isbn);
                OnLlibreDelete.EsborrarEvent ev = new OnLlibreDelete.EsborrarEvent(l, true);
                state.enActualizarBBDD.onBookDeleting(ev);
                if (!OnLlibreDelete.hauriaProceed(ev)) continue;
                state.undoBuffer.push(l);
                if (state.undoBuffer.size() > LibraryViewState.UNDO_MAX) state.undoBuffer.removeLast();
                state.cd.eliminarLlibre(l);
                bookActionsCtrl.eliminarFila(l);
            } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
        }
        ArrayList<Llibre> toShow = host.pageCtrl().esPaginatedMode()
            ? new ArrayList<>(state.biblio.subList(
                host.pageCtrl().obtenirCurrentPage() * ControladorPaginaTaula.PAGE_SIZE,
                Math.min((host.pageCtrl().obtenirCurrentPage() + 1) * ControladorPaginaTaula.PAGE_SIZE, state.biblio.size())))
            : new ArrayList<>(state.biblio);
        state.vista.obtenirGaleria().actualitzarLlibres(toShow);
    }

    void batchEdit(List<Long> isbns) {
        String noChange = I18n.t("batch_no_change");
        String[] formatejarOpts = FormatOptions.withNoChange();
        String[] llegitOpts = {noChange, I18n.t("filter_llegit_chk"), I18n.t("filter_no_llegit_chk")};
        javax.swing.JComboBox<String> comboFormat = new javax.swing.JComboBox<>(formatejarOpts);
        javax.swing.JComboBox<String> comboLlegit = new javax.swing.JComboBox<>(llegitOpts);
        List<domini.Llista> llistes = state.cd.obtenirAllLlistes();
        String[] llistaOpts = new String[llistes.size() + 1];
        llistaOpts[0] = I18n.t("batch_no_add_list");
        for (int i = 0; i < llistes.size(); i++) llistaOpts[i + 1] = llistes.get(i).obtenirNom();
        javax.swing.JComboBox<String> comboLlista = new javax.swing.JComboBox<>(llistaOpts);
        Object[] fields = {
            I18n.t("batch_field_format"), comboFormat,
            I18n.t("batch_field_llegit"), comboLlegit,
            I18n.t("batch_field_add_list"), comboLlista
        };
        int result = JOptionPane.showConfirmDialog(state.vista, fields,
            I18n.t("dlg_batch_edit_title", isbns.size()), JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String selFormat = (String) comboFormat.getSelectedItem();
        int llegitIdx = comboLlegit.getSelectedIndex();
        int selLlistaIdx = comboLlista.getSelectedIndex();
        domini.Llista selLlista = selLlistaIdx > 0 ? llistes.get(selLlistaIdx - 1) : null;
        for (long isbn : isbns) {
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
            } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
        }
        host.refrescarAll();
    }

    void duplicarLlibre(String isbnStr) {
        try {
            Llibre src = state.cd.obtenirLlibre(Long.parseLong(isbnStr));
            Llibre copy = Llibre.copyOf(src);
            GuardarLlibresDialogo dialeg = new GuardarLlibresDialogo();
            new GuardarLlibresDialogoControl(dialeg, null, state.cd);
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
        } catch (Exception e) {
            new DialegError(e).mostrarErrorMessage();
        }
    }

    void prestarLlibre(long isbn) {
        String nom = JOptionPane.showInputDialog(state.vista,
            I18n.t("dlg_loan_msg"), I18n.t("dlg_loan_dialog_title"), JOptionPane.QUESTION_MESSAGE);
        if (nom == null || nom.isBlank()) return;
        try {
            state.cd.prestarLlibre(isbn, nom.trim());
            state.loanedISBNs = state.cd.obtenirLoanedISBNs();
            host.tableCtrl().posarLoanedISBNs(state.loanedISBNs);
            state.vista.getjTableBilio().repaint();
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_loan_done", nom.trim()),
                I18n.t("dlg_loan_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
    }
}
