package presentacio;

import domini.Llibre;
import domini.LlibreFilter;
import domini.Tag;
import herramienta.Config;
import herramienta.FiltreUtils;
import herramienta.I18n;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Filter drawer, search bar, and preset management. */
class FilterController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;

    FilterController(LibraryViewState state, LibraryScreenHost host) {
        this.state = state;
        this.host = host;
    }

    void wireListeners() {
        var vista = state.vista;
        vista.getchckbxLlegit().addItemListener(this::enLlegitSeleccionado);
        vista.getchckbxNoLlegit().addItemListener(this::enNoLlegitSeleccionado);

        java.awt.event.ActionListener enterFiltrar = e -> filtrar();
        vista.getTextNom().addActionListener(enterFiltrar);
        vista.getTextAutor().addActionListener(enterFiltrar);
        vista.getTextISBN().addActionListener(enterFiltrar);
        vista.getAnyMin().addActionListener(enterFiltrar);
        vista.getAnyMax().addActionListener(enterFiltrar);
        vista.getValoracioMin().addActionListener(enterFiltrar);
        vista.getValoracioMax().addActionListener(enterFiltrar);
        vista.getPreuMin().addActionListener(enterFiltrar);
        vista.getPreuMax().addActionListener(enterFiltrar);
        vista.getFilterEditorial().addActionListener(enterFiltrar);
        vista.getFilterSerie().addActionListener(enterFiltrar);
        vista.getFilterIdioma().addActionListener(enterFiltrar);

        vista.getSearchBar().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private javax.swing.Timer debounce;
            private final int DEBOUNCE_MS = 250;
            private void scheduleSearch() {
                if (debounce != null && debounce.isRunning()) debounce.stop();
                debounce = new javax.swing.Timer(DEBOUNCE_MS, e -> {
                    aplicarSearchBar();
                    vista.getjTableBilio().repaint();
                });
                debounce.setRepeats(false);
                debounce.start();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
        });

        vista.getBtnCarregaPreset().addActionListener(e -> carregarPreset());
        vista.getBtnDesaPreset().addActionListener(e -> desarPreset());
        vista.getBtnEsborraPreset().addActionListener(e -> esborrarPreset());
        refreshComboPresets();

        vista.getBtnToggleFiltres().addActionListener(e -> {
            boolean show = !vista.isFilterDrawerVisible();
            vista.setFilterDrawerVisible(show);
            vista.getBtnToggleFiltres().setText(show ? I18n.t("btn_filters_label_open") : I18n.t("btn_filters_label"));
        });

        vista.getbtnFiltrar().addActionListener(e -> filtrar());
        vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
    }

    void enNoLlegitSeleccionado(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && state.vista.getchckbxLlegit().isSelected())
            state.vista.getchckbxLlegit().setSelected(false);
    }

    void enLlegitSeleccionado(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && state.vista.getchckbxNoLlegit().isSelected())
            state.vista.getchckbxNoLlegit().setSelected(false);
    }

    void filtrar() {
        domini.LlibreFilterBuilder b = domini.LlibreFilterBuilder.of();

        String autorTyped = state.vista.getTextAutor().getText().trim();
        if (!autorTyped.isEmpty()) b.autor(autorTyped);

        String nomTyped = state.vista.getTextNom().getText().trim();
        if (!nomTyped.isEmpty()) b.nom(nomTyped);

        String isbnText = state.vista.getTextISBN().getText().trim();
        if (!isbnText.isEmpty()) {
            b.isbn(parseLongField(state.vista.getTextISBN(), isbnText));
        }

        b.anyMin(parseIntField(state.vista.getAnyMin()));
        b.anyMax(parseIntField(state.vista.getAnyMax()));
        b.valoracioMin(parseDoubleField(state.vista.getValoracioMin()));
        b.valoracioMax(parseDoubleField(state.vista.getValoracioMax()));
        b.preuMin(parseDoubleField(state.vista.getPreuMin()));
        b.preuMax(parseDoubleField(state.vista.getPreuMax()));

        if (state.vista.getchckbxLlegit().isSelected())  b.llegit(Boolean.TRUE);
        if (state.vista.getchckbxNoLlegit().isSelected()) b.llegit(Boolean.FALSE);

        Object selTag = state.vista.getComboTagFilter().getSelectedItem();
        if (selTag instanceof Tag) b.tagId(((Tag) selTag).getId());

        String editorial = state.vista.getFilterEditorial().getText().trim();
        if (!editorial.isEmpty()) b.editorial(editorial);
        String serie = state.vista.getFilterSerie().getText().trim();
        if (!serie.isEmpty()) b.serie(serie);
        String idioma = state.vista.getFilterIdioma().getText().trim();
        if (!idioma.isEmpty()) b.idioma(idioma);
        String format = (String) state.vista.getFilterFormat().getSelectedItem();
        if (format != null && !format.isEmpty()) b.format(format);

        LlibreFilter f = b.build();

        boolean dbPath = state.currentLlistaId == null && state.cd.isLargeLibrary();
        if (dbPath) {
            final LlibreFilter filter = f;
            state.vista.getbtnFiltrar().setEnabled(false);
            new SwingWorker<ArrayList<Llibre>, Void>() {
                @Override protected ArrayList<Llibre> doInBackground() {
                    return new ArrayList<>(MainFrameControl.getInstance().aplicarFiltres(filter));
                }
                @Override protected void done() {
                    state.vista.getbtnFiltrar().setEnabled(true);
                    try {
                        host.setTable(get());
                    } catch (Exception ex) {
                        new herramienta.DialogoError(ex).showErrorMessage();
                    }
                }
            }.execute();
            return;
        }

        javax.swing.RowSorter<?> rs = state.vista.getjTableBilio().getRowSorter();
        if (!(rs instanceof javax.swing.DefaultRowSorter)) {
            host.setTable(new ArrayList<>(state.cd.aplicarFiltres(state.biblio, f)));
            return;
        }
        @SuppressWarnings("unchecked")
        javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer> drs =
            (javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer>) rs;

        ArrayList<Llibre> base = state.biblio instanceof ArrayList<Llibre> a ? a : new ArrayList<>(state.biblio);
        if (state.modelLibres != base) host.setTable(base);

        if (!f.hasAnyFilter()) {
            drs.setRowFilter(null);
            host.updateTitleBar();
            return;
        }

        Map<Long, Llibre> isbnMap = new HashMap<>();
        if (base != null) for (Llibre l : base) isbnMap.put(l.getISBN(), l);
        java.util.Set<Long> tagISBNs = f.getTagId() != null ? state.cd.getLlibresWithTag(f.getTagId()) : null;
        java.util.Set<Long> llistaISBNs = f.getLlistaId() != null
            ? state.cd.getLlibresInLlista(f.getLlistaId()).stream().map(Llibre::getISBN).collect(Collectors.toSet())
            : null;

        drs.setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel, Integer>() {
            @Override
            public boolean include(javax.swing.RowFilter.Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                try {
                    long isbn = Long.parseLong(entry.getStringValue(TableController.COL_ISBN));
                    Llibre l = isbnMap.get(isbn);
                    if (l == null) return false;
                    return FiltreUtils.matches(l, f, tagISBNs, llistaISBNs);
                } catch (Exception ignored) { return false; }
            }
        });
        host.updateTitleBar();
    }

    void quitarFiltros() {
        state.vista.getchckbxLlegit().setSelected(false);
        state.vista.getchckbxNoLlegit().setSelected(false);
        state.vista.getAnyMin().setText("");
        state.vista.getAnyMax().setText("");
        state.vista.getValoracioMin().setText("");
        state.vista.getValoracioMax().setText("");
        state.vista.getPreuMin().setText("");
        state.vista.getPreuMax().setText("");
        if (state.vista.getComboTagFilter().getItemCount() > 0)
            state.vista.getComboTagFilter().setSelectedIndex(0);
        state.vista.getFilterEditorial().setText("");
        state.vista.getFilterSerie().setText("");
        state.vista.getFilterIdioma().setText("");
        state.vista.getFilterFormat().setSelectedIndex(0);
        removeAlldataFiltros();
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    void aplicarSearchBar() {
        String query = state.vista.getSearchBar().getText().trim();
        if (host.tableCtrl().highlightRenderer() != null)
            host.tableCtrl().highlightRenderer().setSearchText(query);
        javax.swing.RowSorter<?> sorter = state.vista.getjTableBilio().getRowSorter();
        if (!(sorter instanceof javax.swing.DefaultRowSorter)) { host.updateTitleBar(); return; }
        @SuppressWarnings("unchecked")
        javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer> drs =
            (javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer>) sorter;
        if (query.isEmpty()) {
            drs.setRowFilter(null);
        } else {
            String q = query.toLowerCase(java.util.Locale.ROOT);
            Map<Long, Llibre> isbnMap = new HashMap<>();
            if (state.biblio != null) for (Llibre l : state.biblio) isbnMap.put(l.getISBN(), l);
            drs.setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel, Integer>() {
                @Override
                public boolean include(javax.swing.RowFilter.Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        if (entry.getStringValue(i).toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
                    }
                    try {
                        long isbn = Long.parseLong(entry.getStringValue(TableController.COL_ISBN));
                        Llibre l = isbnMap.get(isbn);
                        if (l != null) {
                            String desc  = l.getDescripcio();
                            String notes = l.getNotes();
                            if (desc  != null && desc.toLowerCase(java.util.Locale.ROOT).contains(q))  return true;
                            if (notes != null && notes.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
                        }
                    } catch (Exception ignored) {}
                    return false;
                }
            });
        }
        host.updateTitleBar();
    }

    void removeAlldataFiltros() {
        state.vista.getTextISBN().setText("");
        state.vista.getTextNom().setText("");
        state.vista.getTextAutor().setText("");
    }

    Map<String, String> collectFilterState() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("nom",          state.vista.getTextNom().getText());
        m.put("autor",        state.vista.getTextAutor().getText());
        m.put("isbn",         state.vista.getTextISBN().getText());
        m.put("anyMin",       state.vista.getAnyMin().getText());
        m.put("anyMax",       state.vista.getAnyMax().getText());
        m.put("valoracioMin", state.vista.getValoracioMin().getText());
        m.put("valoracioMax", state.vista.getValoracioMax().getText());
        m.put("preuMin",      state.vista.getPreuMin().getText());
        m.put("preuMax",      state.vista.getPreuMax().getText());
        m.put("llegit",       state.vista.getchckbxLlegit().isSelected() ? "true"
                            : state.vista.getchckbxNoLlegit().isSelected() ? "false" : "");
        m.put("editorial",    state.vista.getFilterEditorial().getText());
        m.put("serie",        state.vista.getFilterSerie().getText());
        m.put("idioma",       state.vista.getFilterIdioma().getText());
        String fmt = (String) state.vista.getFilterFormat().getSelectedItem();
        m.put("format",       fmt != null ? fmt : "");
        return m;
    }

    void applyFilterState(Map<String, String> s) {
        state.vista.getTextNom().setText(s.getOrDefault("nom", ""));
        state.vista.getTextAutor().setText(s.getOrDefault("autor", ""));
        state.vista.getTextISBN().setText(s.getOrDefault("isbn", ""));
        state.vista.getAnyMin().setText(s.getOrDefault("anyMin", ""));
        state.vista.getAnyMax().setText(s.getOrDefault("anyMax", ""));
        state.vista.getValoracioMin().setText(s.getOrDefault("valoracioMin", ""));
        state.vista.getValoracioMax().setText(s.getOrDefault("valoracioMax", ""));
        state.vista.getPreuMin().setText(s.getOrDefault("preuMin", ""));
        state.vista.getPreuMax().setText(s.getOrDefault("preuMax", ""));
        String llegit = s.getOrDefault("llegit", "");
        state.vista.getchckbxLlegit().setSelected("true".equals(llegit));
        state.vista.getchckbxNoLlegit().setSelected("false".equals(llegit));
        state.vista.getFilterEditorial().setText(s.getOrDefault("editorial", ""));
        state.vista.getFilterSerie().setText(s.getOrDefault("serie", ""));
        state.vista.getFilterIdioma().setText(s.getOrDefault("idioma", ""));
        state.vista.getFilterFormat().setSelectedItem(s.getOrDefault("format", ""));
    }

    private void carregarPreset() {
        int idx = state.vista.getComboPresets().getSelectedIndex();
        if (idx < 0 || Config.getPresetCount() == 0) return;
        applyFilterState(Config.loadPreset(idx));
        filtrar();
    }

    private void desarPreset() {
        String name = JOptionPane.showInputDialog(state.vista, I18n.t("dlg_filter_name_prompt"),
            I18n.t("dlg_save_filter_title"), JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) return;
        Config.savePreset(name.trim(), collectFilterState());
        refreshComboPresets();
        state.vista.getComboPresets().setSelectedIndex(Config.getPresetCount() - 1);
    }

    private void esborrarPreset() {
        int idx = state.vista.getComboPresets().getSelectedIndex();
        if (idx < 0 || Config.getPresetCount() == 0) return;
        String name = Config.getPresetName(idx);
        if (JOptionPane.showConfirmDialog(state.vista, I18n.t("dlg_delete_preset", name),
                I18n.t("dlg_delete_preset_title"), JOptionPane.YES_OPTION) != JOptionPane.YES_OPTION) return;
        Config.deletePreset(idx);
        refreshComboPresets();
    }

    private void refreshComboPresets() {
        javax.swing.JComboBox<String> combo = state.vista.getComboPresets();
        combo.removeAllItems();
        int n = Config.getPresetCount();
        if (n == 0) {
            combo.addItem(I18n.t("lbl_no_presets"));
            state.vista.getBtnCarregaPreset().setEnabled(false);
            state.vista.getBtnEsborraPreset().setEnabled(false);
        } else {
            for (int i = 0; i < n; i++) combo.addItem(Config.getPresetName(i));
            state.vista.getBtnCarregaPreset().setEnabled(true);
            state.vista.getBtnEsborraPreset().setEnabled(true);
        }
    }

    private Integer parseIntField(JTextField field) {
        String raw = field.getText().trim();
        if (raw.isEmpty()) {
            clearInvalidField(field);
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            clearInvalidField(field);
            return value;
        } catch (NumberFormatException e) {
            markInvalidField(field);
            return null;
        }
    }

    private Long parseLongField(JTextField field, String raw) {
        if (raw.isEmpty()) {
            clearInvalidField(field);
            return null;
        }
        try {
            long value = Long.parseLong(raw);
            clearInvalidField(field);
            return value;
        } catch (NumberFormatException e) {
            markInvalidField(field);
            return null;
        }
    }

    private Double parseDoubleField(JTextField field) {
        String raw = field.getText().trim();
        if (raw.isEmpty()) {
            clearInvalidField(field);
            return null;
        }
        try {
            double value = Double.parseDouble(raw);
            clearInvalidField(field);
            return value;
        } catch (NumberFormatException e) {
            markInvalidField(field);
            return null;
        }
    }

    private void markInvalidField(JTextField field) {
        field.putClientProperty("JComponent.outline", "error");
        field.setToolTipText(I18n.t("validation_invalid_number"));
    }

    private void clearInvalidField(JTextField field) {
        field.putClientProperty("JComponent.outline", null);
        field.setToolTipText(null);
    }
}
