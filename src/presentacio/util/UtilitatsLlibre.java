package presentacio.util;

import domini.Llibre;
import java.util.ArrayList;
import java.util.List;

public final class UtilitatsLlibre {

    private UtilitatsLlibre() {}

    public static ArrayList<Llibre> asArrayList(List<Llibre> books) {
        return books instanceof ArrayList<Llibre> a ? a : new ArrayList<>(books);
    }
}
