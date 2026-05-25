package presentacio.detalles.vista;

import javax.swing.JScrollPane;

/** Advanced metadata tab for {@link DetallesLlibrePanel}. */
final class DetallesAvancatTab {
    private DetallesAvancatTab() {}
    static JScrollPane build(DetallesLlibrePanel panel) { return panel.buildAdvancedTab(); }
}
