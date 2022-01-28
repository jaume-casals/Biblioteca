package domini;

import persistencia.ControladorPersistencia;

public class ControladorDomini {
    private static ControladorDomini inst;
    private ControladorPersistencia cp;
    private Biblioteca bib;

    public static ControladorDomini getInstance() {
        if (ControladorDomini.inst == null) ControladorDomini.inst = new ControladorDomini();
        return ControladorDomini.inst;
    }

    private ControladorDomini() {
        cp = ControladorPersistencia.getInstance();
        bib = new Biblioteca(cp.getAllLlibres());
    }
}
