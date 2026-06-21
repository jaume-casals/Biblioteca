package presentacio.detalles.control;

import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import domini.Llibre;
import domini.Llista;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.UtilitatsSwing;
import interficie.EscritorPrestatgeria;
import presentacio.detalles.vista.DialegLlistesLlibre;
import presentacio.detalles.vista.ModelTaulaLlistesLlibre;

public class ControladorLlistesLlibre {

    private final DialegLlistesLlibre vista;
    private final Llibre llibre;
    private final EscritorPrestatgeria cd;
    private ArrayList<Llista> llistesCache = new ArrayList<>();
    private ArrayList<Llista> allLlistesCache = new ArrayList<>();
    private java.util.Set<Integer> memberIds = new java.util.HashSet<>();

    /**
     * Instància única de renderer de checks de prestatgeria sense estat
     * reutilitzada entre les crides a {@link #reload()} (segons el
     * finding LOW de tot.txt). El renderer captura {@code memberIds}
     * en temps de construcció — però memberIds és un camp Set mutable
     * a la classe contenidora, de manera que totes les instàncies
     * comparteixen el mateix set de suport (reassignem el camp, no
     * el Set en si, a cada recàrrega per mantenir vàlida la referència
     * capturada).
     */
    private final javax.swing.ListCellRenderer<java.lang.Object> SHELF_RENDERER =
        new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Llista ll) {
                    setText((memberIds.contains(ll.obtenirId()) ? "☑ " : "☐ ") + ll.obtenirNom());
                }
                return this;
            }
        };

    public ControladorLlistesLlibre(DialegLlistesLlibre vista, Llibre llibre, EscritorPrestatgeria cd) {
        this.vista = vista;
        this.llibre = llibre;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        wireListeners();
        reload();
    }

    private void wireListeners() {
        vista.obtenirBtnAfegir().addActionListener(e -> onAfegir());
        vista.obtenirBtnTreure().addActionListener(e -> onTreure());
        vista.obtenirBtnGuardar().addActionListener(e -> onGuardar());
        vista.obtenirShelfCheckList().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int idx = vista.obtenirShelfCheckList().locationToIndex(e.getPoint());
                if (idx < 0) return;
                Llista l = vista.obtenirShelfCheckList().getModel().getElementAt(idx);
                toggleShelfMembership(l);
            }
        });
    }

    private void toggleShelfMembership(Llista llista) {
        boolean member = llistesCache.stream().anyMatch(l -> l.obtenirId() == llista.obtenirId());
        try {
            if (member) cd.eliminarLlibreFromLlista(llibre.obtenirISBN(), llista.obtenirId());
            else {
                double val = 0.0;
                try { val = Double.parseDouble(vista.obtenirTxtVal().getText().trim()); } catch (NumberFormatException ignored) {}
                cd.afegirLlibreToLlista(llibre.obtenirISBN(), llista.obtenirId(), val, vista.obtenirChkLlegit().isSelected());
            }
            reload();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onAfegir() {
        if (vista.obtenirComboAdd().getItemCount() == 0) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_no_llistes_msg"),
                I18n.t("dlg_no_llistes_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llista llista = (Llista) vista.obtenirComboAdd().getSelectedItem();
        double val = 0.0;
        try { val = Double.parseDouble(vista.obtenirTxtVal().getText().trim()); } catch (NumberFormatException ignored) {}
        boolean llegit = vista.obtenirChkLlegit().isSelected();
        try {
            cd.afegirLlibreToLlista(llibre.obtenirISBN(), llista.obtenirId(), val, llegit);
            reload();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onTreure() {
        int row = vista.obtenirTable().getSelectedRow();
        if (row < 0) return;
        Llista target = vista.obtenirTableModel().obtenirLlistaAt(row);
        if (target == null) return;
        try {
            cd.eliminarLlibreFromLlista(llibre.obtenirISBN(), target.obtenirId());
            reload();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onGuardar() {
        var table = vista.obtenirTable();
        ModelTaulaLlistesLlibre model = vista.obtenirTableModel();
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        for (int r = 0; r < model.getRowCount(); r++) {
            Llista target = model.obtenirLlistaAt(r);
            if (target == null) continue;
            try {
                cd.actualitzarLlibreInLlista(llibre.obtenirISBN(), target.obtenirId(),
                    target.obtenirValoracioLlibre(), target.obtenirLlegitLlibre());
            } catch (Exception ex) {
                new DialegError(ex).mostrarErrorMessage();
                return;
            }
        }
        JOptionPane.showMessageDialog(vista, I18n.t("dlg_canvis_guardats"),
            I18n.t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void reload() {
        new SwingWorker<ReloadData, Void>() {
            @Override protected ReloadData doInBackground() {
                ArrayList<Llista> perLlibre = new ArrayList<>(cd.obtenirLlistesForLlibre(llibre.obtenirISBN()));
                ArrayList<Llista> all = new ArrayList<>(cd.obtenirAllLlistes());
                return new ReloadData(perLlibre, all);
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    ReloadData d = get();
                    llistesCache = d.perLlibre();
                    allLlistesCache = d.all();
                    UtilitatsSwing.reloadComboPreserveSelection(vista.obtenirComboAdd(), allLlistesCache, Llista::obtenirId);
                    vista.obtenirTableModel().setRows(llistesCache);
                    if (vista.obtenirTableModel().getRowCount() > 0) {
                        vista.obtenirTable().setRowSelectionInterval(0, vista.obtenirTableModel().getRowCount() - 1);
                    }
                    // Reutilitza el mateix HashSet entre recàrregues (clear+addAll)
                    // perquè el SHELF_RENDERER capturat continuï apuntant a un set vàlid.
                    memberIds.clear();
                    for (Llista l : llistesCache) memberIds.add(l.obtenirId());
                    javax.swing.DefaultListModel<Llista> model = new javax.swing.DefaultListModel<>();
                    for (Llista l : allLlistesCache) model.addElement(l);
                    vista.obtenirShelfCheckList().setModel(model);
                    java.util.List<Integer> memberIdx = new java.util.ArrayList<>();
                    for (int i = 0; i < allLlistesCache.size(); i++) {
                        if (memberIds.contains(allLlistesCache.get(i).obtenirId())) memberIdx.add(i);
                    }
                    int[] memberIdxArr = new int[memberIdx.size()];
                    for (int i = 0; i < memberIdx.size(); i++) memberIdxArr[i] = memberIdx.get(i);
                    vista.obtenirShelfCheckList().setSelectedIndices(memberIdxArr);
                    vista.obtenirShelfCheckList().setCellRenderer(SHELF_RENDERER);
                } catch (Exception ex) {
                    new DialegError(ex).mostrarErrorMessage();
                }
            }
        }.execute();
    }

    private record ReloadData(ArrayList<Llista> perLlibre, ArrayList<Llista> all) {}
}
