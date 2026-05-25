package presentacio.detalles.vista;

import javax.swing.JScrollPane;

/** General fields tab for {@link DetallesLlibrePanel}. */
final class DetallesGeneralTab {
    private DetallesGeneralTab() {}
    static JScrollPane build(DetallesLlibrePanel panel) { return panel.buildGeneralTab(); }
}
