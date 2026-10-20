package presentacio.renderers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import domini.Llibre;
import presentacio.models.ModelTaulaBiblioteca;
import presentacio.renderers.RenderitzadorBarraProgres;

import javax.swing.JTable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves del renderer de progrés de lectura.
 *
 * <p>Ha de distingir tres casos que abans es confonien: sense dades de
 * pàgines ("—"), 0% real (pàgines conegudes, cap llegida) i progrés
 * parcial.
 */
class TestRenderitzadorProgres {

    private JTable taulaAmb(Llibre llibre) {
        ModelTaulaBiblioteca model = new ModelTaulaBiblioteca();
        model.posarBooks(java.util.List.of(llibre));
        JTable t = new JTable(model);
        t.setSize(400, 60);
        return t;
    }

    @Test
    @DisplayName("Sense pàgines totals vàlides es mostra «—»")
    void senseDades() {
        Llibre l = new Llibre(1L, "T", "A", 2020, null, null, null, false, null);
        JTable t = taulaAmb(l);
        RenderitzadorBarraProgres r = new RenderitzadorBarraProgres();
        r.getTableCellRendererComponent(t, null, false, false, 0, 0);

        assertThat(r.esProgresDesconegut()).isTrue();
        assertThat(r.obtenirTextProgres()).isEqualTo("—");
    }

    @Test
    @DisplayName("Un 0% real (pàgines conegudes, cap llegida) no es confon amb la manca de dades")
    void zeroReal() {
        Llibre l = new Llibre(1L, "T", "A", 2020, null, null, null, false, null);
        l.posarPagines(320);
        l.posarPaginesLlegides(0);
        JTable t = taulaAmb(l);
        RenderitzadorBarraProgres r = new RenderitzadorBarraProgres();
        r.getTableCellRendererComponent(t, null, false, false, 0, 0);

        assertThat(r.esProgresDesconegut()).isFalse();
        assertThat(r.obtenirTextProgres()).isEqualTo("0 / 320");
    }

    @Test
    @DisplayName("El progrés parcial mostra les pàgines llegides sobre les totals")
    void parcial() {
        Llibre l = new Llibre(1L, "T", "A", 2020, null, null, null, false, null);
        l.posarPagines(320);
        l.posarPaginesLlegides(140);
        JTable t = taulaAmb(l);
        RenderitzadorBarraProgres r = new RenderitzadorBarraProgres();
        r.getTableCellRendererComponent(t, null, false, false, 0, 0);

        assertThat(r.esProgresDesconegut()).isFalse();
        assertThat(r.obtenirTextProgres()).isEqualTo("140 / 320");
    }

    @Test
    @DisplayName("Un progrés incoherent (més pàgines llegides que totals) no desborda el text")
    void incoherent() {
        Llibre l = new Llibre(1L, "T", "A", 2020, null, null, null, false, null);
        l.posarPagines(100);
        l.posarPaginesLlegides(500);
        JTable t = taulaAmb(l);
        RenderitzadorBarraProgres r = new RenderitzadorBarraProgres();
        r.getTableCellRendererComponent(t, null, false, false, 0, 0);

        assertThat(r.obtenirTextProgres()).isEqualTo("100 / 100");
    }
}
