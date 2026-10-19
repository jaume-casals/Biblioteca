package presentacio.controladors;

import domini.Llibre;
import java.util.ArrayList;
import java.util.List;
import presentacio.panells.PanelGaleriaCobertes;
import presentacio.util.CoordinadorGeneracions;


/**
 * Coordinador prim per a les actualitzacions de la llista de llibres del {@link GaleriaCobertesPanel}.
 *
 * <p>La galeria i la taula representen la mateixa consulta, de manera que
 * comparteixen el mateix flux de generacions
 * ({@link CoordinadorGeneracions#FLUX_CONSULTA}). Una generació obsoleta
 * que arribi a la galeria és tan invàlida com la que arribaria a la taula:
 * per això el mètode d'actualització comprova la vigència de la generació
 * just abans de tocar el panell.
 */
public class ControladorGaleria {

    private final PanelGaleriaCobertes panel;
    private final CoordinadorGeneracions generacions;

    public ControladorGaleria(PanelGaleriaCobertes panel, CoordinadorGeneracions generacions) {
        this.panel = panel;
        this.generacions = generacions;
    }

    /**
     * Actualitza la galeria si la generació del resultat continua vigent.
     *
     * @param fulla generació del resultat que origina la actualització
     * @param books llibres a mostrar
     */
    public void actualizarSiVigent(CoordinadorGeneracions.Fulla<?> fulla, List<Llibre> books) {
        if (fulla != null && !fulla.esVigent()) return;
        if (generacions != null && generacions.esTancat()) return;
        panel.actualitzarLlibres(books instanceof ArrayList<Llibre> a ? a : new ArrayList<>(books));
    }

    public void onBooksUpdated(List<Llibre> books) {
        actualizarSiVigent(null, books);
    }
}
