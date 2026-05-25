package presentacio;

import domini.Llibre;

import javax.swing.JButton;
import java.util.ArrayList;

/** Callbacks from sub-controllers back to {@link MostrarBibliotecaControl}. */
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
    JButton detallesBtn();
    LibraryViewState state();
}
