package presentacio.galeria;

import domini.Llibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Política d'evicció de {@link ServeiImatgesCoberta}.
 *
 * <p>La memòria cau és FIFO (evicció per ordre d'inserció), no LRU: el
 * {@code LinkedHashMap} usa {@code accessOrder=false} de manera deliberada
 * perquè un {@code get()} que reordenés la llista corromperia les lectures
 * concurrents protegides pel lock de lectura.
 *
 * <p>Aquest test fixa aquesta política: llegir una entrada NO la salva de
 * l'expulsió. Si algú convertís la memòria cau en LRU només posant
 * {@code accessOrder=true}, aquest test fallaria.
 */
class TestServeiImatgesCobertaTest {

    private static final int CAPACITAT = 150;

    private ServeiImatgesCoberta servei;
    private File png;

    @BeforeEach
    void setup() throws Exception {
        servei = new ServeiImatgesCoberta();
        png = File.createTempFile("coberta_test_", ".png");
        png.deleteOnExit();
        BufferedImage img = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", png);
    }

    @AfterEach
    void tearDown() {
        if (servei != null) servei.shutdown();
        if (png != null) png.delete();
    }

    @Test
    @DisplayName("La memòria cau expulsa l'entrada més antiga inserida quan supera la capacitat")
    void expulsaLaMesAntigaInserida() throws Exception {
        carrega(1L, CAPACITAT + 1);

        assertThat(servei.obtenirCached(1L))
            .as("la primera entrada inserida s'ha d'expulsar quan se supera la capacitat")
            .isNull();
        assertThat(servei.obtenirCached((long) CAPACITAT + 1))
            .as("l'entrada més recent ha de continuar a la memòria cau")
            .isNotNull();
    }

    @Test
    @DisplayName("Llegir una entrada no la salva de l'expulsió: la política és FIFO, no LRU")
    void llegirNoSalvaDeLEviccio() throws Exception {
        carrega(1L, CAPACITAT);

        // Llegeix la primera entrada moltes vegades: amb una política LRU
        // quedaria al davant de la cua d'expulsió.
        for (int i = 0; i < 20; i++) {
            assertThat(servei.obtenirCached(1L)).isNotNull();
        }

        // Una entrada nova obliga a expulsar la més antiga inserida.
        carrega((long) CAPACITAT + 1, 1);

        assertThat(servei.obtenirCached(1L))
            .as("amb FIFO, l'entrada més antiga s'expulsa encara que s'acabi de llegir")
            .isNull();
        assertThat(servei.obtenirCached((long) CAPACITAT + 1)).isNotNull();
    }

    /**
     * Carrega n llibres amb ISBN isbnInicial..isbnInicial+n-1, un d'un en un
     * i esperant el callback de cadascun.
     *
     * <p>La càrrega ha de ser seqüencial: el pool de loader té quatre fils,
     * de manera que en paral·lel l'ordre d'inserció a la memòria cau (i per
     * tant l'ordre d'expulsió) no seria determinista.
     */
    private void carrega(long isbnInicial, int quantitat) throws Exception {
        for (int i = 0; i < quantitat; i++) {
            long isbn = isbnInicial + i;
            Llibre l = new Llibre(isbn, "Llibre " + isbn, "Autor", 2000, "", 0.0, 0.0, false,
                png.getAbsolutePath());
            CountDownLatch pendent = new CountDownLatch(1);
            servei.submit(l, 40, 60, pendent::countDown);
            assertThat(pendent.await(30, TimeUnit.SECONDS))
                .as("la càrrega de l'ISBN " + isbn + " ha de completar")
                .isTrue();
        }
    }
}
