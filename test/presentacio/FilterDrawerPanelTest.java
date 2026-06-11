package presentacio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the public surface of {@link FilterDrawerPanel} after the R1
 * declarative-registry refactor: every one of the 32 public getters
 * must still return a live, non-null Swing component, and the
 * underlying {@link FormFieldRegistry} must be consistent with the
 * getters.
 */
class FilterDrawerPanelTest {

    @Test
    @DisplayName("Panel constructs cleanly (no exceptions, registry populated)")
    void constructs() {
        FilterDrawerPanel panel = new FilterDrawerPanel();
        assertThat(panel.getPanelFiltros()).isNotNull();
        assertThat(panel.getScrolpaneFiltro()).isNotNull();
        assertThat(panel.getRegistry()).isNotNull();
        assertThat(panel.getRegistry().components()).isNotEmpty();
    }

    @Test
    @DisplayName("All 32 public getters return non-null components")
    void allGettersReturnComponents() {
        FilterDrawerPanel p = new FilterDrawerPanel();

        assertThat(p.getTextISBN()).isNotNull();
        assertThat(p.getTextNom()).isNotNull();
        assertThat(p.getTextAutor()).isNotNull();
        assertThat(p.getAnyMin()).isNotNull();
        assertThat(p.getAnyMax()).isNotNull();
        assertThat(p.getValoracioMin()).isNotNull();
        assertThat(p.getValoracioMax()).isNotNull();
        assertThat(p.getPreuMin()).isNotNull();
        assertThat(p.getPreuMax()).isNotNull();

        assertThat(p.getchckbxLlegit()).isNotNull();
        assertThat(p.getchckbxNoLlegit()).isNotNull();

        assertThat(p.getbtnFiltrar()).isNotNull();
        assertThat(p.getBtnQuitarFiltros()).isNotNull();

        assertThat(p.getComboTagFilter()).isNotNull();

        assertThat(p.getFilterEditorial()).isNotNull();
        assertThat(p.getFilterSerie()).isNotNull();
        assertThat(p.getFilterIdioma()).isNotNull();
        assertThat(p.getFilterFormat()).isNotNull();

        assertThat(p.getComboPresets()).isNotNull();

        assertThat(p.getBtnCarregaPreset()).isNotNull();
        assertThat(p.getBtnDesaPreset()).isNotNull();
        assertThat(p.getBtnEsborraPreset()).isNotNull();

        assertThat(p.getBtnExportCSV()).isNotNull();
        assertThat(p.getBtnImportarCSV()).isNotNull();
        assertThat(p.getBtnImportarCalibre()).isNotNull();
        assertThat(p.getBtnExportJSON()).isNotNull();
        assertThat(p.getBtnImportarJSON()).isNotNull();
        assertThat(p.getBtnExportHTML()).isNotNull();
        assertThat(p.getBtnExportPDF()).isNotNull();

        assertThat(p.getBtnFetchCovers()).isNotNull();
        assertThat(p.getBtnEscanejarISBN()).isNotNull();
        assertThat(p.getBtnBackupBD()).isNotNull();
        assertThat(p.getBtnRestaurarBD()).isNotNull();
    }

    @Test
    @DisplayName("Text-field getters return JTextField instances (typed correctly)")
    void textGettersReturnTextFields() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getTextISBN()).isInstanceOf(JTextField.class);
        assertThat(p.getAnyMin()).isInstanceOf(JTextField.class);
        assertThat(p.getPreuMax()).isInstanceOf(JTextField.class);
        assertThat(p.getFilterEditorial()).isInstanceOf(JTextField.class);
    }

    @Test
    @DisplayName("Checkbox getters return JCheckBox instances")
    void checkboxGettersReturnCheckBoxes() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getchckbxLlegit()).isInstanceOf(JCheckBox.class);
        assertThat(p.getchckbxNoLlegit()).isInstanceOf(JCheckBox.class);
    }

    @Test
    @DisplayName("Button getters return JButton instances")
    void buttonGettersReturnButtons() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getbtnFiltrar()).isInstanceOf(JButton.class);
        assertThat(p.getBtnBackupBD()).isInstanceOf(JButton.class);
        assertThat(p.getBtnExportCSV()).isInstanceOf(JButton.class);
    }

    @Test
    @DisplayName("Combo getters return JComboBox instances")
    void comboGettersReturnCombos() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getComboTagFilter()).isInstanceOf(JComboBox.class);
        assertThat(p.getFilterFormat()).isInstanceOf(JComboBox.class);
        assertThat(p.getComboPresets()).isInstanceOf(JComboBox.class);
    }

    @Test
    @DisplayName("Filter range fields: min/max getters return distinct, non-null components")
    void rangeFieldsAreDistinct() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getAnyMin()).isNotSameAs(p.getAnyMax());
        assertThat(p.getValoracioMin()).isNotSameAs(p.getValoracioMax());
        assertThat(p.getPreuMin()).isNotSameAs(p.getPreuMax());
    }

    @Test
    @DisplayName("Text fields start with empty text and are editable")
    void textFieldsStartEmptyAndEditable() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getTextISBN().getText()).isEmpty();
        assertThat(p.getTextNom().getText()).isEmpty();
        assertThat(p.getTextAutor().getText()).isEmpty();
        assertThat(p.getAnyMin().getText()).isEmpty();
        assertThat(p.getPreuMax().getText()).isEmpty();
        assertThat(p.getTextISBN().isEditable()).isTrue();
    }

    @Test
    @DisplayName("Text fields accept and round-trip typed text")
    void textFieldsAcceptText() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        p.getTextISBN().setText("978-3-16-148410-0");
        p.getTextNom().setText("Cien años de soledad");
        p.getAnyMin().setText("1950");
        p.getPreuMax().setText("99.95");

        assertThat(p.getTextISBN().getText()).isEqualTo("978-3-16-148410-0");
        assertThat(p.getTextNom().getText()).isEqualTo("Cien años de soledad");
        assertThat(p.getAnyMin().getText()).isEqualTo("1950");
        assertThat(p.getPreuMax().getText()).isEqualTo("99.95");
    }

    @Test
    @DisplayName("Filter format combo is pre-populated with empty + 4 format options")
    void filterFormatIsPrepopulated() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        JComboBox<String> fmt = p.getFilterFormat();
        assertThat(fmt.getItemCount()).isEqualTo(5);
        assertThat(fmt.getItemAt(0)).isEqualTo("");
    }

    @Test
    @DisplayName("Llegit/NoLlegit checkboxes start unchecked and can be toggled")
    void checkboxesToggle() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        assertThat(p.getchckbxLlegit().isSelected()).isFalse();
        assertThat(p.getchckbxNoLlegit().isSelected()).isFalse();

        p.getchckbxLlegit().setSelected(true);
        assertThat(p.getchckbxLlegit().isSelected()).isTrue();

        p.getchckbxNoLlegit().setSelected(true);
        assertThat(p.getchckbxNoLlegit().isSelected()).isTrue();
    }

    @Test
    @DisplayName("applyTheme and applyThemePostLaf do not throw")
    void themeApplicationDoesNotThrow() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        p.applyTheme();
        p.applyThemePostLaf();
    }

    @Test
    @DisplayName("Registry specs cover the 32 public getters (1:1 mapping)")
    void registryCoversAllGetters() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        FormFieldRegistry r = p.getRegistry();
        assertThat(r.has("textISBN")).isTrue();
        assertThat(r.has("textNom")).isTrue();
        assertThat(r.has("textAutor")).isTrue();
        assertThat(r.has("anyMin")).isTrue();
        assertThat(r.has("anyMax")).isTrue();
        assertThat(r.has("valoracioMin")).isTrue();
        assertThat(r.has("valoracioMax")).isTrue();
        assertThat(r.has("preuMin")).isTrue();
        assertThat(r.has("preuMax")).isTrue();
        assertThat(r.has("chckbxLlegit")).isTrue();
        assertThat(r.has("chckbxNoLlegit")).isTrue();
        assertThat(r.has("bttnFiltrar")).isTrue();
        assertThat(r.has("bttnQuitarFiltros")).isTrue();
        assertThat(r.has("comboTagFilter")).isTrue();
        assertThat(r.has("filterEditorial")).isTrue();
        assertThat(r.has("filterSerie")).isTrue();
        assertThat(r.has("filterIdioma")).isTrue();
        assertThat(r.has("filterFormat")).isTrue();
        assertThat(r.has("comboPresets")).isTrue();
        assertThat(r.has("btnCarregaPreset")).isTrue();
        assertThat(r.has("btnDesaPreset")).isTrue();
        assertThat(r.has("btnEsborraPreset")).isTrue();
        assertThat(r.has("btnExportCSV")).isTrue();
        assertThat(r.has("btnImportarCSV")).isTrue();
        assertThat(r.has("btnImportarCalibre")).isTrue();
        assertThat(r.has("btnExportJSON")).isTrue();
        assertThat(r.has("btnImportarJSON")).isTrue();
        assertThat(r.has("btnExportHTML")).isTrue();
        assertThat(r.has("btnExportPDF")).isTrue();
        assertThat(r.has("btnFetchCovers")).isTrue();
        assertThat(r.has("btnEscanejarISBN")).isTrue();
        assertThat(r.has("btnBackupBD")).isTrue();
        assertThat(r.has("btnRestaurarBD")).isTrue();
        assertThat(r.has("btnExportDropdown")).isTrue();
        assertThat(r.has("btnImportDropdown")).isTrue();
    }

    @Test
    @DisplayName("Registry text-specs cover exactly the 12 filter inputs that trigger filtrar()")
    void registryTextSpecsCoverFiltrarInputs() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        var textSpecs = p.getRegistry().specsOfKind(FormFieldRegistry.Kind.TEXT);
        var keys = textSpecs.stream().map(FormFieldRegistry.Field::key).toList();
        assertThat(keys).containsExactlyInAnyOrder(
            "textISBN", "textNom", "textAutor",
            "anyMin", "anyMax",
            "valoracioMin", "valoracioMax",
            "preuMin", "preuMax",
            "filterEditorial", "filterSerie", "filterIdioma");
    }

    @Test
    @DisplayName("Registry specsOfKind returns the right typed lists")
    void registrySpecsByKind() {
        FilterDrawerPanel p = new FilterDrawerPanel();
        var checks = p.getRegistry().specsOfKind(FormFieldRegistry.Kind.CHECK);
        assertThat(checks).hasSize(2);
        assertThat(checks.stream().map(FormFieldRegistry.Field::key))
            .containsExactlyInAnyOrder("chckbxLlegit", "chckbxNoLlegit");

        var combos = p.getRegistry().specsOfKind(FormFieldRegistry.Kind.COMBO);
        assertThat(combos.stream().map(FormFieldRegistry.Field::key))
            .containsExactlyInAnyOrder("comboPresets", "comboTagFilter", "filterFormat");
    }
}
