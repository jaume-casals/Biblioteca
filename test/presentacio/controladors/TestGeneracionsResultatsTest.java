package presentacio.controladors;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.text.ValidadorLlibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.contract.EscritorBiblioteca;
import persistencia.internal.ControladorPersistencia;
import presentacio.panells.PanelMostrarBiblioteca;
import presentacio.util.CoordinadorGeneracions;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressió dels resultats asíncrons obsolets.
 *
 * <p>Es comprova el comportament real dels controladors: la taula, la galeria
 * i l'estat compartit només poden reflectir la generació vigent. La
 * sincronització es fa amb latches (mai amb {@code Thread.sleep}): el test
 * obre una porta, el treballador hi queda bloquejat, el test la deixa passar
 * quan vol i espera que el mètode del backend torni abans de comprovar l'EDT.
 */
class TestGeneracionsResultatsTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:generacions_resultats;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    private ControladorMostrarBiblioteca ctrl;
    private BackendControlat backend;

    @BeforeEach
    void setup() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        backend = new BackendControlat(ControladorDomini.getInstance());
        PanelMostrarBiblioteca vista = new PanelMostrarBiblioteca();
        ctrl = new ControladorMostrarBiblioteca(vista, new ArrayList<>(), new EnActualitzarBBDDNoOp(), backend.com());
    }

    @AfterEach
    void tearDown() {
        if (ctrl != null) ctrl.tancar();
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    // ── Escenaris ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A comença abans que B, B acaba primer i A acaba després: només B queda visible")
    void resultatAntigaNoSobreescriuElNou() throws Exception {
        Llibre antic = llibre(9780306406157L, "Dune");
        Llibre nou = llibre(9788467034145L, "Neuva");

        Porta portaA = backend.porta("obtenirAllLlibresSummary", List.of(antic));
        ctrl.refresh();
        assertTrue(portaA.arribat.await(5, TimeUnit.SECONDS), "el treballador A ha d'arribar a la porta");

        Porta portaB = backend.porta("obtenirAllLlibresSummary", List.of(nou));
        ctrl.refresh();
        assertTrue(portaB.arribat.await(5, TimeUnit.SECONDS), "el treballador B ha d'arribar a la porta");

        // B acaba primer i aplica el seu resultat.
        portaB.deixarPassar();
        assertTrue(portaB.retornat.await(5, TimeUnit.SECONDS), "B ha de tornar del backend");
        esperaEdt();
        assertThat(isbnsVisibles()).containsExactly(9788467034145L);

        // A acaba després: el seu resultat ja és obsolet.
        portaA.deixarPassar();
        assertTrue(portaA.retornat.await(5, TimeUnit.SECONDS), "A ha de tornar del backend");
        esperaEdt();

        assertThat(isbnsVisibles()).containsExactly(9788467034145L);
        assertThat(ctrl.state().biblio).extracting(Llibre::obtenirISBN).containsExactly(9788467034145L);
    }

    @Test
    @DisplayName("Una tasca cancel·lada que igualment acaba no aplica resultats")
    void tascaCanceladaQueIgualmentAcabaNoAplica() throws Exception {
        Llibre antic = llibre(9780306406157L, "Dune");
        Llibre nou = llibre(9788467034145L, "Neuva");
        backend.forcaValor("esLargeLibrary", Boolean.TRUE);

        // El filtre cancel·la el treballador anterior, però la consulta pot
        // continuar: el resultat obsolet no s'ha d'aplicar igualment.
        Porta portaA = backend.porta("aplicarFiltres", List.of(antic));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(portaA.arribat.await(5, TimeUnit.SECONDS), "A ha d'arribar a la porta");

        Porta portaB = backend.porta("aplicarFiltres", List.of(nou));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(portaB.arribat.await(5, TimeUnit.SECONDS), "B ha d'arribar a la porta");

        portaB.deixarPassar();
        assertTrue(portaB.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(isbnsVisibles()).containsExactly(9788467034145L);

        // A acaba després de ser cancel·lada: no pot sobreescriure B.
        portaA.deixarPassar();
        assertTrue(portaA.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(isbnsVisibles()).containsExactly(9788467034145L);
    }

    @Test
    @DisplayName("L'error d'una tasca obsoleta no altera l'indicador de càrrega de la vigent")
    void errorDeLaObsoletaNoAlteraLIndicadorDeCarrega() throws Exception {
        backend.forcaValor("esLargeLibrary", Boolean.TRUE);
        // L'indicador de càrrega s'observa amb un comptador d'esdeveniments
        // "enabled": el test comprova l'efecte real, no una instantània.
        java.util.concurrent.atomic.AtomicInteger habilitzacions =
            new java.util.concurrent.atomic.AtomicInteger();
        ctrl.state().vista.obtenirBtnFiltrar().addPropertyChangeListener("enabled", e -> {
            if (Boolean.TRUE.equals(e.getNewValue())) habilitzacions.incrementAndGet();
        });
        Porta portaA = backend.porta("aplicarFiltres", List.of(llibre(9780306406157L, "Dune")));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(portaA.arribat.await(5, TimeUnit.SECONDS));

        Porta portaB = backend.porta("aplicarFiltres", List.of(llibre(9788467034145L, "Neuva")));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(portaB.arribat.await(5, TimeUnit.SECONDS));

        // Mentre B és en vol, el botó de filtrar ha de continuar deshabilitat.
        assertThat(ctrl.state().vista.obtenirBtnFiltrar().isEnabled())
            .as("el botó de filtrar ha de romandre deshabilitat mentre B és en vol")
            .isFalse();

        // A falla després de ser obsoleta: no ha de tocar l'indicador.
        backend.forcaError("aplicarFiltres", new IllegalStateException("fallada simulada"), portaA);
        portaA.deixarPassar();
        assertTrue(portaA.retornat.await(5, TimeUnit.SECONDS));
        esperaEstable(habilitzacions::get);
        assertThat(habilitzacions.get())
            .as("l'error obsolet no ha de re-habilitar el botó de filtrar")
            .isZero();
        assertThat(ctrl.state().vista.obtenirBtnFiltrar().isEnabled())
            .as("el botó ha de continuar deshabilitat mentre B és en vol")
            .isFalse();

        // B acaba bé: aleshores el botó torna a estar disponible.
        portaB.deixarPassar();
        assertTrue(portaB.retornat.await(5, TimeUnit.SECONDS));
        esperaFins(() -> ctrl.state().vista.obtenirBtnFiltrar().isEnabled(),
            "el botó de filtrar habilitat per la generació vigent");
        assertThat(habilitzacions.get())
            .as("només la generació vigent pot habilitar el botó")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Canvis ràpids de filtre: només l'última generació aplica el resultat")
    void canvisRapidsDeFiltre() throws Exception {
        Llibre a = llibre(9780306406157L, "A");
        Llibre b = llibre(9788467034145L, "B");
        Llibre c = llibre(9788494928926L, "C");
        backend.forcaValor("esLargeLibrary", Boolean.TRUE);

        Porta pa = backend.porta("aplicarFiltres", List.of(a));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(pa.arribat.await(5, TimeUnit.SECONDS));

        Porta pb = backend.porta("aplicarFiltres", List.of(b));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(pb.arribat.await(5, TimeUnit.SECONDS));

        Porta pc = backend.porta("aplicarFiltres", List.of(c));
        ctrl.filtrarCtrl().filtrar();
        assertTrue(pc.arribat.await(5, TimeUnit.SECONDS));

        pc.deixarPassar();
        assertTrue(pc.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(isbnsVisibles()).containsExactly(9788494928926L);

        // Les dues generacions antigues arriveixen després: no apliquen res.
        pa.deixarPassar();
        pb.deixarPassar();
        assertTrue(pa.retornat.await(5, TimeUnit.SECONDS));
        assertTrue(pb.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(isbnsVisibles()).containsExactly(9788494928926L);
    }

    @Test
    @DisplayName("Tancar el controlador invalida les generacions pendents")
    void tancarElControladorInvalidaLesGeneracions() throws Exception {
        Porta porta = backend.porta("obtenirAllLlibresSummary", List.of(llibre(9780306406157L, "Dune")));
        ctrl.refresh();
        assertTrue(porta.arribat.await(5, TimeUnit.SECONDS));

        ctrl.tancar();
        porta.deixarPassar();
        assertTrue(porta.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();

        assertThat(isbnsVisibles()).isEmpty();
        assertThat(ctrl.state().biblio).isEmpty();
    }

    @Test
    @DisplayName("La galeria comparteix la generació de la taula: un resultat obsolet no la toca")
    void laGaleriaComparteixLaGeneracioDeLaTaula() throws Exception {
        ctrl.state().vista.mostrarGaleria();
        assertThat(ctrl.state().vista.esGaleriaMode()).as("mode galeria actiu").isTrue();
        Llibre antic = llibre(9780306406157L, "Dune");
        Llibre nou = llibre(9788467034145L, "Neuva");

        Porta portaA = backend.porta("obtenirAllLlibresSummary", List.of(antic));
        ctrl.refresh();
        assertTrue(portaA.arribat.await(5, TimeUnit.SECONDS));

        Porta portaB = backend.porta("obtenirAllLlibresSummary", List.of(nou));
        ctrl.refresh();
        assertTrue(portaB.arribat.await(5, TimeUnit.SECONDS));

        portaB.deixarPassar();
        assertTrue(portaB.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(ctrl.state().vista.obtenirGaleria().obtenirLlibresVisibles())
            .extracting(Llibre::obtenirISBN).containsExactly(9788467034145L);

        portaA.deixarPassar();
        assertTrue(portaA.retornat.await(5, TimeUnit.SECONDS));
        esperaEdt();
        assertThat(ctrl.state().vista.obtenirGaleria().obtenirLlibresVisibles())
            .as("la galeria no ha d acceptar un resultat obsolet")
            .extracting(Llibre::obtenirISBN).containsExactly(9788467034145L);
    }

    @Test
    @DisplayName("El coordinador de generacions invalida els fluxos substituïts")
    void elCoordinadorInvalidaElsFluxosSubstituits() {
        CoordinadorGeneracions gen = new CoordinadorGeneracions();
        var consulta = gen.obrir(CoordinadorGeneracions.FLUX_CONSULTA, (Integer) null);
        var filtre = gen.obrir(CoordinadorGeneracions.FLUX_FILTRE, (Void) null,
            CoordinadorGeneracions.FLUX_CONSULTA);
        assertThat(consulta.esVigent()).as("la consulta queda obsoleta quan s'obre un filtre").isFalse();
        assertThat(filtre.esVigent()).isTrue();

        var pagina = gen.obrir(CoordinadorGeneracions.FLUX_PAGINA, 3);
        assertThat(pagina.parametres()).isEqualTo(3);
        assertThat(filtre.esVigent()).as("una pàgina no invalida un filtre").isTrue();

        gen.tancar();
        assertThat(pagina.esVigent()).as("tancar el cicle de vida invalida tot").isFalse();
        assertThat(filtre.esVigent()).isFalse();
    }

    // ── Utilitats ────────────────────────────────────────────────────────────

    /** ISBNs visibles a la taula, llegits del model (no de la vista). */
    private List<Object> isbnsVisibles() {
        List<Object> out = new ArrayList<>();
        var t = ctrl.state().vista.obtenirTaulaLlibres();
        var model = t.getModel();
        for (int i = 0; i < t.getRowCount(); i++) {
            Object v = model.getValueAt(t.convertRowIndexToModel(i), 1);
            if (v instanceof Number) out.add(((Number) v).longValue());
            else if (v != null) { try { out.add(Long.parseLong(v.toString().trim())); } catch (NumberFormatException e) { out.add(v); } }
        }
        return out;
    }

    private static Llibre llibre(long isbn, String nom) {
        return ValidadorLlibre.comprovarLlibre(isbn, nom, "Autor", 2000, "", 0.0, 0.0, false, "");
    }

    /**
     * Espera que l'EDT processi tot el que ja hi ha en cua.
     *
     * <p>La sincronització real la fan les {@link Porta}: aquesta espera només
     * dona temps al SwingWorker perquè enfili el seu {@code done()} a l'EDT
     * abans que el test torni a mirar l'estat.
     */
    private static void esperaEdt() throws Exception {
        for (int i = 0; i < 8; i++) {
            SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(5);
        }
        SwingUtilities.invokeAndWait(() -> { });
    }

    /**
     * Espera que un valor observable es quedi estable: dues lectures
     * consecutives, separated per una barrera d'EDT, han de coincidir.
     *
     * <p>Una lectura sola no és suficient: el {@code done()} d'un SwingWorker
     * s'enfila a l'EDT just després que la tasca retorna del backend, de
     * manera que una lectura immediata pot avançar-se'l. La estabilitat
     * evita que el test depengui d'aquesta finestra.
     */
    private static void esperaEstable(java.util.function.LongSupplier valor) throws Exception {
        long fi = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        long anterior = valor.getAsLong();
        while (System.nanoTime() < fi) {
            SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(10);
            long actual = valor.getAsLong();
            if (actual == anterior) return;
            anterior = actual;
        }
        assertThat(valor.getAsLong())
            .as("el valor observable no s'ha estabilitzat")
            .isEqualTo(anterior);
    }

    /** Espera que es compleixi una condicio observable (amb timeout). */
    private static void esperaFins(java.util.function.BooleanSupplier cond, String desc) throws Exception {
        long fi = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < fi) {
            if (cond.getAsBoolean()) return;
            SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(5);
        }
        assertTrue(cond.getAsBoolean(), "temps esgotat esperant: " + desc);
    }

    /** Porta de sincronització per a una crida al backend. */
    private static final class Porta {
        final String metode;
        final CountDownLatch arribat = new CountDownLatch(1);
        final CountDownLatch oberta = new CountDownLatch(1);
        final CountDownLatch retornat = new CountDownLatch(1);
        final Object resultat;
        volatile RuntimeException error;

        Porta(String metode, Object resultat) {
            this.metode = metode;
            this.resultat = resultat;
        }

        void deixarPassar() { oberta.countDown(); }
    }

    /**
     * Backend de proves: delega al {@link ControladorDomini} real, però permet
     * endrapar mètodes concrets amb una {@link Porta}, injectar-ne el resultat
     * i forçar-ne el valor de retorn.
     */
    private static final class BackendControlat implements InvocationHandler {
        private final EscritorBiblioteca real;
        private final List<Porta> portes = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, Object> valorsForcats = new ConcurrentHashMap<>();

        BackendControlat(EscritorBiblioteca real) { this.real = real; }

        /** Contracte de persistència que delega al backend real però permet endrapar mètodes. */
        EscritorBiblioteca com() {
            return (EscritorBiblioteca) Proxy.newProxyInstance(
                EscritorBiblioteca.class.getClassLoader(),
                new Class<?>[]{EscritorBiblioteca.class},
                this);
        }

        Porta porta(String metode, Object resultat) {
            Porta p = new Porta(metode, resultat);
            portes.add(p);
            return p;
        }

        void forcaValor(String metode, Object valor) { valorsForcats.put(metode, valor); }

        void forcaError(String metode, RuntimeException e, Porta porta) { porta.error = e; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String nom = method.getName();
            if (valorsForcats.containsKey(nom)) return valorsForcats.get(nom);

            Porta porta = null;
            synchronized (portes) {
                for (Porta p : portes) {
                    if (p.metode.equals(nom)) { porta = p; portes.remove(p); break; }
                }
            }
            if (porta == null) return method.invoke(real, args);

            porta.arribat.countDown();
            // Espera no interruptible: un treballador cancel·lat que continua
            // la consulta ha de poder tornar igualment.
            boolean oberta = false;
            while (!oberta) {
                try { oberta = porta.oberta.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException ignored) { }
            }
            try {
                if (porta.error != null) throw porta.error;
                return porta.resultat;
            } finally {
                porta.retornat.countDown();
                // Drena l'EDT: el done() del SwingWorker s'enfila just
                // després que doInBackground torni, de manera que el test
                // ja el trobarà aplicat quan torni a mirar l'estat.
                CountDownLatch drenat = new CountDownLatch(1);
                SwingUtilities.invokeLater(drenat::countDown);
                drenat.await(5, TimeUnit.SECONDS);
            }
        }
    }

    /** EnActualitzarBBDE buit: el test no vols events de persistència. */
    private static final class EnActualitzarBBDDNoOp implements presentacio.listener.EnActualitzarBBDD {
        @Override public void enActualitzarLlibre(Llibre l, boolean esNew) { }
        @Override public void enEliminarLlibre(Llibre l) { }
    }
}
