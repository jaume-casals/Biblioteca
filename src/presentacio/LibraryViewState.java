package presentacio;

import domini.Llibre;
import interficie.BibliotecaWriter;
import presentacio.listener.EnActualizarBBDD;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Shared mutable state for the main library screen sub-controllers. */
final class LibraryViewState {
    static final int UNDO_MAX = 20;

    final PanelMostrarBiblioteca vista;
    final BibliotecaWriter cd;
    final EnActualizarBBDD enActualizarBBDD;

    java.util.List<Llibre> biblio;
    java.util.List<Llibre> modelLibres;
    Integer currentLlistaId;
    Set<Long> loanedISBNs = new HashSet<>();
    boolean groupBySeries;
    final Deque<Llibre> undoBuffer = new ArrayDeque<>();

    LibraryViewState(PanelMostrarBiblioteca vista, java.util.List<Llibre> biblio,
                     EnActualizarBBDD enActualizarBBDD, BibliotecaWriter cd) {
        this.vista = vista;
        this.biblio = biblio;
        this.enActualizarBBDD = enActualizarBBDD;
        this.cd = cd;
    }
}
