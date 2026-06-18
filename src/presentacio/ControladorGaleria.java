package presentacio;

import domini.Llibre;

import java.util.ArrayList;
import java.util.List;

/** Coordinador prim per a les actualitzacions de la llista de llibres del {@link GaleriaCobertesPanel}. */
public class ControladorGaleria {

    private final PanelGaleriaCobertes panel;

    public ControladorGaleria(PanelGaleriaCobertes panel) {
        this.panel = panel;
    }

    public void onBooksUpdated(List<Llibre> books) {
        panel.actualitzarLlibres(books instanceof ArrayList<Llibre> a ? a : new ArrayList<>(books));
    }
}
