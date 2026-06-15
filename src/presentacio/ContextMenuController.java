package presentacio;

import domini.Llibre;
import herramienta.DialogoError;
import herramienta.FormatOptions;
import herramienta.I18n;
import presentacio.detalles.control.DetallesLlibrePanelControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

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
class ContextMenuController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final ShelfController shelfCtrl;
    private final BookActionsController bookActionsCtrl;

    ContextMenuController(LibraryViewState state, LibraryScreenHost host,
                          ShelfController shelfCtrl, BookActionsController bookActionsCtrl) {
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
                            state.loanedISBNs = state.cd.getLoanedISBNs();
                            host.tableCtrl().setLoanedISBNs(state.loanedISBNs);
                            state.vista.getjTableBilio().repaint();
                        } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
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

    void showGaleriaContextMenu(MouseEvent e, List<Llibre> selected) {
        if (selected.isEmpty()) return;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemObrir = new JMenuItem(I18n.t("ctx_open_details"));
        itemObrir.setEnabled(selected.size() == 1);
        itemObrir.addActionListener(ev -> {
            try {
                DetallesLlibrePanelControl d = new DetallesLlibrePanelControl(
                    selected.get(0), state.enActualizarBBDD, state.cd);
                d.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
                d.getDetallesLlibrePanel().setVisible(true);
            } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
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
            List<Long> batchIsbns = selected.stream().map(Llibre::getISBN).collect(Collectors.toList());
            itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
            menu.add(itemBatchEdit);
        }

        menu.addSeparator();

        JMenuItem itemCopiarISBN = new JMenuItem(I18n.t("ctx_copy_isbn"));
        itemCopiarISBN.setEnabled(selected.size() == 1);
        itemCopiarISBN.addActionListener(ev ->
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(String.valueOf(selected.get(0).getISBN())), null));
        menu.add(itemCopiarISBN);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    void eliminarLlibresGaleria(List<Llibre> llibres) {
        if (llibres.isEmpty()) return;
        String msg = llibres.size() == 1
            ? I18n.t("dlg_confirm_galeria_delete_one", llibres.get(0).getNom())
            : I18n.t("dlg_confirm_galeria_delete_n", llibres.size());
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Long> isbns = llibres.stream().map(Llibre::getISBN).collect(Collectors.toList());
        for (long isbn : isbns) {
            try {
                Llibre l = state.cd.getLlibre(isbn);
                state.undoBuffer.push(l);
                if (state.undoBuffer.size() > LibraryViewState.UNDO_MAX) state.undoBuffer.removeLast();
                state.cd.deleteLlibre(l);
                bookActionsCtrl.eliminarFila(l);
            } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
        }
        ArrayList<Llibre> toShow = host.pageCtrl().isPaginatedMode()
            ? new ArrayList<>(state.biblio.subList(
                host.pageCtrl().getCurrentPage() * TablePageController.PAGE_SIZE,
                Math.min((host.pageCtrl().getCurrentPage() + 1) * TablePageController.PAGE_SIZE, state.biblio.size())))
            : new ArrayList<>(state.biblio);
        state.vista.getGaleria().updateLlibres(toShow);
    }

    void batchEdit(List<Long> isbns) {
        String noChange = I18n.t("batch_no_change");
        String[] formatOpts = FormatOptions.withNoChange();
        String[] llegitOpts = {noChange, I18n.t("filter_llegit_chk"), I18n.t("filter_no_llegit_chk")};
        javax.swing.JComboBox<String> comboFormat = new javax.swing.JComboBox<>(formatOpts);
        javax.swing.JComboBox<String> comboLlegit = new javax.swing.JComboBox<>(llegitOpts);
        List<domini.Llista> llistes = state.cd.getAllLlistes();
        String[] llistaOpts = new String[llistes.size() + 1];
        llistaOpts[0] = I18n.t("batch_no_add_list");
        for (int i = 0; i < llistes.size(); i++) llistaOpts[i + 1] = llistes.get(i).getNom();
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
                Llibre l = state.cd.getLlibre(isbn);
                if (l == null) continue;
                if (!noChange.equals(selFormat)) l.setFormat(selFormat);
                if (llegitIdx == 1) l.setLlegit(true);
                else if (llegitIdx == 2) l.setLlegit(false);
                state.cd.updateLlibre(l);
                if (selLlista != null) {
                    try { state.cd.addLlibreToLlista(isbn, selLlista.getId(), 0.0, false); }
                    catch (Exception ignored) {}
                }
            } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
        }
        host.refreshAll();
    }

    void duplicarLlibre(String isbnStr) {
        try {
            Llibre src = state.cd.getLlibre(Long.parseLong(isbnStr));
            Llibre copy = Llibre.copyOf(src);
            GuardarLlibresDialogo dialeg = new GuardarLlibresDialogo();
            new GuardarLlibresDialogoControl(dialeg, null, state.cd);
            dialeg.getTextNom().setText(copy.getNom() != null ? copy.getNom() : "");
            dialeg.getTextAutor().setText(copy.getAutor() != null ? copy.getAutor() : "");
            dialeg.getTextAny().setText(copy.getAny() != null && copy.getAny() != 0 ? String.valueOf(copy.getAny()) : "");
            dialeg.getTextDescripcio().setText(copy.getDescripcio() != null ? copy.getDescripcio() : "");
            dialeg.getTextValoracio().setText(copy.getValoracio() != null && copy.getValoracio() != 0.0 ? String.valueOf(copy.getValoracio()) : "");
            dialeg.getTextPreu().setText(copy.getPreu() != null && copy.getPreu() != 0.0 ? String.valueOf(copy.getPreu()) : "");
            dialeg.getTextEditorial().setText(copy.getEditorial());
            dialeg.getTextSerie().setText(copy.getSerie());
            dialeg.getTextVolum().setText(copy.getVolum() > 0 ? String.valueOf(copy.getVolum()) : "");
            dialeg.getTextIdioma().setText(copy.getIdioma() != null ? copy.getIdioma() : "");
            dialeg.getChckLlegit().setSelected(Boolean.TRUE.equals(copy.getLlegit()));
            dialeg.getChckDesitjat().setSelected(copy.isDesitjat());
            dialeg.getTextISBN().setText("");
            dialeg.getTextISBN().requestFocusInWindow();
            dialeg.setLocationRelativeTo(state.vista);
            dialeg.setVisible(true);
            host.refreshAll();
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void prestarLlibre(long isbn) {
        String nom = JOptionPane.showInputDialog(state.vista,
            I18n.t("dlg_loan_msg"), I18n.t("dlg_loan_dialog_title"), JOptionPane.QUESTION_MESSAGE);
        if (nom == null || nom.isBlank()) return;
        try {
            state.cd.prestarLlibre(isbn, nom.trim());
            state.loanedISBNs = state.cd.getLoanedISBNs();
            host.tableCtrl().setLoanedISBNs(state.loanedISBNs);
            state.vista.getjTableBilio().repaint();
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_loan_done", nom.trim()),
                I18n.t("dlg_loan_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
    }
}
