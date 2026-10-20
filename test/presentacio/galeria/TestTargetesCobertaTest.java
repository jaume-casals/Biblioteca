package presentacio.galeria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import domini.Llibre;
import herramienta.ui.UITheme;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves del pintat de les targetes de la galeria.
 *
 * <p>La targeta es desa a memòria cau en diverses imatges pre-renderitzades
 * (fons i marcador de portada). Aquestes imatges duen els colors del tema
 * amb què s'han creat: si no es tornen a fer quan el tema canvia, la galeria
 * mostra la meitat en colors del tema antic i la meitat en colors del nou.
 */
class TestTargetesCobertaTest {

    private static final int CARD_W = 140;
    private static final int COVER_H = 200;
    private static final int CARD_H = COVER_H + 60;

    @Test
    @DisplayName("El marcador de portada es torna a fer quan canvia el tema")
    void elMarcadorDePortadaSegueixElTema() {
        UITheme.Tema original = UITheme.obtenirTheme();
        try {
            UITheme.posarTheme(UITheme.Tema.DARK);
            ServeiImatgesCoberta servei = new ServeiImatgesCoberta();
            FabricantTargetesCoberta fabrica =
                new FabricantTargetesCoberta(servei, CARD_W, CARD_H, COVER_H);
            JPanel card = fabrica.build(llibreSenseCoberta(), 0, amfitrioBuit(), "ca");
            card.setSize(card.getPreferredSize());

            Color fosc = paint(card);

            // La mateixa targeta es torna a pintar amb el tema nou: el
            // marcador que estava a memòria cau ha de tornar a fer-se.
            UITheme.posarTheme(UITheme.Tema.LIGHT);
            Color clar = paint(card);

            // En mode fosc el marcador és fosc; en mode clar, clar. Si el
            // marcador es quedés a la memòria cau, els dos valors serien
            // iguals i la galeria quedaria a mitjos pintar.
            assertThat(luminositat(fosc)).isLessThan(128);
            assertThat(luminositat(clar)).isGreaterThan(128);
        } finally {
            UITheme.posarTheme(original);
        }
    }

    @Test
    @DisplayName("El fons pre-renderitzat de la targeta es reinvalida en canviar de tema")
    void elFonsPreRenderitzatEsReinvalida() {
        UITheme.Tema original = UITheme.obtenirTheme();
        try {
            UITheme.posarTheme(UITheme.Tema.LIGHT);
            ServeiImatgesCoberta servei = new ServeiImatgesCoberta();
            FabricantTargetesCoberta fabrica =
                new FabricantTargetesCoberta(servei, CARD_W, CARD_H, COVER_H);

            Color abans = mostraElFons(fabrica);
            UITheme.posarTheme(UITheme.Tema.DARK);
            // El que fa la galeria quan canvia de tema: sense invalidar, el
            // fons es queda amb els colors del tema anterior.
            fabrica.invalidarFons();
            Color despres = mostraElFons(fabrica);

            assertThat(luminositat(despres)).isLessThan(luminositat(abans));
        } finally {
            UITheme.posarTheme(original);
        }
    }

    /**
     * La graella de la galeria no ha de redibuixar els fills.
     *
     * <p>Swing ja retalla el pintat pel clip: només pinta els fills que
     * toquen la regió visible. Una implementació pròpia de
     * {@code paintChildren} es salta els estats interns de Swing (memòria
     * cau, banderes de repint) i les targetes queden penjades a la memòria
     * cau sense arribar mai a la pantalla: la galeria només es veia quan el
     * ratolí passava per damunt.
     */
    @Test
    @DisplayName("La graella de la galeria no redefineix paintChildren")
    void laGraellaNoRedibuixaElsFills() throws Exception {
        presentacio.panells.PanelGaleriaCobertes galeria =
            new presentacio.panells.PanelGaleriaCobertes();
        galeria.actualitzarLlibres(List.of(llibreSenseCoberta(), llibreSenseCoberta()));

        Component wrap = contenidorDeLesTargetes(galeria);
        assertThat(wrap).as("no s'ha trobat el contenidor de les targetes").isNotNull();
        assertThat(java.util.Arrays.stream(wrap.getClass().getDeclaredMethods())
            .map(java.lang.reflect.Method::getName))
            .as("el contenidor de les targetes no ha de redefinir paintChildren")
            .doesNotContain("paintChildren");
    }

    /**
     * El contenidor de les targetes: el que les conté directament. Es
     * reconeix perquè tots els seus fills són panells buits (les targetes).
     */
    private static Component contenidorDeLesTargetes(java.awt.Component c) {
        if (c instanceof java.awt.Container cont && cont.getComponentCount() > 0) {
            boolean totesSonTargetes = true;
            for (java.awt.Component ch : cont.getComponents()) {
                if (!(ch instanceof JPanel p) || p.getComponentCount() != 0) {
                    totesSonTargetes = false;
                    break;
                }
            }
            if (totesSonTargetes) return cont;
        }
        if (c instanceof java.awt.Container cont) {
            for (java.awt.Component ch : cont.getComponents()) {
                Component r = contenidorDeLesTargetes(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    /**
     * La icona del marcador de portada ha de ser llegible.
     *
     * <p>El marcador és una superfície apagada amb la icona dibuixada a sobre.
     * Si la icona pren el color apagat del tema, en mode clar queda amb un
     * contrast de 2:1 i no es llegeix.
     */
    @Test
    @DisplayName("La icona del marcador de portada es llegeix en tots els temes")
    void laIconaDelMarcadorEsLlegeix() {
        UITheme.Tema original = UITheme.obtenirTheme();
        try {
            for (UITheme.Tema tema : UITheme.Tema.values()) {
                UITheme.posarTheme(tema);
                ServeiImatgesCoberta servei = new ServeiImatgesCoberta();
                FabricantTargetesCoberta fabrica =
                    new FabricantTargetesCoberta(servei, CARD_W, CARD_H, COVER_H);
                JPanel card = fabrica.build(llibreSenseCoberta(), 0, amfitrioBuit(), "ca");
                card.setSize(card.getPreferredSize());

                BufferedImage img = paintIExtreuImatge(card);
                double r = contrastDelMarcador(img);
                assertThat(r).as("contrast de la icona del marcador en tema " + tema)
                    .isGreaterThanOrEqualTo(3.0);
            }
        } finally {
            UITheme.posarTheme(original);
        }
    }

    /**
     * Contrast entre el fons del marcador i el traç de la icona, dins de la
     * zona de la portada.
     */
    private static double contrastDelMarcador(BufferedImage img) {
        java.util.Map<Integer, Integer> histo = new java.util.HashMap<>();
        for (int y = 0; y < COVER_H / 2; y++) {
            for (int x = 0; x < CARD_W; x++) {
                int p = img.getRGB(x, y);
                if (((p >>> 24) & 0xFF) < 40) continue;
                histo.merge(p & 0xFFFFFF, 1, Integer::sum);
            }
        }
        Integer fons = null;
        int max = -1;
        for (java.util.Map.Entry<Integer, Integer> e : histo.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); fons = e.getKey(); }
        }
        Integer icona = null;
        max = -1;
        for (java.util.Map.Entry<Integer, Integer> e : histo.entrySet()) {
            if (e.getKey().equals(fons)) continue;
            if (e.getValue() > max) { max = e.getValue(); icona = e.getKey(); }
        }
        if (fons == null || icona == null) return 0;
        return contrast(new Color(fons), new Color(icona));
    }

    private static double contrast(Color a, Color b) {
        double l1 = luminanciaLineal(a), l2 = luminanciaLineal(b);
        if (l1 < l2) { double t = l1; l1 = l2; l2 = t; }
        return (l1 + 0.05) / (l2 + 0.05);
    }

    private static double luminanciaLineal(Color c) {
        double r = c.getRed() / 255.0, g = c.getGreen() / 255.0, b = c.getBlue() / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /**
     * Canviar de tema no ha de reconstruir les targetes.
     *
     * <p>Reconstruir-les crea un component nou per llibre i bloqueja el fil
     * d'interfície: amb una col·lecció gran la finestra quedava a mitjes
     * pintar, amb zones del tema antic i zones del tema nou.
     */
    @Test
    @DisplayName("El canvi de tema no reconstrueix les targetes de la galeria")
    void elCanviDeTemaNoReconstrueixLesTargetes() {
        UITheme.Tema original = UITheme.obtenirTheme();
        try {
            presentacio.panells.PanelGaleriaCobertes galeria =
                new presentacio.panells.PanelGaleriaCobertes();
            galeria.actualitzarLlibres(List.of(
                llibreSenseCoberta(),
                llibreSenseCoberta(),
                llibreSenseCoberta()));

            List<Component> abans = targetes(galeria);
            assertThat(abans).hasSize(3);

            galeria.aplicarTheme();

            List<Component> despres = targetes(galeria);
            assertThat(despres).hasSize(3);
            for (int i = 0; i < abans.size(); i++) {
                assertThat(despres.get(i)).isSameAs(abans.get(i));
            }
        } finally {
            UITheme.posarTheme(original);
        }
    }

    /** Les targetes de la galeria són els panells fills sense fills. */
    private static List<Component> targetes(java.awt.Component c) {
        List<Component> out = new ArrayList<>();
        if (c instanceof JPanel p && p.getComponentCount() == 0 && c.getParent() != null) {
            out.add(c);
        }
        if (c instanceof java.awt.Container cont) {
            for (Component ch : cont.getComponents()) out.addAll(targetes(ch));
        }
        return out;
    }

    @Test
    @DisplayName("El peu de la targeta (títol i autor) segueix el tema")
    void elPeuDeLaTargetaSegueixElTema() {
        UITheme.Tema original = UITheme.obtenirTheme();
        try {
            UITheme.posarTheme(UITheme.Tema.DARK);
            ServeiImatgesCoberta servei = new ServeiImatgesCoberta();
            FabricantTargetesCoberta fabrica =
                new FabricantTargetesCoberta(servei, CARD_W, CARD_H, COVER_H);
            JPanel card = fabrica.build(llibreSenseCoberta(), 0, amfitrioBuit(), "ca");
            card.setSize(card.getPreferredSize());

            java.awt.image.BufferedImage fosc = paintIExtreuImatge(card);

            // La mateixa targeta, mateix fons pre-renderitzat: el que canvia
            // és el color del text del peu, que ha de seguir el tema.
            UITheme.posarTheme(UITheme.Tema.LIGHT);
            java.awt.image.BufferedImage clar = paintIExtreuImatge(card);

            assertThat(diferents(fosc, clar)).as(
                "el peu ha de tornar a pintar-se amb els colors del tema nou").isGreaterThan(20);
        } finally {
            UITheme.posarTheme(original);
        }
    }

    /** Nombre de píxels que canvien entre dues imatges. */
    private static int diferents(java.awt.image.BufferedImage a, java.awt.image.BufferedImage b) {
        int n = 0;
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) n++;
            }
        }
        return n;
    }

    /**
     * Pinta la targeta i en retorna el color d'un punt del marcador de
     * portada (fora de la icona central).
     */
    private static Color paint(JPanel card) {
        BufferedImage img = paintIExtreuImatge(card);
        return new Color(img.getRGB(CARD_W / 4, COVER_H / 4), true);
    }

    private static BufferedImage paintIExtreuImatge(JPanel card) {
        BufferedImage img = new BufferedImage(card.getWidth(),
            card.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        card.paint(g);
        g.dispose();
        return img;
    }

    /** Pinta només el fons pre-renderitzat de la targeta. */
    private static Color mostraElFons(FabricantTargetesCoberta fabrica) {
        BufferedImage img = new BufferedImage(CARD_W + 8, CARD_H + 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(fabrica.fons(false), 0, 0, null);
        g.dispose();
        // Punt del fons exterior de la targeta (el cos de la targeta el
        // dibuixa paintCard, no el fons pre-renderitzat).
        return new Color(img.getRGB(2, 2), true);
    }

    private static Llibre llibreSenseCoberta() {
        // Sense imatge: la targeta ha de pintar el marcador de portada.
        return new Llibre(9780306406157L, "Cien anys de solitud", "Gabriel García Márquez",
            1967, "", 9.1, 12.9, false, null);
    }

    private static FabricantTargetesCoberta.AmfitrioTargeta amfitrioBuit() {
        Set<Long> seleccion = new LinkedHashSet<>();
        int[] focus = { -1 };
        List<Llibre> buit = new ArrayList<>();
        return new FabricantTargetesCoberta.AmfitrioTargeta(
            seleccion, focus, () -> buit, l -> { }, (l, ll) -> { }, is -> { }, p -> { });
    }

    private static double luminositat(Color c) {
        return 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
    }
}
