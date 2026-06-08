package presentacio;

import domini.Llibre;
import domini.Llista;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import presentacio.detalles.control.DetallesLlibrePanelControl;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/** Book CRUD, details dialogs, stats, quick views, and undo. */
class BookActionsController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final FilterController filterCtrl;
    private final ShelfController shelfCtrl;

    BookActionsController(LibraryViewState state, LibraryScreenHost host,
                          FilterController filterCtrl, ShelfController shelfCtrl) {
        this.state = state;
        this.host = host;
        this.filterCtrl = filterCtrl;
        this.shelfCtrl = shelfCtrl;
    }

    void wireListeners() {
        state.vista.getBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
        state.vista.getBtnLlibreAleatori().addActionListener(e -> mostrarLlibreAleatori());
        state.vista.getBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
        state.vista.getBtnAfegitsRecentment().addActionListener(e -> mostrarAfegitsRecentment());
        state.vista.getBtnLlegitsRecentment().addActionListener(e -> mostrarLlegitsRecentment());
        state.vista.getBtnDesitjats().addActionListener(e -> mostrarDesitjats());
        state.vista.getBtnEnCurs().addActionListener(e -> mostrarEnCurs());
    }

    void abrirDetallesLlibres() { abrirDetalles(false); }

    void abrirDetallesEnEdicio() {
        if (state.vista.isGaleriaMode()) {
            List<Llibre> sel = state.vista.getGaleria().getSelectedLlibres();
            if (!sel.isEmpty()) abrirDetallesDeLlibre(sel.get(0), true);
        } else {
            abrirDetalles(true);
        }
    }

    private void abrirDetalles(boolean editMode) {
        try {
            javax.swing.JTable table = state.vista.getjTableBilio();
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = table.convertRowIndexToModel(viewRow);
            javax.swing.table.TableModel model = table.getModel();
            if (!(model instanceof BibliotecaTableModel bt)) return;
            Llibre l = bt.getBookAt(modelRow);
            if (l == null) return;
            abrirDetallesDeLlibre(l, editMode);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void abrirDetallesDeLlibre(Llibre l, boolean editMode) {
        if (l == null) return;
        try {
            DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, state.enActualizarBBDD, state.cd);
            detalles.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
            if (editMode) detalles.getDetallesLlibrePanel().getBtnEditar().doClick();
            detalles.getDetallesLlibrePanel().setVisible(true);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void refreshLlibre(Llibre l, boolean nuevo) {
        if (!nuevo) host.refreshRow(l);
        else {
            host.appendRow(l);
            host.updateTitleBar();
        }
    }

    void eliminarFilaSeleccionada() {
        JTable t = state.vista.getjTableBilio();
        int[] rows = t.getSelectedRows();
        if (rows.length == 0) return;
        String msg = rows.length == 1
            ? I18n.t("dlg_confirm_delete_one", t.getValueAt(rows[0], TableController.COL_NOM))
            : I18n.t("dlg_confirm_delete_n", rows.length);
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Long> isbns = new ArrayList<>();
        for (int row : rows) isbns.add(Long.parseLong((String) t.getValueAt(row, TableController.COL_ISBN)));
        for (long isbn : isbns) {
            try {
                Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
                if (l == null) continue;
                state.undoBuffer.push(l);
                if (state.undoBuffer.size() > LibraryViewState.UNDO_MAX) state.undoBuffer.removeLast();
                state.cd.deleteLlibre(l);
                eliminarFila(l);
            } catch (Exception e) {
                new DialogoError(e).showErrorMessage();
            }
        }
    }

    void eliminarFila(Llibre l) {
        host.removeRow(l);
        if (state.biblio != null) state.biblio.removeIf(b -> b.getISBN().equals(l.getISBN()));
        host.updateTitleBar();
    }

    void undoDelete() {
        if (state.undoBuffer.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_undo_empty"), I18n.t("dlg_undo_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre l = state.undoBuffer.pop();
        try {
            state.cd.addLlibre(l);
            refreshLlibre(l, true);
            JOptionPane.showMessageDialog(state.vista,
                I18n.t("dlg_undo_done", l.getNom()), I18n.t("dlg_undo_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void mostrarEstadistiques() {
        ArrayList<Llibre> global = new ArrayList<>(state.cd.getAllLlibres());
        if (global.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_empty_library"), I18n.t("dlg_stats_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        EstadistiquesHelper.BookStats globalStats = EstadistiquesHelper.computeStats(global);
        String summary = EstadistiquesHelper.buildStatsSummary(globalStats, I18n.t("lbl_all_library"));

        javax.swing.JTextArea txtSummary = new javax.swing.JTextArea(summary);
        txtSummary.setEditable(false);
        txtSummary.setFont(UITheme.fontBase());
        txtSummary.setBackground(UITheme.BG_PANEL);
        txtSummary.setForeground(UITheme.TEXT_DARK);
        txtSummary.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));

        javax.swing.table.DefaultTableModel shelfModel = new javax.swing.table.DefaultTableModel(
            new String[]{I18n.t("col_stats_llista"), I18n.t("col_stats_llibres"), I18n.t("col_stats_llegits"),
                I18n.t("col_stats_pct"), I18n.t("col_stats_val")}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        Map<Long, Llibre> byIsbn = new HashMap<>();
        for (Llibre l : state.cd.getAllLlibres()) byIsbn.put(l.getISBN(), l);
        Map<Integer, List<Llibre>> shelfBooks = new HashMap<>();
        if (state.cd instanceof domini.ControladorDomini dom) {
            for (persistencia.LlibreLlistaRow row : dom.getAllLlibreLlistaRows()) {
                Llibre l = byIsbn.get(row.isbn());
                if (l != null) shelfBooks.computeIfAbsent(row.llistaId(), k -> new ArrayList<>()).add(l);
            }
        }
        for (Llista ll : state.cd.getAllLlistes()) {
            List<Llibre> shelf = shelfBooks.getOrDefault(ll.getId(), List.of());
            if (shelf.isEmpty()) { shelfModel.addRow(new Object[]{ll.getNom(), 0, 0, "0.0%", "—"}); continue; }
            long llegits = shelf.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
            double avgVal = shelf.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
            shelfModel.addRow(new Object[]{
                ll.getNom(), shelf.size(), llegits,
                String.format("%.1f%%", 100.0 * llegits / shelf.size()),
                String.format("%.2f", avgVal)
            });
        }

        JTable shelfTable = new JTable(shelfModel);
        shelfTable.setFont(UITheme.fontBase());
        shelfTable.setBackground(UITheme.BG_PANEL);
        shelfTable.setForeground(UITheme.TEXT_DARK);
        shelfTable.setRowHeight(26);
        shelfTable.setEnabled(false);
        shelfTable.getTableHeader().setFont(UITheme.fontBold());
        javax.swing.JScrollPane shelfScroll = new javax.swing.JScrollPane(shelfTable);
        shelfScroll.setPreferredSize(new java.awt.Dimension(480, Math.min(200, shelfModel.getRowCount() * 27 + 30)));
        shelfScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("lbl_per_list")));

        int totalLlegits = (int) global.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
        int savedGoal = Config.getReadingGoal();
        javax.swing.JPanel goalPanel = new javax.swing.JPanel(new java.awt.BorderLayout(6, 4));
        goalPanel.setBackground(UITheme.BG_PANEL);
        goalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(UITheme.BORDER_CLR),
            I18n.t("lbl_reading_goal_section"), javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP, UITheme.fontBold(), UITheme.TEXT_MID));

        javax.swing.JProgressBar goalBar = new javax.swing.JProgressBar(0, Math.max(savedGoal, 1));
        goalBar.setValue(Math.min(totalLlegits, Math.max(savedGoal, 1)));
        goalBar.setStringPainted(true);
        goalBar.setFont(UITheme.fontBase());

        javax.swing.JSpinner goalSpinner = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(Math.max(savedGoal, 1), 1, 9999, 1));
        goalSpinner.setFont(UITheme.fontBase());
        goalSpinner.setPreferredSize(new java.awt.Dimension(70, 28));
        goalSpinner.addChangeListener(ev -> {
            int goal = (int) goalSpinner.getValue();
            Config.setReadingGoal(goal);
            goalBar.setMaximum(goal);
            goalBar.setValue(Math.min(totalLlegits, goal));
            goalBar.setString(totalLlegits + " / " + goal);
        });
        goalBar.setMaximum(Math.max(savedGoal, 1));
        goalBar.setString(totalLlegits + " / " + Math.max(savedGoal, 1));

        javax.swing.JLabel lblGoal = new javax.swing.JLabel(I18n.t("lbl_goal"));
        UITheme.styleLabel(lblGoal);
        javax.swing.JPanel goalControls = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        goalControls.setBackground(UITheme.BG_PANEL);
        goalControls.add(lblGoal);
        goalControls.add(goalSpinner);
        goalControls.add(new javax.swing.JLabel(I18n.t("lbl_read_count", totalLlegits)));
        goalPanel.add(goalControls, java.awt.BorderLayout.NORTH);
        goalPanel.add(goalBar, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel tab1 = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        tab1.setBackground(UITheme.BG_PANEL);
        tab1.add(goalPanel, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel statsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        statsPanel.setBackground(UITheme.BG_PANEL);
        statsPanel.add(txtSummary, java.awt.BorderLayout.NORTH);
        if (shelfModel.getRowCount() > 0) statsPanel.add(shelfScroll, java.awt.BorderLayout.CENTER);
        tab1.add(statsPanel, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel tab2 = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        tab2.setBackground(UITheme.BG_PANEL);
        tab2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tab2.add(EstadistiquesHelper.buildReadingChart(globalStats.booksByReadYear));
        tab2.add(EstadistiquesHelper.buildPublisherChart(global));

        javax.swing.JPanel tab3 = EstadistiquesHelper.buildTagCloud(global, state.cd);
        javax.swing.JPanel tab4 = EstadistiquesHelper.buildReadingPacePanel(global, globalStats.booksByReadYear);

        EstadistiquesHelper.showDialog(SwingUtilities.getWindowAncestor(state.vista),
            new javax.swing.JComponent[] { tab1, tab2, tab3, tab4 },
            new String[] { I18n.t("stats_tab_general"), I18n.t("stats_tab_charts"),
                I18n.t("stats_tab_tags"), I18n.t("stats_tab_pace") });
    }

    void mostrarLlibreAleatori() {
        if (state.biblio == null || state.biblio.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_books_view"), I18n.t("dlg_aleatori_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Llibre> noLlegits = state.biblio.stream()
            .filter(l -> !Boolean.TRUE.equals(l.getLlegit()))
            .collect(Collectors.toList());
        if (noLlegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_all_read"), I18n.t("dlg_aleatori_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre aleatori = noLlegits.get(new Random().nextInt(noLlegits.size()));
        JTable t = state.vista.getjTableBilio();
        for (int row = 0; row < t.getRowCount(); row++) {
            if (String.valueOf(aleatori.getISBN()).equals(t.getValueAt(row, TableController.COL_ISBN))) {
                t.setRowSelectionInterval(row, row);
                t.scrollRectToVisible(t.getCellRect(row, 0, true));
                break;
            }
        }
        DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(aleatori, state.enActualizarBBDD, state.cd);
        detalles.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
        detalles.getDetallesLlibrePanel().setVisible(true);
    }

    private void mostrarAfegitsRecentment() {
        ArrayList<Llibre> recents = new ArrayList<>(state.cd.getRecentlyAdded());
        if (recents.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_books_recent"),
                I18n.t("dlg_recently_added_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = recents;
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarLlegitsRecentment() {
        ArrayList<Llibre> llegits = new ArrayList<>(
            state.cd.getAllLlibres().stream()
                .filter(l -> Boolean.TRUE.equals(l.getLlegit()))
                .collect(Collectors.toList()));
        if (llegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_read"), I18n.t("dlg_read_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = llegits;
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarDesitjats() {
        state.biblio = new ArrayList<>(
            state.cd.getAllLlibres().stream()
                .filter(l -> Boolean.TRUE.equals(l.getDesitjat()))
                .collect(Collectors.toList()));
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarEnCurs() {
        state.biblio = new ArrayList<>(
            state.cd.getAllLlibres().stream()
                .filter(l -> l.getPaginesLlegides() > 0 && !Boolean.TRUE.equals(l.getLlegit()))
                .collect(Collectors.toList()));
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    void obrirConfiguracio() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        new ConfiguracioDialog(
            w instanceof JFrame ? (JFrame) w : null,
            new ConfiguracioDialogListener() {
                @Override public void onThemeChange() { state.vista.applyTheme(); }
                @Override public void onRefreshData() {
                    state.biblio = new ArrayList<>(state.cd.getAllLlibres());
                    host.pageCtrl().setUseDBPagination(state.cd.isLargeLibrary());
                    state.currentLlistaId = null;
                    filterCtrl.quitarFiltros();
                    shelfCtrl.refreshComboLlistes();
                }
            },
            state.cd
        ).setVisible(true);
    }
}
