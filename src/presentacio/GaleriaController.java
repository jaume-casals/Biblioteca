package presentacio;

import domini.Llibre;

import java.util.ArrayList;
import java.util.List;

/** Thin coordinator for {@link GaleriaCobertesPanel} book list updates. */
public class GaleriaController {

    private final GaleriaCobertesPanel panel;

    public GaleriaController(GaleriaCobertesPanel panel) {
        this.panel = panel;
    }

    public void onBooksUpdated(List<Llibre> books) {
        panel.updateLlibres(books instanceof ArrayList<Llibre> a ? a : new ArrayList<>(books));
    }
}
