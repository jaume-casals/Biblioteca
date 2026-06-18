package presentacio;

import domini.Llibre;
import interficie.EscritorBiblioteca;
import presentacio.listener.EnActualitzarBBDD;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Estat mutable compartit pels sub-controladors de la pantalla principal de la biblioteca. */
final class EstatVistaBiblioteca {
    static final int UNDO_MAX = 20;

    final PanelMostrarBiblioteca vista;
    final EscritorBiblioteca cd;
    final EnActualitzarBBDD enActualizarBBDD;

    java.util.List<Llibre> biblio;
    java.util.List<Llibre> modelLibres;
    Integer currentLlistaId;
    Set<Long> loanedISBNs = new HashSet<>();
    boolean groupBySeries;
    final Deque<Llibre> undoBuffer = new ArrayDeque<>();

    EstatVistaBiblioteca(PanelMostrarBiblioteca vista, java.util.List<Llibre> biblio,
                     EnActualitzarBBDD enActualizarBBDD, EscritorBiblioteca cd) {
        this.vista = vista;
        this.biblio = biblio;
        this.enActualizarBBDD = enActualizarBBDD;
        this.cd = cd;
    }
}
