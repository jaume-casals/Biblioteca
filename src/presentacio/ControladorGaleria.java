package presentacio;

import domini.Llibre;

import java.util.ArrayList;
import java.util.List;

/** Thin coordinator for {@link GaleriaCobertesPanel} book list updates. */
public class ControladorGaleria {

    private final PanelGaleriaCobertes panel;

    public ControladorGaleria(PanelGaleriaCobertes panel) {
        this.panel = panel;
    }

    public void onBooksUpdated(List<Llibre> books) {
        panel.actualitzarLlibres(books instanceof ArrayList<Llibre> a ? a : new ArrayList<>(books));
    }
}
