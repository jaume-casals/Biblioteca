package presentacio.util;

import domini.Llibre;
import java.util.ArrayDeque;
import java.util.ArrayList;
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

    public EstatVistaBiblioteca(PanelMostrarBiblioteca vista, java.util.List<Llibre> biblio,
                     EnActualitzarBBDD enActualizarBBDD, EscritorBiblioteca cd) {
        this.vista = vista;
        this.biblio = biblio;
        this.enActualizarBBDD = enActualizarBBDD;
        this.cd = cd;
    }
}