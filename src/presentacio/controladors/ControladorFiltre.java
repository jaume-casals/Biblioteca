package presentacio.controladors;

import domini.Llibre;
import domini.LlibreFilter;
import domini.Tag;
import herramienta.config.Configuracio;
import herramienta.i18n.I18n;
import herramienta.text.FiltreUtils;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import presentacio.formularis.RegistreCampsFormulari;
import presentacio.models.ModelTaulaBiblioteca;
import presentacio.util.AmfitrioPantallaBiblioteca;
import presentacio.util.EstatVistaBiblioteca;


/** Calaix de filtre, barra de cerca i gestió de presets. */
public class ControladorFiltre {

    private static final int DEBOUNCE_MS = 250;

    private final EstatVistaBiblioteca state;
    private final AmfitrioPantallaBiblioteca host;

    /** Índex ISBN→Llibre en caché; es reconstrueix només quan canvia
     *  la referència de {@code state.biblio}. Evita reindexar 5000 llibres
     *  a cada pulsació de tecla de la barra de cerca. */
    private java.util.List<Llibre> cachedIsbnMapBiblioRef;
    private Map<Long, Llibre> cachedIsbnMap;

    ControladorFiltre(EstatVistaBiblioteca state, AmfitrioPantallaBiblioteca host) {
        this.state = state;
        this.host = host;
    }

    private Map<Long, Llibre> obtenirIsbnMap() {
        if (cachedIsbnMap == null || cachedIsbnMapBiblioRef != state.biblio) {
            cachedIsbnMapBiblioRef = state.biblio;
            cachedIsbnMap = new HashMap<>();
            if (state.biblio != null) for (Llibre l : state.biblio) cachedIsbnMap.put(l.obtenirISBN(), l);
        }
        return cachedIsbnMap;
    }

    void wireListeners() {
        var vista = state.vista;
        vista.obtenirCasellaLlegit().addItemListener(this::enLlegitSeleccionado);
        vista.obtenirCasellaNoLlegit().addItemListener(this::enNoLlegitSeleccionado);

        java.awt.event.ActionListener enterFiltrar = e -> filtrar();
        vista.obtenirFilterRegistry().specsOfKind(presentacio.formularis.RegistreCampsFormulari.Tipus.TEXT).stream()
            .map(s -> vista.obtenirFilterRegistry().textField(s.key()))
            .forEach(tf -> tf.addActionListener(enterFiltrar));

        vista.obtenirSearchBar().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private javax.swing.Timer debounce;
            private void scheduleSearch() {
                if (debounce != null) debounce.stop();
                debounce = new javax.swing.Timer(DEBOUNCE_MS, e -> {
                    aplicarSearchBar();
                    vista.obtenirTaulaLlibres().repaint();
                });
                debounce.setRepeats(false);
                debounce.start();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
        });

        vista.obtenirBtnCarregaPreset().addActionListener(e -> carregarPreset());
        vista.obtenirBtnDesaPreset().addActionListener(e -> desarPreset());
        vista.obtenirBtnEsborraPreset().addActionListener(e -> esborrarPreset());
        refrescarComboPresets();

        vista.obtenirBtnToggleFiltres().addActionListener(e -> {
            boolean show = !vista.esFilterDrawerVisible();
            vista.posarFilterDrawerVisible(show);
            vista.obtenirBtnToggleFiltres().setText(show ? I18n.t("btn_filters_label_open") : I18n.t("btn_filters_label"));
        });

        vista.obtenirBtnFiltrar().addActionListener(e -> filtrar());
        vista.obtenirBtnQuitarFiltros().addActionListener(e -> quitarFiltros());
    }

    /** Manté el worker de filtre pel camí de BBDD en vol perquè un segon
     *  clic pugui cancel·lar el primer en lloc d'apilar dues consultes
     *  en segon pla els resultats de les quals competirien a l'EDT.
     *  El {@code done()} del worker comprova {@code isCancelled()} abans
     *  de tocar la taula. */
    private SwingWorker<ArrayList<Llibre>, Void> pendingFilterWorker;

    void enNoLlegitSeleccionado(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && state.vista.obtenirCasellaLlegit().isSelected())
            state.vista.obtenirCasellaLlegit().setSelected(false);
    }

    void enLlegitSeleccionado(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && state.vista.obtenirCasellaNoLlegit().isSelected())
            state.vista.obtenirCasellaNoLlegit().setSelected(false);
    }

    void filtrar() {
        domini.LlibreFilter f = domini.LlibreFilter.empty();

        String autorTyped = state.vista.obtenirTextAutor().getText().trim();
        if (!autorTyped.isEmpty()) f.withAutor(autorTyped);

        String nomTyped = state.vista.obtenirTextNom().getText().trim();
        if (!nomTyped.isEmpty()) f.withNom(nomTyped);

        String isbnText = state.vista.obtenirTextISBN().getText().trim();
        if (!isbnText.isEmpty()) {
            f.withIsbn(analitzarLongField(state.vista.obtenirTextISBN(), isbnText));
        }

        f.withAnyMin(analitzarIntField(state.vista.obtenirAnyMin()));
        f.withAnyMax(analitzarIntField(state.vista.obtenirAnyMax()));
        f.withValoracioMin(analitzarDoubleField(state.vista.obtenirValoracioMin()));
        f.withValoracioMax(analitzarDoubleField(state.vista.obtenirValoracioMax()));
        f.withPreuMin(analitzarDoubleField(state.vista.obtenirPreuMin()));
        f.withPreuMax(analitzarDoubleField(state.vista.obtenirPreuMax()));

        if (state.vista.obtenirCasellaLlegit().isSelected())  f.withLlegit(Boolean.TRUE);
        if (state.vista.obtenirCasellaNoLlegit().isSelected()) f.withLlegit(Boolean.FALSE);

        Object selTag = state.vista.obtenirComboTagFilter().getSelectedItem();
        if (selTag instanceof Tag) f.withTagId(((Tag) selTag).obtenirId());

        String editorial = state.vista.obtenirFilterEditorial().getText().trim();
        if (!editorial.isEmpty()) f.withEditorial(editorial);
        String serie = state.vista.obtenirFilterSerie().getText().trim();
        if (!serie.isEmpty()) f.withSerie(serie);
        String idioma = state.vista.obtenirFilterIdioma().getText().trim();
        if (!idioma.isEmpty()) f.withIdioma(idioma);
        String format = (String) state.vista.obtenirFilterFormat().getSelectedItem();
        if (format != null && !format.isEmpty()) f.withFormat(format);

        boolean dbPath = state.currentLlistaId == null && state.cd.esLargeLibrary();
        if (dbPath) {
            final LlibreFilter filter = f;
            if (pendingFilterWorker != null && !pendingFilterWorker.isDone()) {
                pendingFilterWorker.cancel(true);
            }
            state.vista.obtenirBtnFiltrar().setEnabled(false);
            pendingFilterWorker = new SwingWorker<ArrayList<Llibre>, Void>() {
                @Override protected ArrayList<Llibre> doInBackground() {
                    return new ArrayList<>(ControladorMarcPrincipal.getInstance().aplicarFiltres(filter));
                }
                @Override protected void done() {
                    if (isCancelled()) return;
                    state.vista.obtenirBtnFiltrar().setEnabled(true);
                    try {
                        host.posarTable(get());
                    } catch (java.util.concurrent.CancellationException ignored) {
                        // Substituït per un worker més nou; el nou és propietari del resultat.
                    } catch (Exception ex) {
                        new herramienta.ui.DialegError(ex).mostrarErrorMessage();
                    }
                }
            };
            pendingFilterWorker.execute();
            return;
        }

        javax.swing.RowSorter<?> rs = state.vista.obtenirTaulaLlibres().getRowSorter();
        if (!(rs instanceof javax.swing.DefaultRowSorter)) {
            host.posarTable(new ArrayList<>(state.cd.aplicarFiltres(state.biblio, f)));
            return;
        }
        @SuppressWarnings("unchecked")
        javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer> drs =
            (javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer>) rs;

        ArrayList<Llibre> base = state.biblio instanceof ArrayList<Llibre> a ? a : new ArrayList<>(state.biblio);
        if (state.modelLibres != base) host.posarTable(base);

        if (!f.teAnyFilter()) {
            drs.setRowFilter(null);
            host.actualitzarTitleBar();
            return;
        }

        final Map<Long, Llibre> isbnMap = obtenirIsbnMap();
        final Integer tagId = f.obtenirTagId();
        final Integer llistaId = f.obtenirLlistaId();
        if (tagId != null || llistaId != null) {
            new SwingWorker<java.util.Map<String, java.util.Set<Long>>, Void>() {
                @Override protected java.util.Map<String, java.util.Set<Long>> doInBackground() {
                    java.util.Map<String, java.util.Set<Long>> sets = new java.util.HashMap<>();
                    sets.put("tag",     tagId     != null ? state.cd.obtenirLlibresWithTag(tagId) : null);
                    sets.put("llista",  llistaId  != null ? state.cd.obtenirLlibresInLlista(llistaId).stream().map(Llibre::obtenirISBN).collect(Collectors.toSet()) : null);
                    return sets;
                }
                @Override protected void done() {
                    if (isCancelled()) return;
                    java.util.Map<String, java.util.Set<Long>> sets;
                    try { sets = get(); }
                    catch (Exception ex) { new herramienta.ui.DialegError(ex).mostrarErrorMessage(); return; }
                    aplicarRowFilterInMemory(drs, f, isbnMap, sets.get("tag"), sets.get("llista"));
                    host.actualitzarTitleBar();
                }
            }.execute();
            return;
        }

        aplicarRowFilterInMemory(drs, f, isbnMap, null, null);
        host.actualitzarTitleBar();
    }

    private void aplicarRowFilterInMemory(
            javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer> drs,
            LlibreFilter f, Map<Long, Llibre> isbnMap,
            java.util.Set<Long> tagISBNs, java.util.Set<Long> llistaISBNs) {
        drs.setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel, Integer>() {
            @Override
            public boolean include(javax.swing.RowFilter.Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                try {
                    long isbn = Long.parseLong(entry.getStringValue(ModelTaulaBiblioteca.COL_ISBN));
                    Llibre l = isbnMap.get(isbn);
                    if (l == null) return false;
                    return FiltreUtils.matches(l, f, tagISBNs, llistaISBNs);
                } catch (Exception ignored) { return false; }
            }
        });
    }

    private record FilterTextField(String key, Supplier<String> get, Consumer<String> set) {}

    private FilterTextField[] textFilterFields() {
        var v = state.vista;
        return new FilterTextField[] {
            new FilterTextField("nom", () -> v.obtenirTextNom().getText(), v.obtenirTextNom()::setText),
            new FilterTextField("autor", () -> v.obtenirTextAutor().getText(), v.obtenirTextAutor()::setText),
            new FilterTextField("isbn", () -> v.obtenirTextISBN().getText(), v.obtenirTextISBN()::setText),
            new FilterTextField("anyMin", () -> v.obtenirAnyMin().getText(), v.obtenirAnyMin()::setText),
            new FilterTextField("anyMax", () -> v.obtenirAnyMax().getText(), v.obtenirAnyMax()::setText),
            new FilterTextField("valoracioMin", () -> v.obtenirValoracioMin().getText(), v.obtenirValoracioMin()::setText),
            new FilterTextField("valoracioMax", () -> v.obtenirValoracioMax().getText(), v.obtenirValoracioMax()::setText),
            new FilterTextField("preuMin", () -> v.obtenirPreuMin().getText(), v.obtenirPreuMin()::setText),
            new FilterTextField("preuMax", () -> v.obtenirPreuMax().getText(), v.obtenirPreuMax()::setText),
            new FilterTextField("editorial", () -> v.obtenirFilterEditorial().getText(), v.obtenirFilterEditorial()::setText),
            new FilterTextField("serie", () -> v.obtenirFilterSerie().getText(), v.obtenirFilterSerie()::setText),
            new FilterTextField("idioma", () -> v.obtenirFilterIdioma().getText(), v.obtenirFilterIdioma()::setText),
        };
    }

    void quitarFiltros() {
        state.vista.obtenirCasellaLlegit().setSelected(false);
        state.vista.obtenirCasellaNoLlegit().setSelected(false);
        for (FilterTextField f : textFilterFields()) f.set().accept("");
        if (state.vista.obtenirComboTagFilter().getItemCount() > 0)
            state.vista.obtenirComboTagFilter().setSelectedIndex(0);
        state.vista.obtenirFilterFormat().setSelectedIndex(0);
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    void aplicarSearchBar() {
        String query = state.vista.obtenirSearchBar().getText().trim();
        if (host.tableCtrl().highlightRenderer() != null)
            host.tableCtrl().highlightRenderer().posarSearchText(query);
        javax.swing.RowSorter<?> sorter = state.vista.obtenirTaulaLlibres().getRowSorter();
        if (!(sorter instanceof javax.swing.DefaultRowSorter)) { host.actualitzarTitleBar(); return; }
        @SuppressWarnings("unchecked")
        javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer> drs =
            (javax.swing.DefaultRowSorter<? extends javax.swing.table.TableModel, Integer>) sorter;
        if (query.isEmpty()) {
            drs.setRowFilter(null);
        } else {
            String q = query.toLowerCase(java.util.Locale.ROOT);
            Map<Long, Llibre> isbnMap = obtenirIsbnMap();
            drs.setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel, Integer>() {
                @Override
                public boolean include(javax.swing.RowFilter.Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        if (entry.getStringValue(i).toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
                    }
                    try {
                    long isbn = Long.parseLong(entry.getStringValue(ModelTaulaBiblioteca.COL_ISBN));
                        Llibre l = isbnMap.get(isbn);
                        if (l != null) {
                            String desc  = l.obtenirDescripcio();
                            String notes = l.obtenirNotes();
                            if (desc  != null && desc.toLowerCase(java.util.Locale.ROOT).contains(q))  return true;
                            if (notes != null && notes.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
                        }
                    } catch (Exception ignored) {}
                    return false;
                }
            });
        }
        host.actualitzarTitleBar();
    }

    void eliminarAlldataFiltros() {
        state.vista.obtenirTextISBN().setText("");
        state.vista.obtenirTextNom().setText("");
        state.vista.obtenirTextAutor().setText("");
    }

    Map<String, String> collectFilterState() {
        Map<String, String> m = new LinkedHashMap<>();
        for (FilterTextField f : textFilterFields()) m.put(f.key(), f.get().get());
        m.put("llegit", state.vista.obtenirCasellaLlegit().isSelected() ? "true"
                            : state.vista.obtenirCasellaNoLlegit().isSelected() ? "false" : "");
        String fmt = (String) state.vista.obtenirFilterFormat().getSelectedItem();
        m.put("format", fmt != null ? fmt : "");
        return m;
    }

    void aplicarFilterState(Map<String, String> s) {
        for (FilterTextField f : textFilterFields()) f.set().accept(s.getOrDefault(f.key(), ""));
        String llegit = s.getOrDefault("llegit", "");
        state.vista.obtenirCasellaLlegit().setSelected("true".equals(llegit));
        state.vista.obtenirCasellaNoLlegit().setSelected("false".equals(llegit));
        state.vista.obtenirFilterFormat().setSelectedItem(s.getOrDefault("format", ""));
    }

    private void carregarPreset() {
        int idx = state.vista.obtenirComboPresets().getSelectedIndex();
        if (idx < 0 || Configuracio.obtenirPresetCount() == 0) return;
        aplicarFilterState(Configuracio.carregarPreset(idx));
        filtrar();
    }

    private void desarPreset() {
        String name = JOptionPane.showInputDialog(state.vista, I18n.t("dlg_filter_name_prompt"),
            I18n.t("dlg_save_filter_title"), JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) return;
        Configuracio.desarPreset(name.trim(), collectFilterState());
        refrescarComboPresets();
        state.vista.obtenirComboPresets().setSelectedIndex(Configuracio.obtenirPresetCount() - 1);
    }

    private void esborrarPreset() {
        int idx = state.vista.obtenirComboPresets().getSelectedIndex();
        if (idx < 0 || Configuracio.obtenirPresetCount() == 0) return;
        String name = Configuracio.obtenirPresetName(idx);
        if (JOptionPane.showConfirmDialog(state.vista, I18n.t("dlg_delete_preset", name),
                I18n.t("dlg_delete_preset_title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Configuracio.eliminarPreset(idx);
        refrescarComboPresets();
    }

    private void refrescarComboPresets() {
        javax.swing.JComboBox<String> combo = state.vista.obtenirComboPresets();
        combo.removeAllItems();
        int n = Configuracio.obtenirPresetCount();
        if (n == 0) {
            combo.addItem(I18n.t("lbl_no_presets"));
            state.vista.obtenirBtnCarregaPreset().setEnabled(false);
            state.vista.obtenirBtnEsborraPreset().setEnabled(false);
        } else {
            for (int i = 0; i < n; i++) combo.addItem(Configuracio.obtenirPresetName(i));
            state.vista.obtenirBtnCarregaPreset().setEnabled(true);
            state.vista.obtenirBtnEsborraPreset().setEnabled(true);
        }
    }

    private <T> T parseField(JTextField field, String raw, Function<String, T> parser) {
        if (raw.isEmpty()) {
            netejarInvalidField(field);
            return null;
        }
        try {
            T value = parser.apply(raw);
            netejarInvalidField(field);
            return value;
        } catch (NumberFormatException e) {
            markInvalidField(field);
            return null;
        }
    }

    private Integer analitzarIntField(JTextField field) {
        return parseField(field, field.getText().trim(), Integer::parseInt);
    }

    private Long analitzarLongField(JTextField field, String raw) {
        return parseField(field, raw, Long::parseLong);
    }

    private Double analitzarDoubleField(JTextField field) {
        return parseField(field, field.getText().trim(), Double::parseDouble);
    }

    private void markInvalidField(JTextField field) {
        field.putClientProperty("JComponent.outline", "error");
        field.setToolTipText(I18n.t("validation_invalid_number"));
    }

    private void netejarInvalidField(JTextField field) {
        field.putClientProperty("JComponent.outline", null);
        field.setToolTipText(null);
    }
}




