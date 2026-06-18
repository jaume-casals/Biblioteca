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
 * underlying {@link RegistreCampsFormulari} must be consistent with the
 * getters.
 */
class TestPanelCalaixFiltre {

    @Test
    @DisplayName("Panel constructs cleanly (no exceptions, registry populated)")
    void constructs() {
        PanelCalaixFiltre panel = new PanelCalaixFiltre();
        assertThat(panel.obtenirPanelFiltros()).isNotNull();
        assertThat(panel.obtenirScrolpaneFiltro()).isNotNull();
        assertThat(panel.obtenirRegistry()).isNotNull();
        assertThat(panel.obtenirRegistry().components()).isNotEmpty();
    }

    @Test
    @DisplayName("All 32 public getters return non-null components")
    void allGettersReturnComponents() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();

        assertThat(p.obtenirTextISBN()).isNotNull();
        assertThat(p.obtenirTextNom()).isNotNull();
        assertThat(p.obtenirTextAutor()).isNotNull();
        assertThat(p.obtenirAnyMin()).isNotNull();
        assertThat(p.obtenirAnyMax()).isNotNull();
        assertThat(p.obtenirValoracioMin()).isNotNull();
        assertThat(p.obtenirValoracioMax()).isNotNull();
        assertThat(p.obtenirPreuMin()).isNotNull();
        assertThat(p.obtenirPreuMax()).isNotNull();

        assertThat(p.obtenirCasellaLlegit()).isNotNull();
        assertThat(p.obtenirCasellaNoLlegit()).isNotNull();

        assertThat(p.obtenirBtnFiltrar()).isNotNull();
        assertThat(p.obtenirBtnQuitarFiltros()).isNotNull();

        assertThat(p.obtenirComboTagFilter()).isNotNull();

        assertThat(p.obtenirFilterEditorial()).isNotNull();
        assertThat(p.obtenirFilterSerie()).isNotNull();
        assertThat(p.obtenirFilterIdioma()).isNotNull();
        assertThat(p.obtenirFilterFormat()).isNotNull();

        assertThat(p.obtenirComboPresets()).isNotNull();

        assertThat(p.obtenirBtnCarregaPreset()).isNotNull();
        assertThat(p.obtenirBtnDesaPreset()).isNotNull();
        assertThat(p.obtenirBtnEsborraPreset()).isNotNull();

        assertThat(p.obtenirBtnExportCSV()).isNotNull();
        assertThat(p.obtenirBtnImportarCSV()).isNotNull();
        assertThat(p.obtenirBtnImportarCalibre()).isNotNull();
        assertThat(p.obtenirBtnExportJSON()).isNotNull();
        assertThat(p.obtenirBtnImportarJSON()).isNotNull();
        assertThat(p.obtenirBtnExportHTML()).isNotNull();
        assertThat(p.obtenirBtnExportPDF()).isNotNull();

        assertThat(p.obtenirBtnFetchCovers()).isNotNull();
        assertThat(p.obtenirBtnEscanejarISBN()).isNotNull();
        assertThat(p.obtenirBtnBackupBD()).isNotNull();
        assertThat(p.obtenirBtnRestaurarBD()).isNotNull();
    }

    @Test
    @DisplayName("Text-field getters return JTextField instances (typed correctly)")
    void textGettersReturnTextFields() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirTextISBN()).isInstanceOf(JTextField.class);
        assertThat(p.obtenirAnyMin()).isInstanceOf(JTextField.class);
        assertThat(p.obtenirPreuMax()).isInstanceOf(JTextField.class);
        assertThat(p.obtenirFilterEditorial()).isInstanceOf(JTextField.class);
    }

    @Test
    @DisplayName("Checkbox getters return JCheckBox instances")
    void checkboxGettersReturnCheckBoxes() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirCasellaLlegit()).isInstanceOf(JCheckBox.class);
        assertThat(p.obtenirCasellaNoLlegit()).isInstanceOf(JCheckBox.class);
    }

    @Test
    @DisplayName("Button getters return JButton instances")
    void buttonGettersReturnButtons() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirBtnFiltrar()).isInstanceOf(JButton.class);
        assertThat(p.obtenirBtnBackupBD()).isInstanceOf(JButton.class);
        assertThat(p.obtenirBtnExportCSV()).isInstanceOf(JButton.class);
    }

    @Test
    @DisplayName("Combo getters return JComboBox instances")
    void comboGettersReturnCombos() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirComboTagFilter()).isInstanceOf(JComboBox.class);
        assertThat(p.obtenirFilterFormat()).isInstanceOf(JComboBox.class);
        assertThat(p.obtenirComboPresets()).isInstanceOf(JComboBox.class);
    }

    @Test
    @DisplayName("Filter range fields: min/max getters return distinct, non-null components")
    void rangeFieldsAreDistinct() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirAnyMin()).isNotSameAs(p.obtenirAnyMax());
        assertThat(p.obtenirValoracioMin()).isNotSameAs(p.obtenirValoracioMax());
        assertThat(p.obtenirPreuMin()).isNotSameAs(p.obtenirPreuMax());
    }

    @Test
    @DisplayName("Text fields start with empty text and are editable")
    void textFieldsStartEmptyAndEditable() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirTextISBN().getText()).isEmpty();
        assertThat(p.obtenirTextNom().getText()).isEmpty();
        assertThat(p.obtenirTextAutor().getText()).isEmpty();
        assertThat(p.obtenirAnyMin().getText()).isEmpty();
        assertThat(p.obtenirPreuMax().getText()).isEmpty();
        assertThat(p.obtenirTextISBN().isEditable()).isTrue();
    }

    @Test
    @DisplayName("Text fields accept and round-trip typed text")
    void textFieldsAcceptText() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        p.obtenirTextISBN().setText("978-3-16-148410-0");
        p.obtenirTextNom().setText("Cien años de soledad");
        p.obtenirAnyMin().setText("1950");
        p.obtenirPreuMax().setText("99.95");

        assertThat(p.obtenirTextISBN().getText()).isEqualTo("978-3-16-148410-0");
        assertThat(p.obtenirTextNom().getText()).isEqualTo("Cien años de soledad");
        assertThat(p.obtenirAnyMin().getText()).isEqualTo("1950");
        assertThat(p.obtenirPreuMax().getText()).isEqualTo("99.95");
    }

    @Test
    @DisplayName("Filter format combo is pre-populated with empty + 4 format options")
    void filtrarFormatIsPrepopulated() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        JComboBox<String> fmt = p.obtenirFilterFormat();
        assertThat(fmt.getItemCount()).isEqualTo(5);
        assertThat(fmt.getItemAt(0)).isEqualTo("");
    }

    @Test
    @DisplayName("Llegit/NoLlegit checkboxes start unchecked and can be toggled")
    void checkboxesToggle() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        assertThat(p.obtenirCasellaLlegit().isSelected()).isFalse();
        assertThat(p.obtenirCasellaNoLlegit().isSelected()).isFalse();

        p.obtenirCasellaLlegit().setSelected(true);
        assertThat(p.obtenirCasellaLlegit().isSelected()).isTrue();

        p.obtenirCasellaNoLlegit().setSelected(true);
        assertThat(p.obtenirCasellaNoLlegit().isSelected()).isTrue();
    }

    @Test
    @DisplayName("applyTheme and applyThemePostLaf do not throw")
    void themeApplicationDoesNotThrow() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        p.aplicarTheme();
        p.aplicarThemePostLaf();
    }

    @Test
    @DisplayName("Registry specs cover the 32 public getters (1:1 mapping)")
    void registryCoversAllGetters() {
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        RegistreCampsFormulari r = p.obtenirRegistry();
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
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        var textSpecs = p.obtenirRegistry().specsOfKind(RegistreCampsFormulari.Tipus.TEXT);
        var keys = textSpecs.stream().map(RegistreCampsFormulari.Camp::key).toList();
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
        PanelCalaixFiltre p = new PanelCalaixFiltre();
        var checks = p.obtenirRegistry().specsOfKind(RegistreCampsFormulari.Tipus.CHECK);
        assertThat(checks).hasSize(2);
        assertThat(checks.stream().map(RegistreCampsFormulari.Camp::key))
            .containsExactlyInAnyOrder("chckbxLlegit", "chckbxNoLlegit");

        var combos = p.obtenirRegistry().specsOfKind(RegistreCampsFormulari.Tipus.COMBO);
        assertThat(combos.stream().map(RegistreCampsFormulari.Camp::key))
            .containsExactlyInAnyOrder("comboPresets", "comboTagFilter", "filterFormat");
    }
}
