package presentacio;

import herramienta.I18n;

/** Updates pagination widgets on {@link MostrarBibliotecaPanel}. */
public class PaginationView {

    private final MostrarBibliotecaPanel vista;

    public PaginationView(MostrarBibliotecaPanel vista) {
        this.vista = vista;
    }

    /** @param page zero-based page index */
    public void apply(int page, int totalPages) {
        boolean show = totalPages > 1;
        vista.getPaginationPanel().setVisible(show);
        if (!show) return;
        vista.getLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
        vista.getBtnPaginaAnterior().setEnabled(page > 0);
        vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
    }

    public void hide() {
        vista.getPaginationPanel().setVisible(false);
    }
}
