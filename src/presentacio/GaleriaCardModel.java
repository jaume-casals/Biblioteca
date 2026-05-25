package presentacio;

import java.awt.image.BufferedImage;

import domini.Llibre;

/** One card in the cover gallery view. */
public record GaleriaCardModel(Llibre book, BufferedImage image, boolean selected) {
    public GaleriaCardModel(Llibre book) {
        this(book, null, false);
    }
}
