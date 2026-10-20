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
    /**
     * Aplica un resultat a la taula i a la galeria només si la generació
     * indicada continua vigent. La taula i la galeria representen la mateixa
     * consulta, de manera que comparteixen la generació.
     */
    void aplicarResultat(CoordinadorGeneracions.Fulla<?> fulla, java.util.List<Llibre> llibres);
    /**
     * Igual que {@link #aplicarResultat}, però indica quantos llibres
     * coincideixen amb l'àmbit i el filtre vigents.
     *
     * <p>El recompte visible surt d'aquest valor: és l'única font del
     * "quantos llibres hi ha" i evita que el comptador i el contingut
     * mostrin xifres contradictòries.
     *
     * @param ambit llibres que coincideixen amb l'àmbit i el filtre
     */
    void aplicarResultat(CoordinadorGeneracions.Fulla<?> fulla, java.util.List<Llibre> llibres, int ambit);
    /**
     * Actualitza el recompte de llibres visible sense reaplicar el resultat.
     *
     * <p>S'usa quan un filtre o una cerca redueixen les files que es mostren
     * sense canviar la consulta: el recompte ha de reflectir el que l'usuari
     * veu, no el que hi ha abans del filtre.
     */
    void posarRecompte(int enAmbit, int mostrats);
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








