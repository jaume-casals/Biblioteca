package presentacio.util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordina les generacions dels resultats asíncrons de la pantalla principal.
 *
 * <p>Cada flux de resultats ({@link #FLUX_CONSULTA}, {@link #FLUX_FILTRE},
 * {@link #FLUX_PAGINA}) té un comptador propi. Obrir una generació incrementa
 * el comptador del flux i invalida els fluxos que aquella consulta substitueix.
 * Una generació és vigent només si el seu comptador encara és el darrer del
 * flux: així cap resultat obsolet pot aplicar-se a l'EDT.
 *
 * <p>La taula i la galeria comparteixen el flux {@link #FLUX_CONSULTA} perquè
 * representen la mateixa consulta: un resultat obsolet de la taula tampoc pot
 * tocar la galeria ni els seus comptadors. Els altres fluxos (filtre i
 * paginació) produeixen la llista visible de la mateixa manera, de manera que
 * substituir-ne un substitueix els altres.
 *
 * <p>La cancel·lació del {@code SwingWorker} és només una optimització: una
 * consulta JDBC no sempre s'interrompre, de manera que la garantia de
 * coherència és la comparació de generacions feta just abans d'aplicar el
 * resultat, no el {@code isCancelled()}.
 *
 * <p>Els paràmetres de la consulta (llista, filtre, pàgina, mode de vista) es
 * captures en la {@link Fulla} abans de llançar la tasca, de manera que el
 * resultat es correspon sempre amb la consulta que el va demanar.
 */
public final class CoordinadorGeneracions {

    /** Consulta completa de la biblioteca (també alimenta la galeria). */
    public static final String FLUX_CONSULTA = "consulta";
    /** Consulta amb filtre aplicat; substitueix la consulta completa. */
    public static final String FLUX_FILTRE = "filtre";
    /** Càrrega d'una pàgina concreta. */
    public static final String FLUX_PAGINA = "paginacio";

    private final Map<String, AtomicLong> comptadors = new ConcurrentHashMap<>();
    private volatile boolean tancat = false;

    /**
     * Obre una generació del flux {@code flux} sense paràmetres capturats.
     *
     * @param flux flux de resultats que s'obre
     * @param fluxosSubstituits fluxos que aquesta consulta substitueix; els seus
     *        resultats pendents queden obsolets
     */
    public Fulla<Void> obrir(String flux, String... fluxosSubstituits) {
        return obrir(flux, null, fluxosSubstituits);
    }

    /**
     * Obre una generació del flux {@code flux} capturant-ne els paràmetres.
     *
     * <p>Els paràmetres es desen al moment de la crida, abans de llançar la
     * tasca, i es conserven immutablement dins la {@link Fulla}.
     *
     * @param flux flux de resultats que s'obre
     * @param parametres paràmetres de la consulta, capturats ara
     * @param fluxosSubstituits fluxos que aquesta consulta substitueix
     */
    public <P> Fulla<P> obrir(String flux, P parametres, String... fluxosSubstituits) {
        Objects.requireNonNull(flux, "flux");
        long generacio = comptador(flux).incrementAndGet();
        for (String substituit : fluxosSubstituits) {
            if (substituit != null) comptador(substituit).incrementAndGet();
        }
        return new Fulla<>(this, flux, generacio, parametres);
    }

    /**
     * Invalida els resultats pendents dels fluxos indicats sense obrir-ne cap
     * de nou. S'usa quan un canvi de context (p. ex. tancar el controlador)
     * invalida el que ja era en vol.
     */
    public void invalidar(String... fluxos) {
        for (String flux : fluxos) {
            if (flux != null) comptador(flux).incrementAndGet();
        }
    }

    /**
     * Tanca el cicle de vida del coordinador: cap generació oberta deixa de
     * ser vigent. S'usa quan el controlador es tanca.
     */
    public void tancar() {
        tancat = true;
    }

    /** Cert si el coordinador ja està tancat. */
    public boolean esTancat() {
        return tancat;
    }

    private AtomicLong comptador(String flux) {
        return comptadors.computeIfAbsent(flux, k -> new AtomicLong());
    }

    boolean esVigent(String flux, long generacio) {
        return !tancat && comptador(flux).get() == generacio;
    }

    /**
     * Fulla d'una generació: identifica el flux, la generació i els
     * paràmetres capturats de la consulta.
     *
     * @param <P> tipus dels paràmetres capturats
     */
    public static final class Fulla<P> {
        private final CoordinadorGeneracions origen;
        private final String flux;
        private final long generacio;
        private final P parametres;

        private Fulla(CoordinadorGeneracions origen, String flux, long generacio, P parametres) {
            this.origen = origen;
            this.flux = flux;
            this.generacio = generacio;
            this.parametres = parametres;
        }

        /** Cert si aquesta generació continua sent la vigent del seu flux. */
        public boolean esVigent() {
            return origen.esVigent(flux, generacio);
        }

        /** Flux de resultats al qual pertany la generació. */
        public String flux() {
            return flux;
        }

        /** Número de generació dins del flux. */
        public long generacio() {
            return generacio;
        }

        /** Paràmetres de la consulta capturats en obrir la generació. */
        public P parametres() {
            return parametres;
        }

        @Override
        public String toString() {
            return "Fulla[" + flux + "#" + generacio + "]";
        }
    }
}
