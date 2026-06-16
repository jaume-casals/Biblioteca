package presentacio.detalles.control;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import domini.Llibre;
import domini.Llista;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.SwingUtils;
import interficie.ShelfWriter;
import presentacio.detalles.vista.LlistesDelLlibreDialog;
import presentacio.detalles.vista.LlistesDelLlibreTableModel;

public class LlistesDelLlibreControl {

    private final LlistesDelLlibreDialog vista;
    private final Llibre llibre;
    private final ShelfWriter cd;
    private ArrayList<Llista> llistesCache = new ArrayList<>();
    private ArrayList<Llista> allLlistesCache = new ArrayList<>();
    private java.util.Set<Integer> memberIds = new java.util.HashSet<>();

    /**
     * Single stateless shelf-check renderer reused across {@link #reload()}
     * calls (per the tot.txt LOW finding). The renderer captures
     * {@code memberIds} at construction time — but memberIds is a
     * mutable Set field on the enclosing class, so all instances share
     * the same backing set (we re-assign the field, not the Set itself,
     * on every reload to keep the captured reference valid).
     */
    private final javax.swing.ListCellRenderer<java.lang.Object> SHELF_RENDERER =
        new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Llista ll) {
                    setText((memberIds.contains(ll.getId()) ? "☑ " : "☐ ") + ll.getNom());
                }
                return this;
            }
        };

    public LlistesDelLlibreControl(LlistesDelLlibreDialog vista, Llibre llibre, ShelfWriter cd) {
        this.vista = vista;
        this.llibre = llibre;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        wireListeners();
        reload();
    }

    private void wireListeners() {
        vista.getBtnAfegir().addActionListener(e -> onAfegir());
        vista.getBtnTreure().addActionListener(e -> onTreure());
        vista.getBtnGuardar().addActionListener(e -> onGuardar());
        vista.getShelfCheckList().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int idx = vista.getShelfCheckList().locationToIndex(e.getPoint());
                if (idx < 0) return;
                Llista l = vista.getShelfCheckList().getModel().getElementAt(idx);
                toggleShelfMembership(l);
            }
        });
    }

    private void toggleShelfMembership(Llista llista) {
        boolean member = llistesCache.stream().anyMatch(l -> l.getId() == llista.getId());
        try {
            if (member) cd.removeLlibreFromLlista(llibre.getISBN(), llista.getId());
            else {
                double val = 0.0;
                try { val = Double.parseDouble(vista.getTxtVal().getText().trim()); } catch (NumberFormatException ignored) {}
                cd.addLlibreToLlista(llibre.getISBN(), llista.getId(), val, vista.getChkLlegit().isSelected());
            }
            reload();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onAfegir() {
        if (vista.getComboAdd().getItemCount() == 0) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_no_llistes_msg"),
                I18n.t("dlg_no_llistes_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llista llista = (Llista) vista.getComboAdd().getSelectedItem();
        double val = 0.0;
        try { val = Double.parseDouble(vista.getTxtVal().getText().trim()); } catch (NumberFormatException ignored) {}
        boolean llegit = vista.getChkLlegit().isSelected();
        try {
            cd.addLlibreToLlista(llibre.getISBN(), llista.getId(), val, llegit);
            reload();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onTreure() {
        int row = vista.getTable().getSelectedRow();
        if (row < 0) return;
        Llista target = vista.getTableModel().getLlistaAt(row);
        if (target == null) return;
        try {
            cd.removeLlibreFromLlista(llibre.getISBN(), target.getId());
            reload();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onGuardar() {
        var table = vista.getTable();
        LlistesDelLlibreTableModel model = vista.getTableModel();
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        for (int r = 0; r < model.getRowCount(); r++) {
            Llista target = model.getLlistaAt(r);
            if (target == null) continue;
            try {
                cd.updateLlibreInLlista(llibre.getISBN(), target.getId(),
                    target.getValoracioLlibre(), target.getLlegitLlibre());
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
                return;
            }
        }
        JOptionPane.showMessageDialog(vista, I18n.t("dlg_canvis_guardats"),
            I18n.t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void reload() {
        llistesCache = new ArrayList<>(cd.getLlistesForLlibre(llibre.getISBN()));
        allLlistesCache = new ArrayList<>(cd.getAllLlistes());
        SwingUtils.reloadComboPreserveSelection(vista.getComboAdd(), allLlistesCache, Llista::getId);
        vista.getTableModel().setRows(llistesCache);
        if (vista.getTableModel().getRowCount() > 0) {
            vista.getTable().setRowSelectionInterval(0, vista.getTableModel().getRowCount() - 1);
        }
        // Reuse the same HashSet across reloads (clear+addAll) so the
        // captured SHELF_RENDERER still points to a valid set.
        memberIds.clear();
        for (Llista l : llistesCache) memberIds.add(l.getId());
        javax.swing.DefaultListModel<Llista> model = new javax.swing.DefaultListModel<>();
        for (Llista l : allLlistesCache) model.addElement(l);
        vista.getShelfCheckList().setModel(model);
        java.util.List<Integer> memberIdx = new java.util.ArrayList<>();
        for (int i = 0; i < allLlistesCache.size(); i++) {
            if (memberIds.contains(allLlistesCache.get(i).getId())) memberIdx.add(i);
        }
        int[] memberIdxArr = new int[memberIdx.size()];
        for (int i = 0; i < memberIdx.size(); i++) memberIdxArr[i] = memberIdx.get(i);
        vista.getShelfCheckList().setSelectedIndices(memberIdxArr);
        vista.getShelfCheckList().setCellRenderer(SHELF_RENDERER);
    }
}
