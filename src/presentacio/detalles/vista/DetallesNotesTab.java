package presentacio.detalles.vista;

import javax.swing.JPanel;

/** Notes tab for {@link DetallesLlibrePanel}. */
final class DetallesNotesTab {
    private DetallesNotesTab() {}
    static JPanel build(DetallesLlibrePanel panel) { return panel.buildNotesTab(); }
}
