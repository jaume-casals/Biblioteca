package presentacio;

import domini.Llibre;

import javax.swing.JButton;

/**
 * Callbacks from sub-controllers back to {@link MostrarBibliotecaControl}.
 *
 * <p>Acts as a thin controller registry: every sub-controller can be
 * obtained through a typed accessor instead of via a direct field
 * reference on the coordinator. The "single injection point" the
 * tot.txt MEDIUM finding called for is the constructor of
 * {@link MostrarBibliotecaControl} — controllers are constructed there
 * and wired via the accessors below, so adding a 7th controller means
 * editing only the constructor (and adding a one-line accessor here).
 */
interface LibraryScreenHost {
    void setTable(java.util.List<Llibre> llibres);
    void showPage(int page);
    void refreshAll();
    void updateTitleBar();
    void refreshComboLlistes();
    void refreshComboTags();
    void refreshRow(Llibre l);
    void removeRow(Llibre l);
    void appendRow(Llibre l);
    TablePageController pageCtrl();
    TableController tableCtrl();
    FilterController filterCtrl();
    ShelfController shelfCtrl();
    BookActionsController bookActionsCtrl();
    ContextMenuController contextMenuCtrl();
    ViewModeController viewModeCtrl();
    JButton detallesBtn();
    LibraryViewState state();
}
