package presentacio.util;

import domini.Llibre;
import javax.swing.JButton;
import presentacio.controladors.ControladorAccionsLlibre;
import presentacio.controladors.ControladorFiltre;
import presentacio.controladors.ControladorMenuContextual;
import presentacio.controladors.ControladorModeVista;
import presentacio.controladors.ControladorMostrarBiblioteca;
import presentacio.controladors.ControladorPaginaTaula;
import presentacio.controladors.ControladorPrestatgeria;
import presentacio.controladors.ControladorTaula;


/**
 * Callbacks dels sub-controladors cap a {@link ControladorMostrarBiblioteca}.
 *
 * <p>Actua com un registre prim de controladors: cada sub-controlador es
 * pot obtenir a través d'un accessor amb tipus en lloc d'una referència
 * directa a un camp del coordinador. El "punt d'injecció únic" que
 * reclamava la troballa MEDIUM de tot.txt és el constructor de
 * {@link ControladorMostrarBiblioteca} — els controladors es construeixen
 * allà i es connecten via els accessors següents, de manera que afegir un
 * 7è controlador només implica editar el constructor (i afegir un
 * accessor d'una línia aquí).
 */
public interface AmfitrioPantallaBiblioteca {
    void posarTable(java.util.List<Llibre> llibres);
    void mostrarPage(int page);
    void refrescarAll();
    void actualitzarTitleBar();
    void refrescarComboLlistes();
    void refrescarComboTags();
    void refreshRow(Llibre l);
    void removeRow(Llibre l);
    void afegirRow(Llibre l);
    ControladorPaginaTaula pageCtrl();
    ControladorTaula tableCtrl();
    ControladorFiltre filtrarCtrl();
    ControladorPrestatgeria shelfCtrl();
    ControladorAccionsLlibre bookActionsCtrl();
    ControladorMenuContextual contextMenuCtrl();
    ControladorModeVista viewModeCtrl();
    JButton detallesBtn();
    EstatVistaBiblioteca state();
}








