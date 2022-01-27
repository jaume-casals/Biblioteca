package domini;

import persistencia.ControladorPersistencia;

public class ControladorDomini {
    private ControladorPersistencia cp;
    private static ControladorDomini inst;

    public static ControladorDomini getInstance() {
        if (ControladorDomini.inst == null) ControladorDomini.inst = new ControladorDomini();
        return ControladorDomini.inst;
    }

    private ControladorDomini() {
        cp = ControladorPersistencia.getInstance();
    }
}
