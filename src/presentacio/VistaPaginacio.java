package presentacio;

import herramienta.I18n;

/** Actualitza els widgets de paginació a {@link PanelMostrarBiblioteca}. */
public class VistaPaginacio {

    private final PanelMostrarBiblioteca vista;

    public VistaPaginacio(PanelMostrarBiblioteca vista) {
        this.vista = vista;
    }

    /** @param page índex de pàgina començant des de zero */
    public void apply(int page, int totalPages) {
        boolean show = totalPages > 1;
        vista.obtenirPaginationPanel().setVisible(show);
        if (!show) return;
        vista.obtenirLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
        vista.obtenirBtnPaginaAnterior().setEnabled(page > 0);
        vista.obtenirBtnPaginaSeguent().setEnabled(page < totalPages - 1);
    }

    public void hide() {
        vista.obtenirPaginationPanel().setVisible(false);
    }
}
