package persistencia;

import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.Llista;
import domini.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contracte de persistència compartit per tots els motors.
 *
 * <p>Les afirmacions d'aquesta classe són <b>agnòstiques del motor</b>: la
 * classe base no sap si la connecta H2 o MariaDB. El que canvia entre motors
 * està aïllat a la costura {@code protected abstract} de la classe base i
 * <b>no s'amaga</b>: cada diferència real del motor té una afirmació
 * explícita que la documenta (límit de BLOB, col·lació del LIKE, tipus de
 * retorn numèric).
 *
 * <p>Regla d'or del contracte: <b>no s'assumeix portabilitat SQL entre
 * motors</b>. Si un comportament divergeix, es declara explícitament en el
 * mètode corresponent enlloc d'esborrar la divergència.
 *
 * <p>Les subclasses són {@code ContractPersistenciaH2Test} (H2 en memòria,
 * dins de {@code make test}) i {@code ContractPersistenciaMariaDBTest}
 * (MariaDB real, fora de {@code make test}).
 */
abstract class ContractPersistenciaBase {

    /** Versió d'esquema més recent aplicada per {@code ConnexioServidor}. */
    protected static final int ULTIMA_VERSIO_ESGEMA = 39;

    protected ControladorPersistencia cp;
    protected ControladorDomini domini;

    // ── Costura del backend ──────────────────────────────────────────────────

    /**
     * Obre (o reobre) la connexió de persistència del motor sota prova.
     * La implementació de MariaDB ha de cridar
     * {@code Assumptions.assumeTrue(...)} si no hi ha servidor configurat,
     * de manera que la classe quedi saltada i no arribi a tocar cap base de
     * dades.
     */
    protected abstract ControladorPersistencia obrirControlador() throws Exception;

    /** Nom llegible del motor, per als missatges d'error. */
    protected abstract String nomEngine();

    /**
     * {@code true} si el {@code LIKE} del motor no distingeix majúscules de
     * minúscules. H2 és sensible a la caixa; MariaDB usa una col·lació
     * {@code _ci} per defecte i no ho és.
     */
    protected abstract boolean cercaIgnoraMajuscules();

    /**
     * Mida màxima en bytes de la coberta ({@code imatge_blob BLOB}) que el
     * motor accepta, o {@code null} si el motor no hi posa límit.
     * MariaDB limita el tipus {@code BLOB} a 65535 bytes; H2 no hi posa cap.
     */
    protected abstract Integer limitCobertaBytes();

    // ── Preparació ───────────────────────────────────────────────────────────

    @BeforeEach
    void obrirContracte() throws Exception {
        cp = obrirControlador();
        domini = ControladorDomini.create(cp);
        netejar();
    }

    @AfterEach
    void tancarContracte() {
        try {
            if (cp != null) cp.netejarAllData();
        } catch (Exception ignored) {
            // La neteja del @AfterEach és best-effort: si el test ja ha fallat,
            // l'error original és el que interessa.
        }
    }

    protected void netejar() throws SQLException {
        cp.netejarAllData();
    }

    // ── Utilitats ────────────────────────────────────────────────────────────

    protected static Llibre llibre(long isbn, String nom) {
        return Llibre.builder()
            .isbn(isbn)
            .nom(nom)
            .autor("Autor Contracte")
            .any(2001)
            .build();
    }

    protected long compte(String sql) throws SQLException {
        try (Statement st = cp.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    protected String text(String sql) throws SQLException {
        try (Statement st = cp.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    protected boolean taulaExisteix(String taula) {
        try (Statement st = cp.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + taula)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /** Llegeix un llibre del motor amb els camps lleugers (sense descripció ni notes). */
    protected Llibre llegirLleuger(long isbn) {
        for (Llibre l : cp.obtenirAllLlibres()) {
            if (l.obtenirISBN() != null && l.obtenirISBN() == isbn) return l;
        }
        throw new AssertionError("No s'ha trobat el llibre " + isbn + " al motor " + nomEngine());
    }

    /** Llegeix un llibre amb els camps pesats carregats des de la BBDD. */
    protected Llibre llegirComplet(long isbn) {
        Llibre l = llegirLleuger(isbn);
        cp.carregarHeavyFields(isbn, l);
        return l;
    }

    // ── Esquema i migracions ─────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: l'esquema es crea amb totes les migracions aplicades")
    void esquemaIMigracionsAplicades() throws SQLException {
        List<String> taules = List.of("llibre", "llista", "llibre_llista", "tag", "llibre_tag",
            "autor", "llibre_autor", "prestec", "lectura", "schema_version");
        for (String t : taules) {
            assertThat(taulaExisteix(t))
                .as("la taula %s ha d'existir al motor %s", t, nomEngine())
                .isTrue();
        }

        // schema_version ha de contenir totes les migracions, de la 1 a l'última.
        List<Integer> versions = new ArrayList<>();
        try (Statement st = cp.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_version ORDER BY version")) {
            while (rs.next()) versions.add(rs.getInt(1));
        }
        assertThat(versions)
            .as("schema_version ha de contenir les migracions 1..%d al motor %s", ULTIMA_VERSIO_ESGEMA, nomEngine())
            .containsExactlyElementsOf(
                java.util.stream.IntStream.rangeClosed(1, ULTIMA_VERSIO_ESGEMA).boxed().toList());
    }

    @Test
    @DisplayName("contracte: la creació de l'esquema és idempotent")
    void esquemaEsIdempotent() throws Exception {
        // Reobrir el controlador torna a aplicar les migracions: no hi ha de
        // poder cap error ni cap versió duplicada.
        ControladorPersistencia altre = obrirControlador();
        try (Statement st = altre.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schema_version")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(ULTIMA_VERSIO_ESGEMA);
        }
    }

    // ── CRUD de llibres ──────────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: crea, llegeix, actualitza i esborra un llibre")
    void crudDeLlibre() throws SQLException {
        Llibre l = Llibre.builder()
            .isbn(1001L).nom("El nom del llibre").autor("Autor Contracte")
            .any(1999).descripcio("Una descripció").valoracio(4.5).preu(12.75)
            .llegit(Boolean.TRUE).imatge("http://exemple/imatge.jpg")
            .build();
        domini.afegirLlibre(l);

        assertThat(cp.comptarLlibres()).isEqualTo(1);
        Llibre llegit = llegirComplet(1001L);
        assertThat(llegit.obtenirNom()).isEqualTo("El nom del llibre");
        assertThat(llegit.obtenirAny()).isEqualTo(1999);
        assertThat(llegit.obtenirDescripcio()).isEqualTo("Una descripció");
        assertThat(llegit.obtenirValoracio()).isEqualTo(4.5);
        assertThat(llegit.obtenirPreu()).isEqualTo(12.75);
        assertThat(llegit.obtenirLlegit()).isTrue();
        assertThat(llegit.obtenirImatge()).isEqualTo("http://exemple/imatge.jpg");
        assertThat(llegit.obtenirAutors()).containsExactly("Autor Contracte");

        // Actualització
        llegit.posarNom("El nom corregit");
        llegit.posarAny(2005);
        domini.actualitzarLlibre(llegit);
        Llibre actualitzat = llegirComplet(1001L);
        assertThat(actualitzat.obtenirNom()).isEqualTo("El nom corregit");
        assertThat(actualitzat.obtenirAny()).isEqualTo(2005);
        assertThat(actualitzat.obtenirDescripcio()).isEqualTo("Una descripció");

        // Esborrat
        domini.eliminarLlibre(1001L);
        assertThat(cp.comptarLlibres()).isZero();
    }

    @Test
    @DisplayName("contracte: un ISBN duplicat es rebutja")
    void isbnDuplicatEsRebutja() {
        domini.afegirLlibre(llibre(1002L, "Primer"));
        assertThatThrownBy(() -> domini.afegirLlibre(llibre(1002L, "Segon")))
            .isInstanceOf(domini.BibliotecaException.class);
        assertThat(cp.comptarLlibres()).isEqualTo(1);
    }

    // ── Relacions ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: els autors es desenen a la taula llibre_autor")
    void relacioDautors() throws SQLException {
        Llibre l = llibre(2001L, "Llibre amb dos autors");
        l.posarAutors(List.of("Autor Alfa", "Autor Beta"));
        domini.afegirLlibre(l);

        assertThat(llegirComplet(2001L).obtenirAutors())
            .containsExactlyInAnyOrder("Autor Alfa", "Autor Beta");
        assertThat(compte("SELECT COUNT(*) FROM autor")).isEqualTo(2);
        assertThat(compte("SELECT COUNT(*) FROM llibre_autor WHERE isbn = 2001")).isEqualTo(2);
    }

    @Test
    @DisplayName("contracte: les etiquetes es desenen a la taula llibre_tag")
    void relacioDetiquetes() throws SQLException {
        domini.afegirLlibre(llibre(2002L, "Llibre amb etiqueta"));
        Tag tag = domini.afegirTag("Ciència");
        domini.afegirLlibreToTag(2002L, tag.obtenirId());

        assertThat(domini.obtenirTagsForLlibre(2002L)).extracting(Tag::obtenirNom).containsExactly("Ciència");
        assertThat(compte("SELECT COUNT(*) FROM llibre_tag WHERE isbn = 2002")).isEqualTo(1);
        assertThat(domini.obtenirLlibresWithTag(tag.obtenirId())).containsExactly(2002L);
    }

    @Test
    @DisplayName("contracte: les llistes de lectura es desenen a la taula llibre_llista")
    void relacioDeLlistes() throws SQLException {
        domini.afegirLlibre(llibre(2003L, "Llibre en llista"));
        Llista llista = domini.afegirLlista("Preferits");
        domini.afegirLlibreToLlista(2003L, llista.obtenirId(), 4.0, true);

        assertThat(domini.obtenirLlistesForLlibre(2003L)).extracting(Llista::obtenirNom).containsExactly("Preferits");
        assertThat(compte("SELECT COUNT(*) FROM llibre_llista WHERE isbn = 2003")).isEqualTo(1);
        assertThat(domini.obtenirCountInLlista(llista.obtenirId())).isEqualTo(1);
        assertThat(domini.obtenirLlibresInLlista(llista.obtenirId())).extracting(Llibre::obtenirISBN).containsExactly(2003L);
    }

    // ── Filtres i paginació ──────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: els filtres per nom, autor i any filtren a SQL")
    void filtres() {
        domini.afegirLlibre(llibre(3001L, "Cervesa de Barcelona"));
        Llibre b = llibre(3002L, "Vins del Priorat");
        b.posarAutors(List.of("Celler Castell"));
        domini.afegirLlibre(b);
        Llibre c = llibre(3003L, "Formatge de Lactància");
        c.posarAny(2010);
        domini.afegirLlibre(c);

        List<Llibre> perNom = domini.cercarLlibresSQL(new LlibreFilter().withNom("Cervesa"));
        assertThat(perNom).extracting(Llibre::obtenirISBN).containsExactly(3001L);

        List<Llibre> perAutor = domini.cercarLlibresSQL(new LlibreFilter().withAutor("Celler Castell"));
        assertThat(perAutor).extracting(Llibre::obtenirISBN).containsExactly(3002L);

        List<Llibre> perAny = domini.cercarLlibresSQL(new LlibreFilter().withAnyMin(2005));
        assertThat(perAny).extracting(Llibre::obtenirISBN).containsExactly(3003L);
    }

    @Test
    @DisplayName("contracte: la cerca ignora la caixa segons el motor (diferència declarada)")
    void cercaSegonsLaCollacioDelMotor() {
        domini.afegirLlibre(llibre(3101L, "Cervesa"));

        List<Llibre> minuscules = domini.cercarLlibresSQL(new LlibreFilter().withNom("cervesa"));
        if (cercaIgnoraMajuscules()) {
            // MariaDB: col·lació *_ci per defecte → el LIKE no distingeix la caixa.
            assertThat(minuscules).extracting(Llibre::obtenirISBN).containsExactly(3101L);
        } else {
            // H2: el LIKE és sensible a la caixa.
            assertThat(minuscules).isEmpty();
        }

        // El resultat ha de ser el mateix passi el text amb la caixa exacta.
        List<Llibre> exactes = domini.cercarLlibresSQL(new LlibreFilter().withNom("Cervesa"));
        assertThat(exactes).extracting(Llibre::obtenirISBN).containsExactly(3101L);
    }

    @Test
    @DisplayName("contracte: la paginació retorna la finestra demanada")
    void paginacio() {
        for (int i = 1; i <= 5; i++) domini.afegirLlibre(llibre(4000L + i, "Llibre " + i));

        assertThat(domini.comptarLlibresDB()).isEqualTo(5);
        assertThat(domini.obtenirLlibresPage(0, 2)).extracting(Llibre::obtenirISBN)
            .containsExactly(4001L, 4002L);
        assertThat(domini.obtenirLlibresPage(2, 2)).extracting(Llibre::obtenirISBN)
            .containsExactly(4003L, 4004L);
        assertThat(domini.obtenirLlibresPage(4, 2)).extracting(Llibre::obtenirISBN)
            .containsExactly(4005L);
        assertThat(domini.obtenirLlibresPage(10, 2)).isEmpty();
    }

    // ── Unicode, dates i nuls ────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: el text Unicode (accents, CJK, emoji) es desa i es llegeix igual")
    void unicode() throws SQLException {
        String nom = "Cançó d'hivern 雪 ❄️ 日本語";
        String descripcio = "És una lletra — amb «cometes», emoji 📚 i CJK 龍";
        Llibre l = Llibre.builder().isbn(5001L).nom(nom).autor("Autor Ünicode")
            .descripcio(descripcio).build();
        domini.afegirLlibre(l);

        Llibre llegit = llegirComplet(5001L);
        assertThat(llegit.obtenirNom()).isEqualTo(nom);
        assertThat(llegit.obtenirDescripcio()).isEqualTo(descripcio);
        assertThat(llegit.obtenirAutors()).containsExactly("Autor Ünicode");
    }

    @Test
    @DisplayName("contracte: les dates es desen i es llegeixen en format ISO")
    void dates() throws SQLException {
        Llibre l = llibre(5002L, "Llibre amb dates");
        l.posarDataCompra("2024-02-29");
        l.posarDataLectura("2023-12-31");
        domini.afegirLlibre(l);

        Llibre llegit = llegirComplet(5002L);
        assertThat(llegit.obtenirDataCompra()).isEqualTo("2024-02-29");
        assertThat(llegit.obtenirDataLectura()).isEqualTo("2023-12-31");
        assertThat(text("SELECT data_compra FROM llibre WHERE ISBN = 5002")).isEqualTo("2024-02-29");
    }

    @Test
    @DisplayName("contracte: els valors nuls es desen com a NULL")
    void valorsNuls() throws SQLException {
        Llibre l = llibre(5003L, "Llibre amb nuls");
        l.posarIdioma(null);
        l.posarFormat(null);
        l.posarPaisOrigen(null);
        l.posarEstat(null);
        l.posarLlenguaOriginal(null);
        l.posarDataCompra(null);
        l.posarDataLectura(null);
        domini.afegirLlibre(l);

        Llibre llegit = llegirComplet(5003L);
        assertThat(llegit.obtenirIdioma()).isNull();
        assertThat(llegit.obtenirFormat()).isNull();
        assertThat(llegit.obtenirPaisOrigen()).isNull();
        assertThat(llegit.obtenirEstat()).isNull();
        assertThat(llegit.obtenirLlenguaOriginal()).isNull();
        assertThat(llegit.obtenirDataCompra()).isNull();
        assertThat(llegit.obtenirDataLectura()).isNull();
        assertThat(compte("SELECT COUNT(*) FROM llibre WHERE idioma IS NULL AND data_compra IS NULL")).isEqualTo(1);
    }

    // ── Camps pesats ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: els camps pesats carregats, buits i esborrats es distingeixen")
    void campsPesats() throws SQLException {
        Llibre l = Llibre.builder().isbn(6001L).nom("Llibre amb camps pesats")
            .descripcio("Descripció original").build();
        l.posarNotes("Notes originals");
        domini.afegirLlibre(l);

        // (1) Carregats: el valor arriba intacte.
        Llibre carregat = llegirComplet(6001L);
        assertThat(carregat.teCampsPesatsCarregats()).isTrue();
        assertThat(carregat.obtenirDescripcio()).isEqualTo("Descripció original");
        assertThat(carregat.obtenirNotes()).isEqualTo("Notes originals");

        // (2) No carregats: un UPDATE no ha d'esborrar el que hi ha a la BBDD.
        Llibre lleuger = llegirLleuger(6001L);
        assertThat(lleuger.teCampsPesatsCarregats()).isFalse();
        lleuger.posarNom("Llibre renomenat sense tocar els camps pesats");
        domini.actualitzarLlibre(lleuger);
        Llibre després = llegirComplet(6001L);
        assertThat(després.obtenirNom()).isEqualTo("Llibre renomenat sense tocar els camps pesats");
        assertThat(després.obtenirDescripcio()).isEqualTo("Descripció original");
        assertThat(després.obtenirNotes()).isEqualTo("Notes originals");

        // (3) Esborrats explícitament: el camp es buida, no es queda el valor vell.
        després.posarDescripcio("");
        després.posarNotes("");
        domini.actualitzarLlibre(després);
        Llibre buidat = llegirComplet(6001L);
        assertThat(buidat.obtenirDescripcio()).isEmpty();
        assertThat(buidat.obtenirNotes()).isEmpty();
    }

    // ── Coberta (BLOB) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: la coberta (BLOB) es desa i es llegeix byte a byte")
    void cobertaBlob() throws SQLException {
        domini.afegirLlibre(llibre(7001L, "Llibre amb coberta"));
        byte[] portada = new byte[4096];
        for (int i = 0; i < portada.length; i++) portada[i] = (byte) (i % 256);
        cp.posarLlibreBlob(7001L, portada);

        byte[] llegida = cp.obtenirLlibreBlob(7001L);
        assertThat(llegida).isNotNull();
        assertThat(llegida).containsExactly(portada);
    }

    @Test
    @DisplayName("contracte: el límit de mida de la coberta és el del motor (diferència declarada)")
    void limitDeMidaDeLaCoberta() throws SQLException {
        domini.afegirLlibre(llibre(7002L, "Llibre amb coberta gran"));
        Integer limit = limitCobertaBytes();

        if (limit == null) {
            // H2: el tipus BLOB no hi posa límit de mida.
            byte[] gran = new byte[100_000];
            cp.posarLlibreBlob(7002L, gran);
            assertThat(cp.obtenirLlibreBlob(7002L)).containsExactly(gran);
        } else {
            // MariaDB: el tipus BLOB es limita a 65535 bytes. Una coberta
            // més gran no s'ha de poder desar — l'error ha de ser explícit,
            // no una truncament silenciós.
            byte[] massaGran = new byte[limit + 1];
            assertThatThrownBy(() -> cp.posarLlibreBlob(7002L, massaGran))
                .isInstanceOf(SQLException.class);
            // I una coberta dins del límit sí que s'ha de desar.
            byte[] dinsDelLimit = new byte[limit];
            cp.posarLlibreBlob(7002L, dinsDelLimit);
            assertThat(cp.obtenirLlibreBlob(7002L)).containsExactly(dinsDelLimit);
        }
    }

    // ── Còpia de seguretat ───────────────────────────────────────────────────

    @Test
    @DisplayName("contracte: la còpia de seguretat es restaura després d'esborrar-ho tot")
    void copiaSeguretatEsRestaura() throws Exception {
        Llibre l1 = llibre(8001L, "Llibre de la còpia");
        l1.posarAutors(List.of("Autor Original"));
        l1.posarDescripcio("Descripció de la còpia");
        domini.afegirLlibre(l1);
        domini.afegirLlibre(llibre(8002L, "Segon llibre de la còpia"));
        Tag tag = domini.afegirTag("Etiqueta de la còpia");
        domini.afegirLlibreToTag(8001L, tag.obtenirId());
        Llista llista = domini.afegirLlista("Llista de la còpia");
        domini.afegirLlibreToLlista(8001L, llista.obtenirId(), 3.5, true);

        File dump = dumpTemporal();
        domini.copiaSegToSQL(dump);
        assertThat(dump.length()).isPositive();

        // Esborra-ho tot i comprova que la BBDD és buida.
        cp.netejarAllData();
        assertThat(cp.comptarLlibres()).isZero();
        assertThat(compte("SELECT COUNT(*) FROM llibre_autor")).isZero();

        // Restaura.
        cp.restaurarFromSQLFile(dump);

        assertThat(cp.comptarLlibres()).isEqualTo(2);
        Llibre restaurat = llegirComplet(8001L);
        assertThat(restaurat.obtenirNom()).isEqualTo("Llibre de la còpia");
        assertThat(restaurat.obtenirDescripcio()).isEqualTo("Descripció de la còpia");
        assertThat(restaurat.obtenirAutors()).containsExactly("Autor Original");
        assertThat(compte("SELECT COUNT(*) FROM llibre_tag WHERE isbn = 8001")).isEqualTo(1);
        assertThat(compte("SELECT COUNT(*) FROM llibre_llista WHERE isbn = 8001")).isEqualTo(1);
        assertThat(compte("SELECT COUNT(*) FROM llista")).isEqualTo(1);
        assertThat(compte("SELECT COUNT(*) FROM tag")).isEqualTo(1);
    }

    @Test
    @DisplayName("contracte: la còpia de seguretat conserva el text amb barres invertides")
    void copiaSeguretatConservaBarresInvertides() throws Exception {
        // La barra invertida és el punt on H2 i MySQL/MariaDB divergeixen:
        // MySQL la tracta com a caràcter d'escapada dins les literals, H2 no.
        // El format del volcat és SQL estàndard i la connexió MariaDB activa
        // NO_BACKSLASH_ESCAPES perquè tots dos els motors el llegeixin igual.
        String descripcio = "Ruta Windows C:\\rutes\\biblioteca i una barra solta \\";
        Llibre l = Llibre.builder().isbn(8101L).nom("Llibre amb barres").descripcio(descripcio).build();
        domini.afegirLlibre(l);

        File dump = dumpTemporal();
        domini.copiaSegToSQL(dump);
        cp.netejarAllData();
        cp.restaurarFromSQLFile(dump);

        assertThat(llegirComplet(8101L).obtenirDescripcio()).isEqualTo(descripcio);
    }

    @Test
    @DisplayName("contracte: la còpia de seguretat conserva el text multilínia i les cometes simples")
    void copiaSeguretatConservaTextMultiliniaICometes() throws Exception {
        String descripcio = "Primera línia\nSegona línia\nTercera línia";
        String notes = "Una nota amb 'cometes simples' i -- dos guions";
        Llibre l = Llibre.builder().isbn(8201L).nom("Llibre multilínia").descripcio(descripcio).build();
        l.posarNotes(notes);
        domini.afegirLlibre(l);

        File dump = dumpTemporal();
        domini.copiaSegToSQL(dump);
        cp.netejarAllData();
        cp.restaurarFromSQLFile(dump);

        Llibre restaurat = llegirComplet(8201L);
        assertThat(restaurat.obtenirDescripcio()).isEqualTo(descripcio);
        assertThat(restaurat.obtenirNotes()).isEqualTo(notes);
    }

    @Test
    @DisplayName("contracte: la còpia de seguretat deixa la coberta fora a propòsit")
    void copiaSeguretatOmetLaCoberta() throws Exception {
        domini.afegirLlibre(llibre(8301L, "Llibre amb coberta a la còpia"));
        byte[] portada = new byte[512];
        cp.posarLlibreBlob(8301L, portada);

        File dump = dumpTemporal();
        domini.copiaSegToSQL(dump);
        cp.netejarAllData();
        cp.restaurarFromSQLFile(dump);

        // El volcat SQL exclou la coberta de manera deliberada (l'usuari ha
        // de tornar a baixar les cobertes). El contracte ho deixa escrit per
        //què ningú no ho interpreti com un error de restauració.
        assertThat(cp.obtenirLlibreBlob(8301L)).isNull();
        assertThat(Files.readString(dump.toPath(), StandardCharsets.UTF_8))
            .doesNotContain("imatge_blob");
    }

    private File dumpTemporal() throws java.io.IOException {
        File f = File.createTempFile("biblioteca_contract_", ".sql");
        f.deleteOnExit();
        return f;
    }
}
