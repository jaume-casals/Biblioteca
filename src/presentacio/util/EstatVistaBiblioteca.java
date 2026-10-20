package presentacio.util;

import domini.Llibre;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import persistencia.contract.EscritorBiblioteca;
import presentacio.listener.EnActualitzarBBDD;
import presentacio.panells.PanelMostrarBiblioteca;


/** Estat mutable compartit pels sub-controladors de la pantalla principal de la biblioteca. */
public final class EstatVistaBiblioteca {
    public static final int UNDO_MAX = 20;

    public final PanelMostrarBiblioteca vista;
    public final EscritorBiblioteca cd;
    public final EnActualitzarBBDD enActualizarBBDD;

    public java.util.List<Llibre> biblio;
    public java.util.List<Llibre> modelLibres;
    public Integer currentLlistaId;
    public Set<Long> loanedISBNs = new HashSet<>();
    public boolean groupBySeries;
    public final Deque<Llibre> undoBuffer = new ArrayDeque<>();

    /**
     * Recompte de llibres de la vista vigent.
     *
     * <p>El comptador visible el derivaven abans de dues quantitats que
     * no tenien per què coincidir: el nombre de files del model de la taula
     * i la mida de la llista en memòria. Quan un resultat asíncron obsolet
     * tocava una de les dues i no l'altra, el comptador mostrava valors
     * contradictoris (per exemple "1000 / 0 llibres").
     *
     * <p>Aquí el recompte té una única font: el nombre de llibres que
     * coincideixen amb l'àmbit i el filtre vigents ({@code llibresEnAmbit})
     * i el nombre que s'estan renderitzant ara mateix ({@code llibresMostrats}).
     * Tots dos els fixa el mateix camí que aplica el resultat, de manera que
     * no es poden desincronitzar.
     */
    private volatile int llibresEnAmbit;
    private volatile int llibresMostrats;

    /** Llibres que coincideixen amb l'àmbit i el filtre vigents. */
    public int obtenirLlibresEnAmbit() { return llibresEnAmbit; }

    /** Llibres renderitzats ara mateix (files de la taula o targetes de la galeria). */
    public int obtenirLlibresMostrats() { return llibresMostrats; }

    /** fixa tots dos valors de cop: el resultat aplicat defineix l'àmbit i el que es mostra. */
    public void posarComptador(int enAmbit, int mostrats) {
        this.llibresEnAmbit = Math.max(0, enAmbit);
        this.llibresMostrats = Math.max(0, mostrats);
    }

    /**
     * Títol de la vista vigent ("Tots els llibres", el nom d'una
     * prestatgeria, "Afegits recentment"…). El mostra la capçalera.
     */
    private volatile String titolVista = "";

    public String obtenirTitolVista() { return titolVista; }

    public void posarTitolVista(String t) { this.titolVista = t == null ? "" : t; }

    /**
     * Generacions dels resultats asíncrons de la pantalla. La taula i la
     * galeria comparteixen el flux de consulta, de manera que un resultat
     * obsolet de la taula tampoc pot modificar l'estat de la galeria.
     */
    public final CoordinadorGeneracions generacions = new CoordinadorGeneracions();

    public EstatVistaBiblioteca(PanelMostrarBiblioteca vista, java.util.List<Llibre> biblio,
                     EnActualitzarBBDD enActualizarBBDD, EscritorBiblioteca cd) {
        this.vista = vista;
        this.biblio = biblio;
        this.enActualizarBBDD = enActualizarBBDD;
        this.cd = cd;
    }
}