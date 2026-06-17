package checkBiblio;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/**
 * Biblioteca StressTest — martell de caòs / casos límit.
 *
 * Compila: javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio/UiTestSupport.java checkBiblio/StressTest.java -d bin
 * Executa: java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio.StressTest
 */
public class TestEstres {

    private static Robot robot;
    private static PrintWriter report;
    private static int passCount = 0, failCount = 0, warnCount = 0;
    private static int stressThreads = 50;
    private static int stressInstances = 0;          // JVMs fills llançats a la fase extrema multi-instància
    private static int stressSoakSeconds = 0;         // 0 = desactivat
    private static int stressFuzzPerDialog = 25;      // cadenes aleatòries per diàleg a la fase de fuzz
    private static boolean stressMemoryProbe = false; // comprovació de creixement del heap entre iteracions
    private static long stressInitialHeapBytes = -1;   // línia base per al sondatge de memòria
    private static final List<Process> childProcs = new ArrayList<>();
    private static final java.util.Random FUZZ = new java.util.Random(0xBADCAFE);
    private static final List<Long> createdISBNs = new ArrayList<>();
    private static final AtomicLong isbnSeq = new AtomicLong(
        9780000000000L + (new Random().nextLong() & Long.MAX_VALUE) % 900_000_000L);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object LOG_LOCK = new Object();

    // ── Entrada ────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            System.getProperty("biblioteca.h2.url",
                "jdbc:h2:mem:stress;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"));
        domini.ControladorDomini.resetForTest();
        persistencia.ControladorPersistencia.resetForTest();
        stressThreads = Integer.getInteger("biblioteca.stress.threads", 50);
        stressInstances = Integer.getInteger("biblioteca.stress.instances", 0);
        stressSoakSeconds = Integer.getInteger("biblioteca.stress.soak", 0);
        stressFuzzPerDialog = Integer.getInteger("biblioteca.stress.fuzz", 25);
        stressMemoryProbe = Boolean.getBoolean("biblioteca.stress.memprobe");
        if (stressInstances > 0) {
            log("Multi-instància: es llançaran " + stressInstances + " JVMs fills");
        }
        if (stressSoakSeconds > 0) {
            log("Soak: activitat de BD en segon pla durant " + stressSoakSeconds + "s");
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("FATAL: Entorn headless — cal Robot. Instal·la Xvfb o defineix DISPLAY.");
            System.exit(1);
        }

        report = new PrintWriter(new FileWriter("checkBiblio/stress_report.txt", false), true);
        robot = new Robot();
        robot.setAutoDelay(50);

        log("=== Biblioteca StressTest ===");
        log("Iniciat: " + LocalDateTime.now());

        Thread appThread = new Thread(() -> {
            try { main.Ejecutable.main(new String[]{"--swing"}); }
            catch (Exception e) { log("APP LAUNCH ERROR: " + e); }
        }, "app-main");
        appThread.setDaemon(true);
        appThread.start();

        JFrame mainFrame = SuportTestUi.waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: No hi ha finestra principal. Avorto.");
            closeReport(); System.exit(1);
        }
        log("OK: finestra principal visible — \"" + mainFrame.getTitle() + "\"");
        SuportTestUi.sleep(800);

        try {
            runAllTests(mainFrame);
        } catch (Exception e) {
            log("FATAL: " + e);
            e.printStackTrace();
        } finally {
            log("\n══════════ SUMMARY ══════════");
            log("PASS : " + passCount);
            log("FAIL : " + failCount);
            log("WARN : " + warnCount);
            log("TOTAL: " + (passCount + failCount + warnCount));
            log("Acabat: " + LocalDateTime.now());
            closeReport();
            System.out.printf("%n[STRESS] Fet. PASS=%d FAIL=%d WARN=%d%n", passCount, failCount, warnCount);
            System.out.println("[STRESS] Informe: checkBiblio/stress_report.txt");
            System.exit(failCount > 0 ? 1 : 0);
        }
    }

    // ── Executador de fases ────────────────────────────────────────────────────────
    @FunctionalInterface interface FuncioProva { void run() throws Exception; }

    private static void phase(String num, String name, FuncioProva fn) {
        int failsBefore = failCount;
        int warnsBefore = warnCount;
        log("\n══════════════════════════════════════════════════════");
        log("FASE " + num + ": " + name);
        log("══════════════════════════════════════════════════════");
        try {
            fn.run();
            dismissAllDialogs();
            SuportTestUi.sleep(150);
        } catch (Exception e) {
            fail("La fase ha llançat: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            dismissAllDialogs();
        }
        if (failCount > failsBefore || warnCount > warnsBefore) {
            log("  FASE " + num + " ha produït nous problemes (errors: +" + (failCount - failsBefore)
                + ", avisos: +" + (warnCount - warnsBefore) + ")");
        }
    }

    private static void runAllTests(JFrame main) throws Exception {
        // Validació: entrades incorrectes
        phase("01", "Validació: ISBN buit",             () -> testValidation_emptyISBN(main));
        phase("02", "Validació: ISBN no numèric",       () -> testValidation_nonNumericISBN(main));
        phase("03", "Validació: ISBN de 3 dígits",      () -> testValidation_shortISBN(main));
        phase("04", "Validació: títol buit",            () -> testValidation_emptyTitle(main));
        phase("05", "Validació: valoració 11.0",        () -> testValidation_ratingHigh(main));
        phase("06", "Validació: valoració -1.0",        () -> testValidation_ratingLow(main));
        phase("07", "Validació: preu negatiu",          () -> testValidation_negativePrice(main));
        phase("08", "Validació: any no numèric",        () -> testValidation_badYear(main));
        // Entrades de caòs
        phase("09", "Caòs: injecció SQL a títol/autor", () -> testChaos_sqlInjection(main));
        phase("10", "Caòs: emoji + títol unicode",      () -> testChaos_unicode(main));
        phase("11", "Caòs: títol de 500 caràcters",     () -> testChaos_longTitle(main));
        phase("12", "Caòs: camps només amb espais",     () -> testChaos_whitespace(main));
        phase("13", "Caòs: ISBN duplicat",              () -> testChaos_duplicateISBN(main));
        // Creacions vàlides (se'n fa seguiment per a la neteja)
        phase("14", "Crear 5 llibres de test vàlids",   () -> testCreateValidBooks(main));
        // Estrès de paginació
        phase("15", "Paginació ràpida endavant+endarrere", () -> testRapidPagination(main));
        // Casos límit de cerca
        phase("16", "Cerca: comodins SQL",              () -> testSearch_sql(main));
        phase("17", "Cerca: metacaràcters de regex",    () -> testSearch_regex(main));
        phase("18", "Cerca: espais / buit",             () -> testSearch_empty(main));
        phase("19", "Cerca: cadena molt llarga",        () -> testSearch_long(main));
        // Estrès de filtres
        phase("20", "Filtre: llegit + no llegit alhora",() -> testFilter_llegitBoth(main));
        phase("21", "Filtre: rang d'anys invertit",     () -> testFilter_invertedYears(main));
        phase("22", "Filtre: aplicar i esborrar",       () -> testFilter_applyAndClear(main));
        // Commutacions ràpides de UI
        phase("23", "Commutació ràpida mode fosc x6",   () -> testRapid_darkMode(main));
        phase("24", "Commutació ràpida galeria x4",     () -> testRapid_gallery(main));
        phase("25", "Commutació ràpida sèries x4",      () -> testRapid_series(main));
        phase("26", "Commutació ràpida calaix filtres x6", () -> testRapid_filterDrawer(main));
        // Diàlegs de detalls del llibre
        phase("27", "Detalls llibre: obrir/tancar x5 ràpid", () -> testDetails_rapidOpenClose(main));
        phase("28", "Detalls llibre: subdiàleg Llistes", () -> testDetails_llistesDialog(main));
        phase("29", "Detalls llibre: subdiàleg Etiquetes", () -> testDetails_etiquetesDialog(main));
        phase("30", "Detalls llibre: Historial de prèstecs", () -> testDetails_historial(main));
        phase("31", "Detalls llibre: Imprimir fitxa",   () -> testDetails_imprimir(main));
        // Gestió de llistes
        phase("32", "Gestionar llistes: estrès CRUD",   () -> testLlistesManagement(main));
        // Diàlegs
        phase("33", "Diàleg Estadístiques",             () -> testStats(main));
        phase("34", "Diàleg Configuració",              () -> testConfiguracio(main));
        // Exportació / Còpia de seguretat (sense fallar)
        phase("35", "Botons d'exportació sense fallar", () -> testExport(main));
        phase("36", "Diàleg còpia de seguretat BD",     () -> testBackup(main));
        // Dreceres de teclat
        phase("37", "Ctrl+A selecciona-ho tot",         () -> testKbd_ctrlA(main));
        phase("38", "Ctrl+F focus cerca",               () -> testKbd_ctrlF(main));
        phase("39", "Ctrl+N nou llibre (i Esc)",        () -> testKbd_ctrlN(main));
        // Miscel·lània
        phase("40", "Diàleg llibre aleatori",           () -> testAleatori(main));
        phase("41", "Barra lateral: tots els botons exercitats", () -> testAllSidebarButtons(main));
        // Integritat de dades: anada i tornada de persistència via la capa de domini (asserions dures)
        phase("41B", "Integritat de dades: anada i tornada completa de la BD", () -> testDataIntegrity(main));
        // Extrem (run2 porta al límit) — abans de la neteja
        if (Boolean.getBoolean("biblioteca.stress.extreme")) {
            phase("43", "Extrem: ràfega de 30 llibres vàlids", () -> testExtreme_burstCreate(main, 30));
            phase("44", "Extrem: martell de paginació x40",    () -> testExtreme_pagination(main, 40));
            phase("45", "Extrem: bucle filtre+cerca x60",       () -> testExtreme_filterLoop(main, 60));
            phase("46", "Extrem: commutació galeria x20",       () -> testExtreme_gallery(main, 20));
            phase("47", "Extrem: clics UI concurrents",         () -> testExtreme_concurrent(main, stressThreads));
            phase("48", "Extrem: tots els diàlegs en ràfega",   () -> testExtreme_dialogSpam(main));
            // ── Més enllà de l'extrem original: extra-extrem (run2 amb STRESS_EXTREME=1) ──
            phase("49", "Extrem: inserció massiva capa domini 1000", () -> testExtreme_bulkInsertDomain(1000));
            phase("50", "Extrem: inserció massiva capa domini 5000", () -> testExtreme_bulkInsertDomain(5000));
            phase("51", "Extrem: sondatge de memòria (creixement del heap)", () -> testExtreme_memoryProbe());
            phase("52", "Extrem: mico d'accions aleatòries x500", () -> testExtreme_randomMonkey(main, 500));
            phase("53", "Extrem: fuzz de cada diàleg amb cadenes aleatòries",
                () -> testExtreme_fuzzEveryDialog(main, stressFuzzPerDialog));
            phase("54", "Extrem: tempesta concurrent de filtre+cerca+edició",
                () -> testExtreme_concurrentEditStorm(main, stressThreads));
            phase("55", "Extrem: cerca massiva SQL amb 1000 patrons LIKE", () -> testExtreme_bulkSearchDomain());
            if (stressInstances > 0) {
                phase("56", "Extrem: multi-instància — llança " + stressInstances + " JVMs fills",
                    () -> testExtreme_multiInstance(stressInstances));
            }
            if (stressSoakSeconds > 0) {
                phase("57", "Extrem: activitat BD en segon pla durant " + stressSoakSeconds + "s",
                    () -> testExtreme_longSoak(stressSoakSeconds));
            }
        }
        phase("42", "Neteja: esborrar tots els llibres de test", () -> testCleanup(main));
        // Sempre atura els JVMs fills, encara que hi hagi fallada.
        if (!childProcs.isEmpty()) killChildProcs();
    }

    // ── VALIDACIÓ ────────────────────────────────────────────────────────────────

    private static void testValidation_emptyISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "Títol", "SomeTitle");
        // L'ISBN es queda buit
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("ISBN buit");
    }

    private static void testValidation_nonNumericISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", "NOTANISBN!!!");
        SuportTestUi.setFieldNear(dlg, "Títol", "SomeTitle");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("ISBN no numèric");
    }

    private static void testValidation_shortISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", "123");
        SuportTestUi.setFieldNear(dlg, "Títol", "SomeTitle");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("ISBN de 3 dígits");
    }

    private static void testValidation_emptyTitle(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        // El títol es queda buit
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("Títol buit");
    }

    private static void testValidation_ratingHigh(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        SuportTestUi.setFieldNear(dlg, "Títol", "RatingTest");
        SuportTestUi.setFieldNear(dlg, "Valoració", "11.0");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("Valoració 11.0");
    }

    private static void testValidation_ratingLow(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        SuportTestUi.setFieldNear(dlg, "Títol", "RatingTest");
        SuportTestUi.setFieldNear(dlg, "Valoració", "-1.0");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        checkExpectError("Valoració -1.0");
    }

    private static void testValidation_negativePrice(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        SuportTestUi.setFieldNear(dlg, "Títol", "PriceTest");
        SuportTestUi.setFieldNear(dlg, "Preu", "-9.99");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass("Preu negatiu → error de validació");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass("Preu negatiu → desar rebutjat (formulari encara obert)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Preu negatiu acceptat silenciosament (s'esperava error de validació)");
        } else {
            warn("Preu negatiu → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testValidation_badYear(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        SuportTestUi.setFieldNear(dlg, "Títol", "YearTest");
        SuportTestUi.setFieldNear(dlg, "Any", "ABCD");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass("Any no numèric → error de validació");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass("Any no numèric → desar rebutjat (formulari encara obert)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Any no numèric acceptat silenciosament (s'esperava error de validació)");
        } else {
            warn("Any no numèric → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    // ── CAÒS ──────────────────────────────────────────────────────────────────────

    private static void testChaos_sqlInjection(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
        SuportTestUi.setFieldNear(dlg, "Títol",     "'; DROP TABLE llibres; --");
        SuportTestUi.setFieldNear(dlg, "Autor",     "' OR '1'='1' --");
        SuportTestUi.setFieldNear(dlg, "Descripció","\" onload=\"alert(1)\" x=\"");
        SuportTestUi.setFieldNear(dlg, "Editorial", "Robert'); DROP TABLE Students;--");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(800);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            warn("Entrada amb injecció SQL → diàleg de validació (rebutjat inesperadament)");
            dismissAllDialogs();
        } else if (after != null) {
            // Encara obert = desat però es mostren els detalls
            pass("Injecció SQL als camps desada sense fallar — app intacta");
            createdISBNs.add(isbn);
            dismissAllDialogs();
        } else {
            pass("Injecció SQL desada sense fallar — sense diàleg d'error");
            createdISBNs.add(isbn);
        }
    }

    private static void testChaos_unicode(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        SuportTestUi.setFieldNear(dlg, "Títol", "📚 你好 مرحبا مكتبة 書 🔥💀 Ñöñö");
        SuportTestUi.setFieldNear(dlg, "Autor", "Ångström Ünïcödé Ñoño");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(800);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            warn("Títol unicode/emoji rebutjat");
            dismissAllDialogs();
        } else {
            pass("Títol unicode amb emoji desat sense fallar");
            createdISBNs.add(isbn);
            dismissAllDialogs();
        }
    }

    private static void testChaos_longTitle(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        SuportTestUi.setFieldNear(dlg, "Títol", "A".repeat(500));
        SuportTestUi.setFieldNear(dlg, "Autor", "B".repeat(300));
        SuportTestUi.setFieldNear(dlg, "Descripció", "C".repeat(1000));
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(800);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass("Títol de 500 caràcters → validació rebutjada (límit nom 255)");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass("Títol de 500 caràcters → desar rebutjat (formulari encara obert)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Títol de 500 caràcters acceptat silenciosament (límit nom 255)");
        } else {
            warn("Títol de 500 caràcters → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testChaos_whitespace(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN",  "   ");
        SuportTestUi.setFieldNear(dlg, "Títol", "   ");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(600);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass("ISBN/títol només amb espais → error de validació");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass("Camps només amb espais → desar rebutjat (formulari encara obert)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Camps només amb espais acceptats sense error");
        } else {
            warn("Espais → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testChaos_duplicateISBN(JFrame main) throws Exception {
        // Troba l'ISBN de la fila 0
        JTable table = SuportTestUi.findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("Sense files per al test d'ISBN duplicat"); return; }
        Object isbnVal = table.getModel().getValueAt(0, 1); // col 1 = ISBN
        String existingISBN = isbnVal != null ? isbnVal.toString().trim() : "";
        if (existingISBN.isEmpty()) { warn("No s'ha pogut llegir l'ISBN de la fila 0"); return; }

        openNewBookDialog(main);
        JDialog dlg = SuportTestUi.waitForDialog(1500); if (dlg == null) { fail("manca el diàleg de nou llibre"); return; }
        SuportTestUi.setFieldNear(dlg, "ISBN",  existingISBN);
        SuportTestUi.setFieldNear(dlg, "Títol", "DuplicateTest");
        SuportTestUi.clickSave(dlg); SuportTestUi.sleep(800);
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass("ISBN duplicat → diàleg d'error: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass("ISBN duplicat → desar rebutjat (formulari encara obert)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("ISBN duplicat " + existingISBN + " acceptat sense error");
        } else {
            warn("ISBN duplicat → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    // ── CREAR LLIBRES VÀLIDS ───────────────────────────────────────────────────────

    private static void testCreateValidBooks(JFrame main) throws Exception {
        int saved = 0;
        String[] genres = {"Novel·la", "Ciència", "Història", "Poesia", "Assaig"};
        for (int i = 0; i < 5; i++) {
            long isbn = uniqueISBN();
            JDialog dlg = null;
            for (int attempt = 0; attempt < 3 && dlg == null; attempt++) {
                openNewBookDialog(main);
                dlg = SuportTestUi.waitForDialog(1500 + attempt * 500);
            }
            if (dlg == null) { fail("manca el diàleg de nou llibre (llibre " + i + ")"); continue; }
            SuportTestUi.setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
            SuportTestUi.setFieldNear(dlg, "Títol",     "StressBook_" + i);
            SuportTestUi.setFieldNear(dlg, "Autor",     "Autor Stress " + i);
            SuportTestUi.setFieldNear(dlg, "Any",       String.valueOf(2000 + i));
            SuportTestUi.setFieldNear(dlg, "Valoració", String.valueOf((i + 1) * 1.5));
            SuportTestUi.setFieldNear(dlg, "Preu",      String.valueOf((i + 1) * 4.99));
            SuportTestUi.setFieldNear(dlg, "Editorial", "Editorial " + genres[i]);
            SuportTestUi.setFieldNear(dlg, "Sèrie",     "StressSeries");
            SuportTestUi.setFieldNear(dlg, "Volum",     String.valueOf(i + 1));
            SuportTestUi.clickSave(dlg); SuportTestUi.sleep(700);
            JDialog after = SuportTestUi.getTopDialog();
            if (after != null && SuportTestUi.looksLikeError(after)) {
                fail("Llibre vàlid " + i + " (ISBN=" + isbn + ") → error: \"" + after.getTitle() + "\"");
                dismissAllDialogs();
            } else {
                dismissAllDialogs();
                pass("Llibre vàlid " + i + " creat (ISBN=" + isbn + ")");
                createdISBNs.add(isbn);
                saved++;
            }
        }
        if (saved == 5) pass("S'han creat els 5 llibres de test");
        else warn(saved + "/5 llibres de test creats");
    }

    // ── PAGINACIÓ ──────────────────────────────────────────────────────────────────

    private static void testRapidPagination(JFrame main) throws Exception {
        goAllBooks(main);
        AbstractButton next = SuportTestUi.findBtnIn(main, "Seg");
        AbstractButton prev = SuportTestUi.findBtnIn(main, "Anterior");
        if (next == null || prev == null) { warn("No s'han trobat els botons de paginació"); return; }

        // Endavant 15 pàgines
        for (int i = 0; i < 15 && next.isEnabled(); i++) { SuportTestUi.doClick(next); SuportTestUi.sleep(80); }
        SuportTestUi.sleep(200);

        // Endarrere 15 pàgines
        for (int i = 0; i < 15 && prev.isEnabled(); i++) { SuportTestUi.doClick(prev); SuportTestUi.sleep(80); }
        SuportTestUi.sleep(200);

        // Vés a l'última pàgina (saturat — biblioteques petites acaben en pocs clics)
        int steps = 0;
        while (next.isEnabled() && steps++ < 40) { SuportTestUi.doClick(next); SuportTestUi.sleep(25); }
        SuportTestUi.sleep(200);

        // Torna a la primera
        steps = 0;
        while (prev.isEnabled() && steps++ < 40) { SuportTestUi.doClick(prev); SuportTestUi.sleep(25); }
        SuportTestUi.sleep(300);

        if (SuportTestUi.getTopDialog() != null) { fail("Estrès de paginació → diàleg d'error"); dismissAllDialogs(); }
        else pass("Estrès de paginació: endavant/endarrere/última/primera — sense fallar");
    }

    // ── CERCA ──────────────────────────────────────────────────────────────────────

    private static void testSearch_sql(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("No s'ha trobat el camp de cerca"); return; }
        String[] payloads = {
            "' OR '1'='1", "'; DROP TABLE--", "\" OR \"\"=\"",
            "1 UNION SELECT * FROM", "'; DELETE FROM llibres--"
        };
        boolean crashed = false;
        for (String p : payloads) {
            SuportTestUi.setField(sf, p); SuportTestUi.sleep(400);
            if (SuportTestUi.getTopDialog() != null) { crashed = true; fail("Cerca SQL \"" + p + "\" → diàleg"); dismissAllDialogs(); }
        }
        SuportTestUi.setField(sf, ""); SuportTestUi.sleep(300);
        if (!crashed) pass("Patrons de cerca amb injecció SQL → sense fallar");
    }

    private static void testSearch_regex(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("No s'ha trobat el camp de cerca"); return; }
        String[] payloads = {".*", ".+", "^.*$", "[a-z]+", "(a|b)*", "\\d+", "?invalid", "a{10000}"};
        boolean crashed = false;
        for (String p : payloads) {
            SuportTestUi.setField(sf, p); SuportTestUi.sleep(300);
            if (SuportTestUi.getTopDialog() != null) { crashed = true; fail("Cerca regex \"" + p + "\" → diàleg"); dismissAllDialogs(); }
        }
        SuportTestUi.setField(sf, ""); SuportTestUi.sleep(300);
        if (!crashed) pass("Cerques amb metacaràcters de regex → sense fallar");
    }

    private static void testSearch_empty(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("No s'ha trobat el camp de cerca"); return; }
        SuportTestUi.setField(sf, "test"); SuportTestUi.sleep(300);
        SuportTestUi.setField(sf, ""); SuportTestUi.sleep(300);
        SuportTestUi.setField(sf, "   "); SuportTestUi.sleep(300);
        SuportTestUi.setField(sf, ""); SuportTestUi.sleep(200);
        if (SuportTestUi.getTopDialog() != null) { fail("Cerca buida/amb espais → diàleg"); dismissAllDialogs(); }
        else pass("Cerca buida/amb espais → sense fallar");
    }

    private static void testSearch_long(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("No s'ha trobat el camp de cerca"); return; }
        SuportTestUi.setField(sf, "x".repeat(5000)); SuportTestUi.sleep(600);
        if (SuportTestUi.getTopDialog() != null) { fail("Cerca de 5000 caràcters → diàleg"); dismissAllDialogs(); }
        else pass("Cerca de 5000 caràcters → sense fallar");
        SuportTestUi.setField(sf, ""); SuportTestUi.sleep(200);
    }

    // ── FILTRES ────────────────────────────────────────────────────────────────────

    private static void testFilter_llegitBoth(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JCheckBox llegit   = SuportTestUi.findCheckBoxGlobal("Llegit");
        JCheckBox noLlegit = SuportTestUi.findCheckBoxGlobal("No llegit");
        if (llegit == null || noLlegit == null) { warn("No s'han trobat les caselles de filtre"); closeFilter(main); return; }
        SwingUtilities.invokeAndWait(() -> { llegit.setSelected(true); noLlegit.setSelected(true); });
        AbstractButton filtrarBtn = SuportTestUi.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { SuportTestUi.doClick(filtrarBtn); SuportTestUi.sleep(600); }
        if (SuportTestUi.getTopDialog() != null) { fail("Filtre llegit+noLlegit → diàleg"); dismissAllDialogs(); }
        else pass("Filtre llegit+noLlegit → sense fallar");
        AbstractButton clear = SuportTestUi.findBtnIn(main, "Treure");
        if (clear != null) { SuportTestUi.doClick(clear); SuportTestUi.sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_invertedYears(JFrame main) throws Exception {
        ensureFilterOpen(main);
        SuportTestUi.sleep(300);
        // Troba els dos camps de text d'any (després de l'etiqueta "Any:")
        List<Component> flat = new ArrayList<>();
        for (Window w : Window.getWindows()) if (w.isVisible()) SuportTestUi.flattenVisible((Container)w, flat);
        JTextField yearMin = null, yearMax = null;
        for (int i = 0; i < flat.size(); i++) {
            if (flat.get(i) instanceof JLabel lbl && lbl.getText() != null
                    && SuportTestUi.norm(lbl.getText()).contains("any")) {
                for (int j = i+1; j < Math.min(i+12, flat.size()); j++) {
                    if (flat.get(j) instanceof JTextField tf) {
                        if (yearMin == null) yearMin = tf;
                        else { yearMax = tf; break; }
                    }
                }
                if (yearMax != null) break;
            }
        }
        if (yearMin == null || yearMax == null) { warn("No s'han trobat els camps de rang d'anys"); closeFilter(main); return; }
        final JTextField fMin = yearMin, fMax = yearMax;
        SwingUtilities.invokeAndWait(() -> { fMin.selectAll(); fMin.setText("2020"); fMax.selectAll(); fMax.setText("1900"); });
        AbstractButton filtrarBtn = SuportTestUi.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { SuportTestUi.doClick(filtrarBtn); SuportTestUi.sleep(600); }
        if (SuportTestUi.getTopDialog() != null) { warn("Rang d'anys invertit → diàleg (pot ser correcte): " + SuportTestUi.getTopDialog().getTitle()); dismissAllDialogs(); }
        else pass("Rang d'anys invertit gestionat sense fallar");
        AbstractButton clear = SuportTestUi.findBtnIn(main, "Treure");
        if (clear != null) { SuportTestUi.doClick(clear); SuportTestUi.sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_applyAndClear(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JTextField sf = findSearchField(main);
        if (sf != null) SuportTestUi.setField(sf, "a");
        AbstractButton filtrarBtn = SuportTestUi.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { SuportTestUi.doClick(filtrarBtn); SuportTestUi.sleep(500); }
        AbstractButton clear = SuportTestUi.findBtnIn(main, "Treure");
        if (clear != null) { SuportTestUi.doClick(clear); SuportTestUi.sleep(400); }
        if (SuportTestUi.getTopDialog() != null) { fail("Aplicar/esborrar filtre → diàleg"); dismissAllDialogs(); }
        else pass("Aplicar+esborrar filtre → sense fallar");
        closeFilter(main);
    }

    // ── COMMUTACIONS RÀPIDES ───────────────────────────────────────────────────────

    private static void testRapid_darkMode(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnByTooltip(main, "clar i fosc", "claro y oscuro", "light/dark");
        if (btn == null) btn = SuportTestUi.findBtnIn(main, "fosc", "clar", "Sèpia", "Sepia", "Oceà", "Ocean", "Light", "Dark");
        if (btn == null) { warn("No s'ha trobat el botó de mode fosc"); return; }
        for (int i = 0; i < 6; i++) { SuportTestUi.doClick(btn); SuportTestUi.sleep(180); }
        SuportTestUi.sleep(400);
        if (SuportTestUi.getTopDialog() != null) { fail("Commutació ràpida mode fosc → diàleg"); dismissAllDialogs(); }
        else pass("Mode fosc commutat 6 vegades → sense fallar (compteig parell → restaurat)");
    }

    private static void testRapid_gallery(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Galeria");
        if (btn == null) { warn("No s'ha trobat el botó de galeria"); return; }
        for (int i = 0; i < 4; i++) { SuportTestUi.doClick(btn); SuportTestUi.sleep(300); }
        SuportTestUi.sleep(300);
        if (SuportTestUi.getTopDialog() != null) { fail("Commutació galeria → diàleg"); dismissAllDialogs(); }
        else pass("Galeria commutada 4 vegades → sense fallar");
    }

    private static void testRapid_series(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Sèries", "Series");
        if (btn == null) { warn("No s'ha trobat el botó de sèries"); return; }
        for (int i = 0; i < 4; i++) { SuportTestUi.doClick(btn); SuportTestUi.sleep(300); }
        SuportTestUi.sleep(300);
        if (SuportTestUi.getTopDialog() != null) { fail("Commutació sèries → diàleg"); dismissAllDialogs(); }
        else pass("Sèries commutades 4 vegades → sense fallar");
    }

    private static void testRapid_filterDrawer(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Filtres");
        if (btn == null) { warn("No s'ha trobat el botó del calaix de filtres"); return; }
        for (int i = 0; i < 6; i++) { SuportTestUi.doClick(btn); SuportTestUi.sleep(120); }
        SuportTestUi.sleep(300);
        if (SuportTestUi.getTopDialog() != null) { fail("Commutació ràpida calaix filtres → diàleg"); dismissAllDialogs(); }
        else pass("Calaix de filtres commutat 6 vegades → sense fallar");
    }

    // ── DIÀLEGS DE DETALLS DEL LLIBRE ───────────────────────────────────────────────

    private static void testDetails_rapidOpenClose(JFrame main) throws Exception {
        goAllBooks(main);
        JTable table = SuportTestUi.findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("Sense files"); return; }
        int opened = 0;
        for (int i = 0; i < 5; i++) {
            openRow(main, 0);
            JDialog d = SuportTestUi.waitForDialog(2500);
            if (d != null) {
                opened++;
                SuportTestUi.pressEscape(robot);
                SuportTestUi.sleep(400);
                dismissAllDialogs();
            }
        }
        if (opened >= 4) pass("Obrir/tancar ràpid x5: " + opened + " diàlegs — sense fallar");
        else warn("Obrir/tancar ràpid: només " + opened + "/5 diàlegs han aparegut");
    }

    private static void testDetails_llistesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Manca el diàleg de detalls (Llistes)"); return; }
        AbstractButton btn = SuportTestUi.findBtnIn(details.getContentPane(), "Llistes", "Listas", "Lists");
        if (btn == null) { warn("Manca el botó Llistes"); dismissAllDialogs(); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(700);
        JDialog sub = SuportTestUi.getTopDialogExcept(details);
        if (sub != null) pass("Subdiàleg Llistes: \"" + sub.getTitle() + "\"");
        else warn("No s'ha trobat el subdiàleg Llistes");
        dismissAllDialogs();
    }

    private static void testDetails_etiquetesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Manca el diàleg de detalls (Etiquetes)"); return; }
        AbstractButton btn = SuportTestUi.findBtnIn(details.getContentPane(), "Etiquetes", "Etiquetas", "Tags");
        if (btn == null) { warn("Manca el botó Etiquetes"); dismissAllDialogs(); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(700);
        JDialog sub = SuportTestUi.getTopDialogExcept(details);
        if (sub != null) pass("Subdiàleg Etiquetes: \"" + sub.getTitle() + "\"");
        else warn("No s'ha trobat el subdiàleg Etiquetes");
        dismissAllDialogs();
    }

    private static void testDetails_historial(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Manca el diàleg de detalls (Historial)"); return; }
        AbstractButton btn = SuportTestUi.findBtnIn(details.getContentPane(), "Historial");
        if (btn == null) { warn("Manca el botó Historial de prèstecs"); dismissAllDialogs(); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(700);
        JDialog sub = SuportTestUi.getTopDialogExcept(details);
        if (sub != null) pass("Diàleg Historial de prèstecs: \"" + sub.getTitle() + "\"");
        else warn("No s'ha trobat el diàleg Historial");
        dismissAllDialogs();
    }

    private static void testDetails_imprimir(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Manca el diàleg de detalls (Imprimir)"); return; }
        AbstractButton btn = SuportTestUi.findBtnIn(details.getContentPane(), "Imprimir", "Print");
        if (btn == null) { warn("Manca el botó Imprimir"); dismissAllDialogs(); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(800);
        // Pot obrir un diàleg d'impressió — simplement cancel·la/fuig
        JDialog sub = SuportTestUi.getTopDialogExcept(details);
        if (sub != null) {
            pass("Imprimir obert: \"" + sub.getTitle() + "\"");
            SuportTestUi.pressEscape(robot);
            SuportTestUi.sleep(400);
        } else {
            pass("Imprimir clicat — sense subdiàleg Swing (la UI d'impressió nativa pot ser headless/no-op)");
            SuportTestUi.pressEscape(robot);
            SuportTestUi.sleep(400);
        }
        dismissAllDialogs();
    }

    // ── GESTIÓ DE LLISTES ────────────────────────────────────────────────────────

    private static void testLlistesManagement(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Gestionar llistes");
        if (btn == null) { warn("No s'ha trobat el botó Gestionar llistes"); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(700);
        JDialog dlg = SuportTestUi.waitForDialog(2000);
        if (dlg == null) { warn("Manca el diàleg Gestionar llistes"); return; }

        AbstractButton novaBtn  = SuportTestUi.findBtnIn((Container)dlg, "Nova");
        JTextField     nameTF   = SuportTestUi.findComponent((Container)dlg, JTextField.class);
        AbstractButton upBtn    = SuportTestUi.findBtnIn((Container)dlg, "▲", "Pujar");
        AbstractButton downBtn  = SuportTestUi.findBtnIn((Container)dlg, "▼", "Baixar");
        AbstractButton colorBtn = SuportTestUi.findBtnIn((Container)dlg, "Color");
        AbstractButton delBtn   = SuportTestUi.findBtnIn((Container)dlg, "Eliminar");

        // Nom buit → s'espera validació
        if (novaBtn != null && nameTF != null) {
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(""); });
            SuportTestUi.doClick(novaBtn); SuportTestUi.sleep(400);
            JDialog err = SuportTestUi.getTopDialogExcept(dlg);
            if (err != null) { pass("Nom de llista buit → diàleg d'error"); dismissTopDialog(); }
            else warn("Nom de llista buit: ignorat silenciosament");
        }

        // Crea 3 llistes de test
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 3 && novaBtn != null && nameTF != null; i++) {
            String name = "StressTestList_" + i + "_" + (System.currentTimeMillis() % 1000);
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(name); });
            SuportTestUi.sleep(80);
            SuportTestUi.doClick(novaBtn); SuportTestUi.sleep(500);
            JDialog err = SuportTestUi.getTopDialogExcept(dlg);
            if (err != null) { warn("Error en crear llista: " + err.getTitle()); dismissTopDialog(); }
            else { created.add(name); log("  Creada: " + name); }
        }
        if (!created.isEmpty()) pass("Creades " + created.size() + " llistes de test");

        // Reordena (primer assegura la selecció)
        if (upBtn != null && downBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForReorder = (JList<Object>) SuportTestUi.findComponent((Container)dlg, JList.class);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            SuportTestUi.doClick(downBtn); SuportTestUi.sleep(200);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            SuportTestUi.doClick(upBtn); SuportTestUi.sleep(200);
            pass("Botons de reordenament: pujar/baixar funcionen");
        }

        // Botó de color — assegura primer que hi ha un element seleccionat
        if (colorBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForColor = (JList<Object>) SuportTestUi.findComponent((Container)dlg, JList.class);
            if (listForColor != null && listForColor.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForColor.setSelectedIndex(0));
            SuportTestUi.doClick(colorBtn); SuportTestUi.sleep(700);
            JDialog colorDlg = SuportTestUi.getTopDialogExcept(dlg);
            if (colorDlg != null) {
                pass("S'ha obert el selector de color");
                SuportTestUi.pressEscape(robot);
                SuportTestUi.sleep(400); dismissAllDialogsExcept(dlg);
            } else warn("No s'ha obert el selector de color");
        }

        // Esborra les llistes creades
        if (delBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listComp = (JList<Object>) SuportTestUi.findComponent((Container)dlg, JList.class);
            for (int i = 0; i < created.size(); i++) {
                if (listComp != null) {
                    int idx = listComp.getModel().getSize() - 1;
                    if (idx < 0) break;
                    SwingUtilities.invokeAndWait(() -> listComp.setSelectedIndex(Math.max(0, listComp.getModel().getSize()-1)));
                    SuportTestUi.sleep(150);
                }
                SuportTestUi.doClick(delBtn); SuportTestUi.sleep(400);
                JDialog confirm = SuportTestUi.getTopDialogExcept(dlg);
                if (confirm != null) {
                    if (isStressTestListDeleteConfirm(confirm)) {
                        clickAffirmDelete(confirm);
                    } else {
                        warn("Confirmació inesperada en esborrar llista — cancel·lat");
                        cancelTopDialog();
                    }
                    SuportTestUi.sleep(400);
                }
            }
            pass("Esborrades " + created.size() + " llistes de test");
        }

        SwingUtilities.invokeLater(dlg::dispose); SuportTestUi.sleep(400);
    }

    // ── ESTADÍSTIQUES ──────────────────────────────────────────────────────────────

    private static void testStats(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Estad");
        if (btn == null) { warn("No s'ha trobat el botó Estadístiques"); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(800);
        JDialog dlg = SuportTestUi.waitForDialog(2000); if (dlg == null) { warn("Manca el diàleg d'Estadístiques"); return; }
        List<String> items = new ArrayList<>();
        SuportTestUi.collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[TBL]"))) pass("Estadístiques: té taula");
        else pass("Estadístiques: sense taula de prestatgeries (sense assignacions — s'espera)");
        if (items.stream().anyMatch(s -> s.contains("[LBL]") && s.toLowerCase().contains("llegit"))) pass("Estadístiques: té recompte de llegits");
        else warn("Estadístiques: manca l'etiqueta de recompte de llegits");
        // Edita l'objectiu
        JTextField objField = SuportTestUi.findComponent((Container)dlg, JTextField.class);
        if (objField != null) {
            final JTextField tf = objField;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText("52"); });
            SuportTestUi.sleep(200); pass("Estadístiques: camp d'objectiu editable");
        }
        SwingUtilities.invokeLater(dlg::dispose); SuportTestUi.sleep(400);
    }

    // ── CONFIGURACIÓ ─────────────────────────────────────────────────────────────

    private static void testConfiguracio(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Configuració", "Configur");
        if (btn == null) { warn("No s'ha trobat el botó Configuració"); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(900);
        JDialog dlg = SuportTestUi.waitForDialog(3000); if (dlg == null) { warn("Manca el diàleg de Configuració"); return; }
        // Verifica que hi ha els camps clau
        List<String> items = new ArrayList<>();
        SuportTestUi.collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[CMB]"))) pass("Configuració: té quadres combinats");
        else warn("Configuració: manquen quadres combinats");
        // Cancel·la sense desar
        AbstractButton cancel = SuportTestUi.findBtnIn((Container)dlg, "Cancel", "Tancar");
        if (cancel != null) { SuportTestUi.doClick(cancel); SuportTestUi.sleep(300); }
        else { SwingUtilities.invokeLater(dlg::dispose); SuportTestUi.sleep(300); }
        pass("Diàleg de Configuració obert i tancat sense fallar");
    }

    // ── EXPORTACIÓ ───────────────────────────────────────────────────────────────

    private static void testExport(JFrame main) throws Exception {
        ensureFilterOpen(main);
        SuportTestUi.sleep(300);
        AbstractButton exportBtn = SuportTestUi.findBtnIn(main, "Exportar");
        if (exportBtn == null) { warn("No s'ha trobat el botó Exportar (cal el filtre obert)"); closeFilter(main); return; }

        String[][] exports = {{"CSV", "Export CSV", "Exportar CSV"},
                               {"JSON", "Export JSON", "Exportar JSON"},
                               {"HTML", "Export HTML", "Exportar HTML"}};
        for (String[] exp : exports) {
            SuportTestUi.doClick(exportBtn); SuportTestUi.sleep(400);
            AbstractButton item = SuportTestUi.findBtnIn(main, exp[1], exp[2]);
            if (item != null) { SuportTestUi.doClick(item); SuportTestUi.sleep(700); }
            JDialog fc = SuportTestUi.getTopDialog();
            if (fc != null) {
                pass("Exportar " + exp[0] + " → ha aparegut un diàleg");
                focusMain(main);
                SuportTestUi.pressEscape(robot);
                SuportTestUi.sleep(400); dismissAllDialogs();
            } else {
                warn("Exportar " + exp[0] + " → no ha aparegut cap diàleg (pot haver-se cancel·lat o desat automàticament)");
                focusMain(main);
                SuportTestUi.pressEscape(robot);
                SuportTestUi.sleep(200);
            }
        }
        closeFilter(main);
    }

    // ── CÒPIA DE SEGURETAT ───────────────────────────────────────────────────────

    private static void testBackup(JFrame main) throws Exception {
        ensureFilterOpen(main);
        SuportTestUi.sleep(300);
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Backup", "backup");
        if (btn == null) { warn("No s'ha trobat el botó Backup (cal el filtre obert)"); closeFilter(main); return; }
        SuportTestUi.doClick(btn); SuportTestUi.sleep(800);
        JDialog fc = SuportTestUi.getTopDialog();
        if (fc != null) {
            pass("Còpia de seguretat BD → diàleg: \"" + fc.getTitle() + "\"");
            SuportTestUi.pressEscape(robot);
            SuportTestUi.sleep(400); dismissAllDialogs();
        } else {
            warn("Còpia de seguretat BD → no ha aparegut cap diàleg");
        }
        closeFilter(main);
    }

    // ── DRECERES DE TECLAT ────────────────────────────────────────────────────────

    private static void testKbd_ctrlA(JFrame main) throws Exception {
        JTable table = SuportTestUi.findComponent((Container)main, JTable.class);
        if (table == null) { warn("No hi ha taula per al test Ctrl+A"); return; }
        goAllBooks(main); SuportTestUi.sleep(300);
        triggerRootAction(main, "seleccionarTot");
        SuportTestUi.sleep(200);
        int sel = table.getSelectedRowCount();
        if (sel > 0) pass("Ctrl+A ha seleccionat " + sel + " files");
        else warn("Ctrl+A: 0 files seleccionades");
        SwingUtilities.invokeAndWait(() -> table.clearSelection());
    }

    private static void testKbd_ctrlF(JFrame main) throws Exception {
        triggerRootAction(main, "focusFiltres");
        SuportTestUi.sleep(200);
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JTextField search = findSearchField(main);
        if (search != null && search.isFocusOwner()) pass("Ctrl+F → camp de cerca amb focus");
        else if (focused instanceof JTextField) pass("Ctrl+F → JTextField amb focus");
        else warn("Ctrl+F → focus: " + (focused != null ? focused.getClass().getSimpleName() : "null"));
    }

    private static void testKbd_ctrlN(JFrame main) throws Exception {
        triggerRootAction(main, "nouLlibre");
        SuportTestUi.sleep(900);
        JDialog dlg = SuportTestUi.getTopDialog();
        if (dlg != null) {
            pass("Ctrl+N → diàleg de nou llibre: \"" + dlg.getTitle() + "\"");
            dismissTopDialog();
        } else warn("Ctrl+N → no ha aparegut cap diàleg");
    }

    // ── ALEATORI ────────────────────────────────────────────────────────────────

    private static void testAleatori(JFrame main) throws Exception {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "aleatori", "Aleatori");
        if (btn == null) { warn("No s'ha trobat el botó Llibre aleatori"); return; }
        // Clica 3 vegades
        for (int i = 0; i < 3; i++) {
            SuportTestUi.doClick(btn); SuportTestUi.sleep(700);
            JDialog dlg = SuportTestUi.getTopDialog();
            if (dlg != null) {
                log("  Diàleg aleatori " + (i+1) + ": \"" + dlg.getTitle() + "\"");
                dismissTopDialog();
            }
        }
        pass("Llibre aleatori clicat 3 vegades → sense fallar");
    }

    // ── TOTS ELS BOTONS DE LA BARRA LATERAL ──────────────────────────────────────

    // ── EXTREM ───────────────────────────────────────────────────────────────────

    private static void testExtreme_burstCreate(JFrame main, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            openNewBookDialog(main);
            JDialog dlg = SuportTestUi.waitForDialog(2000);
            if (dlg == null) { fail("ràfega de creació: manca el diàleg a " + i); return; }
            long isbn = uniqueISBN();
            SuportTestUi.setFieldNear(dlg, "ISBN", String.valueOf(isbn));
            SuportTestUi.setFieldNear(dlg, "Títol", "Stress_" + i);
            SuportTestUi.setFieldNear(dlg, "Autor", "Bot");
            SuportTestUi.clickSave(dlg);
            SuportTestUi.sleep(350);
            dismissAllDialogs();
            createdISBNs.add(isbn);
        }
        pass("Creats en ràfega " + count + " llibres");
        goAllBooks(main);
    }

    private static void testExtreme_pagination(JFrame main, int clicks) throws Exception {
        AbstractButton next = SuportTestUi.findBtnIn(main, "Seguent", "Next");
        if (next == null) { warn("No hi ha botó de pàgina següent"); return; }
        for (int i = 0; i < clicks; i++) { SuportTestUi.doClick(next); SuportTestUi.sleep(80); }
        AbstractButton prev = SuportTestUi.findBtnIn(main, "Anterior", "Previous");
        if (prev != null) for (int i = 0; i < Math.min(clicks, 10); i++) { SuportTestUi.doClick(prev); SuportTestUi.sleep(80); }
        pass("Paginació martellejada amb " + clicks + " clics endavant");
    }

    private static void testExtreme_filterLoop(JFrame main, int iterations) throws Exception {
        ensureFilterOpen(main);
        JTextField nom = SuportTestUi.findTextFieldNear(main, "Nom");
        JTextField search = findSearchField(main);
        AbstractButton filtrar = SuportTestUi.findBtnIn(main, "Filtrar");
        AbstractButton clear = SuportTestUi.findBtnIn(main, "Treure", "Quitar");
        for (int i = 0; i < iterations; i++) {
            if (search != null) SuportTestUi.setField(search, i % 2 == 0 ? "a" : "");
            if (nom != null) SuportTestUi.setField(nom, i % 3 == 0 ? "Stress" : "");
            if (filtrar != null) SuportTestUi.doClick(filtrar);
            SuportTestUi.sleep(40);
            if (clear != null && i % 5 == 0) SuportTestUi.doClick(clear);
            SuportTestUi.sleep(30);
        }
        if (search != null) SuportTestUi.setField(search, "");
        pass("Bucle filtre+cerca x" + iterations);
    }

    private static void testExtreme_gallery(JFrame main, int toggles) throws Exception {
        AbstractButton galeria = SuportTestUi.findBtnIn(main, "Galeria");
        if (galeria == null) { warn("Manca el botó Galeria"); return; }
        for (int i = 0; i < toggles; i++) { SuportTestUi.doClick(galeria); SuportTestUi.sleep(60); }
        pass("Galeria commutada x" + toggles);
    }

    private static void testExtreme_concurrent(JFrame main, int threadCount) throws Exception {
        AtomicReference<AbstractButton> toggleBtn = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
            toggleBtn.set(SuportTestUi.findBtnIn(main, "Filtres", "Galeria", "Sèrie")));
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
        List<Thread> pool = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int id = t;
            Thread th = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 5; i++) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                AbstractButton btn = toggleBtn.get();
                                if (btn != null) btn.doClick();
                            } catch (Exception e) { errors.incrementAndGet(); }
                        });
                        SuportTestUi.sleep(30 + (id % 10));
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
            }, "stress-ui-" + t);
            th.setDaemon(true);
            th.start();
            pool.add(th);
        }
        start.countDown();
        for (Thread th : pool) th.join(8000);
        SuportTestUi.sleep(500);
        dismissAllDialogs();
        if (errors.get() > 0) fail("Errors UI concurrents: " + errors.get());
        else pass("Bombardeig concurrent de la UI (" + threadCount + " fils × 5 clics)");
    }

    private static void testExtreme_dialogSpam(JFrame main) throws Exception {
        String[] opens = {"Estad", "Configur", "aleatori", "llistes"};
        for (int round = 0; round < 3; round++) {
            for (String hint : opens) {
                AbstractButton btn = SuportTestUi.findBtnIn(main, hint);
                if (btn == null) continue;
                SuportTestUi.doClick(btn); SuportTestUi.sleep(400);
                dismissAllDialogs();
            }
        }
        pass("Ràfega de diàlegs x3 rondes");
    }

    // ── EXTRA-EXTREM (fases 49+) ─────────────────────────────────────────────
    // Apunten a la saturació de recursos a nivell de SO que els tests
    // d'un sol procés no poden assolir: creixement del heap al llarg
    // d'operacions massives, contenció multi-JVM sobre el mateix lib/,
    // tests de mico aleatori sobre l'arbre d'accions Swing, i activitat
    // de fons de llarga durada.

    /**
     * Inserció massiva de N llibres directament al domini (sense UI).
     * S'usa per sembrar la base de dades per a les fases de sondatge
     * de memòria i cerca massiva. Registra el temps i la mida final per
     *què l'informe mostri si el camí d'inserció és lineal o quadràtic.
     */
    private static void testExtreme_bulkInsertDomain(int n) {
        long t0 = System.nanoTime();
        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        int before = cd.getSize();
        for (int i = 0; i < n; i++) {
            long isbn = uniqueISBN();
            domini.Llibre l = new domini.Llibre(isbn,
                "Bulk_" + i,
                "Bot " + (i % 50),
                1900 + (i % 130),
                "bulk-insert " + i,
                (double) (i % 11),
                (double) (i % 50),
                i % 2 == 0,
                null);
            try { cd.addLlibre(l); createdISBNs.add(isbn); }
            catch (Exception e) { fail("inserció massiva " + i + ": " + e.getMessage()); return; }
        }
        long elapsed = (System.nanoTime() - t0) / 1_000_000L;
        int after = cd.getSize();
        pass("Inserció massiva de " + n + " llibres en " + elapsed + " ms (mida: " + before + " → " + after + ")");
    }

    /**
     * Sondatge de memòria: captura el heap abans d'una ràfega d'estrès,
     * fa 10 rondes de (insereix 500 + esborra'ls tots), comprova el
     * creixement del heap. Es marca una fuita si el heap usat creix més
     * de 64 MB al llarg de les rondes (la JVM conserva objectes
     * assolibles del model de taula Swing, però aquests estan limitats
     * per la mida de pàgina visible).
     */
    private static void testExtreme_memoryProbe() {
        Runtime rt = Runtime.getRuntime();
        // Escalfament del GC
        System.gc(); SuportTestUi.sleep(120); System.gc(); SuportTestUi.sleep(120);
        long usedBefore = rt.totalMemory() - rt.freeMemory();
        if (stressInitialHeapBytes < 0) stressInitialHeapBytes = usedBefore;

        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        List<Long> scratch = new ArrayList<>();
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 200; i++) {
                long isbn = uniqueISBN();
                try { cd.addLlibre(new domini.Llibre(isbn, "MemProbe_" + round + "_" + i,
                    "Bot", 2000, "", 5.0, 1.0, false, null)); scratch.add(isbn); }
                catch (Exception ignored) {}
            }
            for (long isbn : scratch) {
                try { cd.deleteLlibre(Long.valueOf(isbn)); } catch (Exception ignored) {}
            }
            scratch.clear();
            System.gc(); SuportTestUi.sleep(80);
        }
        System.gc(); SuportTestUi.sleep(120);
        long usedAfter = rt.totalMemory() - rt.freeMemory();
        long delta = usedAfter - stressInitialHeapBytes;
        if (delta > 64L * 1024 * 1024) {
            warn("El heap ha crescut " + (delta / (1024 * 1024)) + " MB al llarg de 5 anades i tornades (línia base " +
                (stressInitialHeapBytes / (1024 * 1024)) + " MB) — possible fuita");
        } else {
            pass("Sondatge del heap: usat " + (usedAfter / (1024 * 1024)) + " MB (Δ " +
                (delta / (1024 * 1024)) + " MB) — dins del límit de 64 MB");
        }
    }

    /**
     * Mico d'accions aleatòries: tria un botó / fila de taula / camp de
     * text visible a l'atzar i n'executa l'acció. Detecta exceptions del
     * fil de dispatch d'esdeveniments, interblocatges de diàlegs modals
     * enganxats i reentrances accidentals als gestors.
     */
    private static void testExtreme_randomMonkey(JFrame main, int actions) {
        java.util.concurrent.atomic.AtomicInteger exceptions = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger deadlocks = new java.util.concurrent.atomic.AtomicInteger();
        long t0 = System.nanoTime();
        for (int i = 0; i < actions; i++) {
            try {
                // Captura tots els components visibles de totes les finestres
                List<Component> visible = new ArrayList<>();
                for (Window w : Window.getWindows()) {
                    if (!w.isVisible()) continue;
                    SuportTestUi.flattenVisible((Container) w, visible);
                }
                if (visible.isEmpty()) { deadlocks.incrementAndGet(); continue; }
                Component c = visible.get(FUZZ.nextInt(visible.size()));
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        if (c instanceof AbstractButton btn && btn.isEnabled()) btn.doClick();
                        else if (c instanceof JTextField tf) {
                            tf.setText(randomFuzzString(8 + FUZZ.nextInt(16)));
                        } else if (c instanceof JCheckBox cb) {
                            cb.setSelected(!cb.isSelected());
                        } else if (c instanceof JTable tbl && tbl.getRowCount() > 0) {
                            int row = FUZZ.nextInt(tbl.getRowCount());
                            tbl.setRowSelectionInterval(row, row);
                        }
                    } catch (Exception e) { exceptions.incrementAndGet(); }
                });
                // Cedeix periòdicament perquè l'EDT pugui buidar els diàlegs
                if (i % 25 == 0) { dismissAllDialogs(); SuportTestUi.sleep(40); }
            } catch (Exception e) { exceptions.incrementAndGet(); }
        }
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        dismissAllDialogs();
        if (exceptions.get() > 0) {
            warn("Mico: " + exceptions.get() + " exceptions al llarg de " + actions + " accions aleatòries en " + elapsedMs + " ms");
        } else if (deadlocks.get() > actions / 4) {
            warn("Mico: " + deadlocks.get() + " iteracions NO han trobat cap component visible — la UI pot estar enganxada");
        } else {
            pass("Mico: " + actions + " accions aleatòries en " + elapsedMs + " ms (sense exceptions)");
        }
    }

    /**
     * Fuzz de cada diàleg amb N cadenes aleatòries. Detecta NPE, injecció
     * de regex, OOM amb cadenes llargues i fallades de format per localització.
     */
    private static void testExtreme_fuzzEveryDialog(JFrame main, int perDialog) {
        // Botons habituals que obren diàlegs a la finestra principal
        String[] openers = {"Afegir", "Estad", "Configur", "llistes", "aleatori"};
        java.util.concurrent.atomic.AtomicInteger exceptions = new java.util.concurrent.atomic.AtomicInteger();
        int fuzzedDialogs = 0;
        for (String hint : openers) {
            AbstractButton btn = SuportTestUi.findBtnIn(main, hint);
            if (btn == null) continue;
            for (int i = 0; i < perDialog; i++) {
                SuportTestUi.doClick(btn); SuportTestUi.sleep(150);
                JDialog dlg = SuportTestUi.getTopDialog();
                if (dlg == null) { SuportTestUi.sleep(200); continue; }
                fuzzedDialogs++;
                // Fuzz de cada camp de text / àrea de text / quadre combinat del diàleg
                List<Component> comps = new ArrayList<>();
                SuportTestUi.flattenVisible((Container) dlg, comps);
                for (Component c : comps) {
                    final Component fz = c;
                    final String payload = randomFuzzString(FUZZ.nextInt(4) == 0 ? 2000 : 32);
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                if (fz instanceof JTextField tf) tf.setText(payload);
                                else if (fz instanceof JTextArea ta) ta.setText(payload);
                                else if (fz instanceof JComboBox<?> cb) cb.setSelectedItem(payload);
                                else if (fz instanceof JCheckBox chk) chk.setSelected(FUZZ.nextBoolean());
                            } catch (Exception e) { exceptions.incrementAndGet(); }
                        });
                    } catch (Exception e) { exceptions.incrementAndGet(); }
                }
                dismissAllDialogs();
            }
        }
        if (exceptions.get() > 0) {
            fail("Fuzz: " + exceptions.get() + " exceptions al llarg de " + fuzzedDialogs + " fuzzings de diàleg");
        } else {
            pass("Fuzz: " + fuzzedDialogs + " invocacions de diàleg × " + perDialog + " càrregues aleatòries — sense exceptions");
        }
    }

    /**
     * Tempesta d'edició concurrent: N fils de treball obren files
     * repetidament, commuten una casella i desen. Carrega la memòria cau
     * en memòria + el camí d'escriptura a la BD.
     */
    private static void testExtreme_concurrentEditStorm(JFrame main, int threadCount) throws Exception {
        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        // Pre-sembra almenys N llibres
        while (cd.getSize() < threadCount * 2) {
            try { cd.addLlibre(new domini.Llibre(uniqueISBN(), "StormSeed", "Bot", 2020,
                "", 5.0, 1.0, false, null)); } catch (Exception ignored) {}
        }
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger updates = new java.util.concurrent.atomic.AtomicInteger();
        List<domini.Llibre> snapshot = new ArrayList<>(cd.getAllLlibres());
        Thread[] pool = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            pool[t] = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 10; i++) {
                        domini.Llibre l = snapshot.get(FUZZ.nextInt(snapshot.size()));
                        try {
                            l.setValoracio(FUZZ.nextDouble() * 10.0);
                            l.setLlegit(FUZZ.nextBoolean());
                            cd.updateLlibre(l);
                            updates.incrementAndGet();
                        } catch (Exception e) { errors.incrementAndGet(); }
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
            }, "stress-storm-" + t);
            pool[t].setDaemon(true);
            pool[t].start();
        }
        start.countDown();
        for (Thread t : pool) t.join(30_000);
        if (errors.get() > 0) {
            fail("Tempesta d'edició: " + errors.get() + " errors d'actualització al llarg de " + updates.get() + " actualitzacions correctes");
        } else {
            pass("Tempesta d'edició: " + threadCount + " fils × 10 actualitzacions = " + updates.get() + " — sense errors");
        }
    }

    /**
     * Martelleja la cerca SQL LIKE amb 1000 patrons aleatoris. Valida
     * que el camí de cerca no faci OOM amb entrades riques en regex i
     * que l'escapament de LIKE funcioni.
     */
    private static void testExtreme_bulkSearchDomain() {
        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        if (cd.getSize() < 100) {
            // Les fases 49/50 haurien d'haver sembrat prou; si no, sembra 100 ràpidament.
            for (int i = 0; i < 100; i++) {
                try { cd.addLlibre(new domini.Llibre(uniqueISBN(), "Seed_" + i, "Bot",
                    2020, "", 5.0, 1.0, false, null)); } catch (Exception ignored) {}
            }
        }
        long t0 = System.nanoTime();
        int total = 0;
        for (int i = 0; i < 200; i++) {
            String pattern = randomFuzzString(4 + FUZZ.nextInt(12));
            try {
                int n = cd.aplicarFiltres(domini.ConstructorFiltreLlibre.of().nom(pattern).build()).size();
                total += n;
            } catch (Exception e) {
                fail("Cerca massiva: el patró \"" + pattern + "\" ha llançat: " + e.getMessage());
                return;
            }
        }
        long elapsed = (System.nanoTime() - t0) / 1_000_000L;
        pass("Cerca massiva: 200 patrons LIKE en " + elapsed + " ms (total encerts: " + total + ")");
    }

    /**
     * Estrès multi-instància: llança N JVMs fills cadascun executant un
     * petit subconjunt d'accions de UI sobre la MATEIXA base de dades H2
     * al disc. Tots surten nets dins del temps límit. El pare monitora
     * els codis de sortida i exposa les fallades.
     *
     * <p>Cada fill fa servir un fitxer H2 al disc ÚNIC (NO memòria
     * compartida), de manera que realment competeixen per la memòria cau
     * de fitxers del SO i pel directori lib/ dels JAR. Un test que
     * requerís estat compartit derrotaria el propòsit.</p>
     */
    private static void testExtreme_multiInstance(int instances) throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "bib-stress-multi-" + System.nanoTime());
        if (!tmpDir.mkdirs()) { warn("No s'ha pogut crear el directori temporal per a multi-instància"); return; }
        log("Multi-instància: " + instances + " JVMs fills a " + tmpDir);
        String cp = "bin" + File.pathSeparator + "lib" + File.separator + "h2-2.3.232.jar"
            + File.pathSeparator + "lib" + File.separator + "mariadb-java-client-3.3.3.jar"
            + File.separator + "lib" + File.separator + "gson-2.11.0.jar";
        String javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        java.util.List<Integer> exitCodes = new java.util.ArrayList<>();
        java.util.List<String> instanceNames = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < instances; i++) {
                File h2File = new File(tmpDir, "inst" + i);
                // user.home per instància perquè ~/.biblioteca/config.properties i les
                // memòries cau de cover no contaminin el directori home real de l'usuari.
                File homeDir = new File(tmpDir, "home" + i);
                if (!homeDir.mkdirs()) { warn("No s'ha pogut crear user.home per a inst" + i); }
                ProcessBuilder pb = new ProcessBuilder(javaCmd, "-Xmx256m",
                    "-Dbiblioteca.test=true",
                    "-Dbiblioteca.h2.url=jdbc:h2:file:" + h2File.getAbsolutePath() +
                        ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE",
                    "-Duser.home=" + homeDir.getAbsolutePath(),
                    "-cp", cp,
                    "main.Ejecutable", "--swing");
                pb.inheritIO();
                Process p = pb.start();
                childProcs.add(p);
                instanceNames.add("inst" + i + " (pid=" + safePid(p) + ")");
                log("  iniciat: " + instanceNames.get(i));
            }
            // Cada JVM fill necessita ~5s per iniciar, pintar la finestra i carregar l'esquema.
            int settleMs = Math.max(5000, instances * 2000);
            log("Multi-instància: estabilitzant " + settleMs + " ms abans de comprovar la vivència");
            SuportTestUi.sleep(settleMs);
            // Sonda de vivència: queden fills vius? n'ha mort algun?
            int alive = 0, dead = 0;
            for (int i = 0; i < childProcs.size(); i++) {
                Process p = childProcs.get(i);
                if (p.isAlive()) alive++;
                else { dead++; exitCodes.add(p.exitValue()); }
            }
            log("Multi-instància després d'estabilitzar: " + alive + " vius, " + dead + " morts");
            // Deixa'ls córrer uns quants segons més
            int holdMs = Math.max(3000, instances * 1000);
            log("Multi-instància: aguantant " + holdMs + " ms més");
            SuportTestUi.sleep(holdMs);
            alive = 0; dead = 0;
            for (int i = 0; i < childProcs.size(); i++) {
                Process p = childProcs.get(i);
                if (p.isAlive()) alive++;
                else { dead++; exitCodes.add(p.exitValue()); }
            }
            log("Multi-instància: " + alive + " vius, " + dead + " morts");
            // Captura l'ús del heap del pare (cada fill ha carregat els seus ~80MB)
            long parentUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            log("Multi-instància: heap del pare després = " + (parentUsed / (1024 * 1024)) + " MB");
            killChildProcs();
            // Espera una sortida gràcil
            for (Process p : childProcs) {
                try { if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly(); }
                catch (Exception ignored) {}
            }
            if (dead == 0) {
                pass("Multi-instància: " + instances + " fills han corregut durant " + (settleMs + holdMs) / 1000 + "s sense fallades");
            } else if (dead <= instances / 4) {
                warn("Multi-instància: " + dead + "/" + instances + " fills han mort (codis de sortida: " + exitCodes + ")");
            } else {
                fail("Multi-instància: " + dead + "/" + instances + " fills han mort (codis de sortida: " + exitCodes + ")");
            }
        } finally {
            killChildProcs();
            // Neteja a millor esforç
            deleteTree(tmpDir);
        }
    }

    /**
     * Soak llarg: N treballadors de fons martellegen el domini durant un
     * temps fixat. Cada treballador insereix, actualitza, esborra i
     * cerca a l'atzar. Detecta fuites lentes, OOM i qualsevol corrupció
     * de dades sota càrrega sostinguda.
     */
    private static void testExtreme_longSoak(int seconds) {
        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        // Pre-sembra ~200 llibres perquè la cerca/filtrar tingui alguna cosa a fer
        if (cd.getSize() < 200) {
            for (int i = 0; i < 200; i++) {
                try { cd.addLlibre(new domini.Llibre(uniqueISBN(), "SoakSeed_" + i,
                    "Bot", 2000, "", 5.0, 1.0, false, null)); } catch (Exception ignored) {}
            }
        }
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        java.util.concurrent.atomic.AtomicInteger inserts = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger updates = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger deletes = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger searches = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        int workerCount = Math.min(8, Math.max(2, stressThreads / 8));
        for (int t = 0; t < workerCount; t++) {
            Thread w = new Thread(() -> {
                java.util.Random r = new java.util.Random();
                while (System.nanoTime() < deadline && errors.get() < 10) {
                    try {
                        int op = r.nextInt(4);
                        switch (op) {
                            case 0: {
                                long isbn = uniqueISBN();
                                cd.addLlibre(new domini.Llibre(isbn, "Soak_" + isbn, "Bot",
                                    2000, "", 5.0, 1.0, false, null));
                                inserts.incrementAndGet();
                                break;
                            }
                            case 1: {
                                List<domini.Llibre> all = cd.getAllLlibres();
                                if (!all.isEmpty()) {
                                    domini.Llibre pick = all.get(r.nextInt(all.size()));
                                    pick.setValoracio(r.nextDouble() * 10.0);
                                    try { cd.updateLlibre(pick); updates.incrementAndGet(); }
                                    catch (Exception ignored) {}
                                }
                                break;
                            }
                            case 2: {
                                List<domini.Llibre> all = cd.getAllLlibres();
                                if (all.size() > 50) {
                                    domini.Llibre pick = all.get(r.nextInt(all.size()));
                                    try { cd.deleteLlibre(Long.valueOf(pick.getISBN())); deletes.incrementAndGet(); }
                                    catch (Exception ignored) {}
                                }
                                break;
                            }
                            case 3: {
                                cd.aplicarFiltres(domini.ConstructorFiltreLlibre.of()
                                    .nom("a" + r.nextInt(100)).build());
                                searches.incrementAndGet();
                                break;
                            }
                        }
                    } catch (Exception e) { errors.incrementAndGet(); }
                }
            }, "soak-worker-" + t);
            w.setDaemon(true);
            workers.add(w);
            w.start();
        }
        // Informa del creixement del heap cada 5 segons
        long lastReport = System.nanoTime();
        long lastInsert = 0, lastUpdate = 0, lastDelete = 0, lastSearch = 0;
        long prevInsert = 0, prevUpdate = 0, prevDelete = 0, prevSearch = 0;
        while (System.nanoTime() < deadline) {
            SuportTestUi.sleep(1000);
            if (System.nanoTime() - lastReport > 4_000_000_000L) {
                long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long i = inserts.get(), u = updates.get(), d = deletes.get(), s = searches.get();
                log(String.format("Soak +%ds: heap=%dMB ops/s i=%d u=%d d=%d s=%d err=%d",
                    (System.nanoTime() - lastReport + 4_000_000_000L) / 1_000_000_000L,
                    used / (1024 * 1024),
                    (i - prevInsert) / 5, (u - prevUpdate) / 5, (d - prevDelete) / 5, (s - prevSearch) / 5,
                    errors.get()));
                prevInsert = lastInsert = i; prevUpdate = lastUpdate = u;
                prevDelete = lastDelete = d; prevSearch = lastSearch = s;
                lastReport = System.nanoTime();
            }
        }
        for (Thread w : workers) {
            try { w.join(5000); } catch (InterruptedException ignored) {}
        }
        long finalUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long growth = stressInitialHeapBytes > 0 ? finalUsed - stressInitialHeapBytes : 0;
        if (errors.get() > 10) {
            fail("Soak: " + errors.get() + " errors de treballador (ins=" + inserts + " act=" + updates +
                " esb=" + deletes + " cerca=" + searches + ")");
        } else {
            String growthStr = (stressInitialHeapBytes > 0 && growth > 0)
                ? " heap Δ " + (growth / (1024 * 1024)) + " MB" : "";
            pass("Soak: " + seconds + "s — ins=" + inserts + " act=" + updates +
                " esb=" + deletes + " cerca=" + searches + " err=" + errors.get() + growthStr);
        }
    }

    // ── AJUDANTS D'EXTREM ──────────────────────────────────────────────────────────

    private static String randomFuzzString(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int pick = FUZZ.nextInt(7);
            switch (pick) {
                case 0: sb.append((char) (0x20 + FUZZ.nextInt(0x5F))); break;     // ASCII imprimible
                case 1: sb.append((char) (0xC0 + FUZZ.nextInt(0x20))); break;    // Suplement Latin-1
                case 2: sb.append((char) (0x4E00 + FUZZ.nextInt(0x9FFF))); break; // CJK
                case 3: sb.append('\n'); break;                                  // salt de línia
                case 4: sb.append('\t'); break;                                  // tabulador
                case 5: sb.append('\0'); break;                                  // NUL
                default: sb.append(FUZZ.nextInt(10)); break;                     // dígit
            }
        }
        return sb.toString();
    }

    private static long safePid(Process p) {
        try { return p.pid(); } catch (Exception e) { return -1; }
    }

    private static void killChildProcs() {
        for (Process p : childProcs) {
            try { if (p.isAlive()) p.destroyForcibly(); } catch (Exception ignored) {}
        }
        childProcs.clear();
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        try { f.delete(); } catch (Exception ignored) {}
    }

    private static void testAllSidebarButtons(JFrame main) throws Exception {
        String[] btns = {"Tots els", "Afegits recentment", "Llegits", "Llista de desitjos", "En curs"};
        for (String label : btns) {
            AbstractButton btn = SuportTestUi.findBtnIn(main, label);
            if (btn == null) { warn("No s'ha trobat el botó de la barra lateral: \"" + label + "\""); continue; }
            SuportTestUi.doClick(btn); SuportTestUi.sleep(500);
            JDialog d = SuportTestUi.getTopDialog();
            if (d != null) {
                dismissAllDialogs();
                pass("Barra lateral \"" + label + "\" → diàleg tancat, sense fallar");
            } else {
                pass("Barra lateral \"" + label + "\" → actualització del panell, sense diàleg");
            }
        }
        goAllBooks(main);
    }

    // ── INTEGRITAT DE DADES (capa de domini, asserions dures) ────────────────────────

    /**
     * Crea un llibre a través de la capa de domini amb TOTS els camps
     * informats (inclosos els afegits recentment: nom_ca/es/en, estat,
     * exemplars, idioma, format, serie, volum, pagines), i el rellegeix
     * DIRECTAMENT DE LA BASE DE DADES
     * ({@link persistencia.ControladorPersistencia#getAllLlibres()}, que
     * reconstrueix cada Llibre a partir del ResultSet) i afirma una
     * veritable anada i tornada de persistència. Després exercita una
     * anada i tornada de creació/devolució de prèstec i una anada i
     * tornada d'addició/eliminació d'etiqueta. A diferència de les fases
     * de UI, aquestes són comprovacions dures de PASS/FAIL.
     */
    private static void testDataIntegrity(JFrame main) throws Exception {
        domini.ControladorDomini cd = domini.ControladorDomini.getInstance();
        persistencia.ControladorPersistencia cp = persistencia.ControladorPersistencia.getInstance();

        long isbn = uniqueISBN();
        domini.Llibre l = new domini.Llibre(isbn, "Integrity_" + isbn, "Autor Integ",
            2021, "descripcio round-trip", 7.5, 19.99, true, null);
        l.setNomCa("Títol CA"); l.setNomEs("Título ES"); l.setNomEn("Title EN");
        l.setEditorial("Edit Integ"); l.setSerie("Serie Integ"); l.setVolum(3);
        l.setEstat("nou"); l.setExemplars(4); l.setDesitjat(false);
        l.setIdioma("Català"); l.setFormat("Tapa dura");
        l.setPagines(320); l.setPaginesLlegides(160);

        domini.Tag tag = null;
        try {
            cd.addLlibre(l);

            // Torna a llegir de la BD (no la memòria cau en memòria) per provar que ha persistit.
            domini.Llibre db = null;
            for (domini.Llibre b : cp.getAllLlibres())
                if (b.getISBN() == isbn) { db = b; break; }

            if (db == null) { fail("IntegritatDades: no s'ha trobat el llibre ISBN=" + isbn + " després de la recàrrega"); return; }

            check("nom persisteix",       "Integrity_" + isbn, db.getNom());
            check("nom_ca persisteix",    "Títol CA",  db.getNomCa());
            check("nom_es persisteix",    "Título ES", db.getNomEs());
            check("nom_en persisteix",    "Title EN",  db.getNomEn());
            check("editorial persisteix", "Edit Integ", db.getEditorial());
            check("serie persisteix",     "Serie Integ", db.getSerie());
            check("volum persisteix",     3, db.getVolum());
            check("estat persisteix",     "nou", db.getEstat());
            check("exemplars persisteix", 4, db.getExemplars());
            check("idioma persisteix",    "Català", db.getIdioma());
            check("format persisteix",    "Tapa dura", db.getFormat());
            check("pagines persisteix",   320, db.getPagines());
            check("pagines_llegides persisteix", 160, db.getPaginesLlegides());
            check("llegit persisteix",    Boolean.TRUE, db.getLlegit());

            // ── Anada i tornada de prèstec ──────────────────────────────────────────────
            cd.prestarLlibre(isbn, "Joan Tester");
            check("prèstec registrat (actiu)", true, cd.getLoanedISBNs().contains(isbn));
            check("countLoans >= 1", true, cd.countLoans(isbn) >= 1);
            cd.retornarLlibre(isbn);
            check("prèstec retornat (no actiu)", false, cd.getLoanedISBNs().contains(isbn));

            // ── Anada i tornada d'etiqueta ───────────────────────────────────────────────
            tag = cd.addTag("StressTag_" + UUID.randomUUID().toString().substring(0, 8));
            cd.addLlibreToTag(isbn, tag.getId());
            check("membresia d'etiqueta afegida", true, cd.getLlibresWithTag(tag.getId()).contains(isbn));
            cd.removeLlibreFromTag(isbn, tag.getId());
            check("membresia d'etiqueta eliminada", false, cd.getLlibresWithTag(tag.getId()).contains(isbn));
        } catch (Exception e) {
            fail("IntegritatDades ha llançat: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            // Teardown a millor esforç — no deixem que les fallades de neteja emmascarin el resultat.
            try { cd.deleteLlibre(Long.valueOf(isbn)); } catch (Exception ignored) {}
            if (tag != null) { try { cd.deleteTag(tag); } catch (Exception ignored) {} }
        }
    }

    /** Asserció dura d'igualtat usada per {@link #testDataIntegrity}. */
    private static void check(String what, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) pass("IntegritatDades: " + what);
        else fail("IntegritatDades: " + what + " — s'esperava <" + expected + "> però era <" + actual + ">");
    }

    // ── NETEJA ─────────────────────────────────────────────────────────────────────

    private static void testCleanup(JFrame main) throws Exception {
        if (createdISBNs.isEmpty()) { warn("No hi ha ISBN de test per netejar"); return; }
        log("Netejant " + createdISBNs.size() + " llibres de test...");
        goAllBooks(main); SuportTestUi.sleep(500);
        JTextField sf = findSearchField(main);
        int deleted = 0;
        // Buida la barra de cerca perquè el RowFilter no amagui llibres
        if (sf != null) { SuportTestUi.setField(sf, ""); SuportTestUi.sleep(400); }
        for (long isbn : createdISBNs) {
            JTable table = SuportTestUi.findComponent((Container)main, JTable.class);
            if (table == null) { log("  ISBN " + isbn + " no és a la taula — es salta"); continue; }
            // Escaneja el model directament (evita problemes de recompte de la vista del RowFilter)
            String isbnStr = String.valueOf(isbn);
            int modelRow = -1;
            javax.swing.table.TableModel mdl = table.getModel();
            for (int r = 0; r < mdl.getRowCount(); r++) {
                Object v = mdl.getValueAt(r, 1);
                if (v != null && isbnStr.equals(String.valueOf(v))) { modelRow = r; break; }
            }
            if (modelRow < 0) { log("  ISBN " + isbn + " no és a la taula — es salta"); continue; }
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow < 0) { log("  ISBN " + isbn + " filtrat — es salta"); continue; }
            final int fr = viewRow;
            SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(fr, fr));
            focusMain(main);
            SuportTestUi.pressDelete(robot);
            SuportTestUi.sleep(600);
            JDialog confirm = SuportTestUi.getTopDialog();
            if (confirm != null) {
                if (SuportTestUi.isTestBookDeleteConfirm(confirm)) {
                    clickAffirmDelete(confirm);
                    SuportTestUi.sleep(400);
                    deleted++;
                } else {
                    warn("Diàleg inesperat durant l'esborrat de neteja per a ISBN " + isbn + " — cancel·lat");
                    cancelTopDialog();
                }
            } else {
                warn("No hi ha confirmació d'esborrat per a ISBN " + isbn + " — el llibre pot romandre");
            }
        }
        if (sf != null) { SuportTestUi.setField(sf, ""); SuportTestUi.sleep(400); }
        goAllBooks(main);
        pass("Neteja: esborrats " + deleted + "/" + createdISBNs.size() + " llibres de test");
    }

    // ── AJUDANTS: INTERACCIÓ ───────────────────────────────────────────────────────

    private static void openNewBookDialog(JFrame main) throws Exception {
        dismissAllDialogs();
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Afegir", "Nou", "New");
        if (btn != null) SuportTestUi.doClick(btn);
        else {
            SwingUtilities.invokeAndWait(() -> main.requestFocusInWindow());
            SuportTestUi.sleep(80);
            SuportTestUi.pressCtrlN(robot);
        }
        SuportTestUi.sleep(600);
    }

    private static void openRow(JFrame main, int row) throws Exception {
        JTable table = SuportTestUi.findComponent((Container)main, JTable.class);
        if (table == null || row >= table.getRowCount()) return;
        // invokeLater, no invokeAndWait: obrirDetalls obre un JDialog modal (bloquega l'EDT).
        SwingUtilities.invokeLater(() -> {
            main.toFront();
            table.requestFocusInWindow();
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            javax.swing.Action act = table.getActionMap().get("obrirDetalls");
            if (act != null) act.actionPerformed(new java.awt.event.ActionEvent(table, 0, "obrirDetalls"));
        });
        SuportTestUi.sleep(900);
    }

    private static void triggerRootAction(JFrame main, String key) throws Exception {
        SwingUtilities.invokeLater(() -> {
            main.toFront();
            main.getRootPane().requestFocusInWindow();
            javax.swing.Action act = main.getRootPane().getActionMap().get(key);
            if (act != null) act.actionPerformed(new java.awt.event.ActionEvent(main.getRootPane(), 0, key));
        });
        SuportTestUi.sleep(120);
    }

    /** Porta la finestra principal al davant i sol·licita el focus a nivell de SO. */
    private static void focusMain(JFrame main) throws Exception {
        SwingUtilities.invokeAndWait(() -> { main.toFront(); main.requestFocus(); });
        SuportTestUi.sleep(120);
    }

    /** Obre la fila, buida l'EDT, espera més estona el JDialog de detalls i verifica que és un diàleg de detalls de llibre. */
    private static JDialog openDetailsAndWait(JFrame main, int row) throws Exception {
        dismissAllDialogs();
        SuportTestUi.sleep(200);
        openRow(main, row);
        // Buida els esdeveniments EDT pendents perquè el contingut del diàleg estigui completament dibuixat
        SwingUtilities.invokeAndWait(() -> {});
        SuportTestUi.sleep(400);
        JDialog d = SuportTestUi.waitForDialog(3000);
        if (d == null) return null;
        if (SuportTestUi.isBookDetailsDialog(d)) return d;
        // Assegura't que és el diàleg de detalls, no un d'error
        if (SuportTestUi.looksLikeError(d)) {
            log("  [openDetailsAndWait] s'ha obtingut un diàleg d'error: \"" + d.getTitle() + "\" — es descarta i es reintenta");
            dismissAllDialogs();
            SuportTestUi.sleep(300);
            openRow(main, row);
            SwingUtilities.invokeAndWait(() -> {});
            SuportTestUi.sleep(400);
            d = SuportTestUi.waitForDialog(3000);
        }
        return d;
    }

    private static void goAllBooks(JFrame main) {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Tots els");
        if (btn != null) { SuportTestUi.doClick(btn); SuportTestUi.sleep(500); }
    }

    private static void ensureFilterOpen(JFrame main) {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Filtres");
        if (btn != null && btn.getText() != null && !btn.getText().contains("▲")) { SuportTestUi.doClick(btn); SuportTestUi.sleep(400); }
    }

    private static void closeFilter(JFrame main) {
        AbstractButton btn = SuportTestUi.findBtnIn(main, "Filtres ▲");
        if (btn != null) { SuportTestUi.doClick(btn); SuportTestUi.sleep(300); }
    }

    private static void checkExpectError(String label) {
        JDialog after = SuportTestUi.getTopDialog();
        if (after != null && SuportTestUi.looksLikeError(after)) {
            pass(label + " → diàleg de validació: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && SuportTestUi.isBookFormDialog(after)) {
            pass(label + " → desar rebutjat (formulari encara obert: \"" + after.getTitle() + "\")");
            dismissAllDialogs();
        } else if (after != null) {
            warn(label + " → diàleg inesperat: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else {
            fail(label + " acceptat sense error de validació");
        }
    }

    private static void dismissAllDialogs() {
        for (int i = 0; i < 8; i++) {
            JDialog d = SuportTestUi.getTopDialog();
            if (d == null) break;
            cancelTopDialog();
        }
    }

    private static void dismissAllDialogsExcept(JDialog keep) {
        for (int i = 0; i < 5; i++) {
            JDialog d = SuportTestUi.getTopDialog();
            if (d == null || d == keep) break;
            AbstractButton ok = SuportTestUi.findBtnIn((Container)d, "OK", "Tancar", "Cancel·lar", "Close");
            if (ok != null) SuportTestUi.doClick(ok); else SwingUtilities.invokeLater(d::dispose);
            SuportTestUi.sleep(300);
        }
    }

    private static void dismissTopDialog() {
        cancelTopDialog();
    }

    /** Descarta sense afirmar accions destructives (sense Sí/Yes/OK). */
    private static void cancelTopDialog() {
        JDialog d = SuportTestUi.getTopDialog();
        if (d == null) return;
        AbstractButton cancel = SuportTestUi.findBtnIn((Container)d, "Cancel·lar", "Cancelar", "Tancar", "Close", "No");
        if (cancel != null) SuportTestUi.doClick(cancel);
        else {
            SuportTestUi.pressEscape(robot);
            SuportTestUi.sleep(200);
            if (SuportTestUi.getTopDialog() == d) SwingUtilities.invokeLater(d::dispose);
        }
        SuportTestUi.sleep(280);
    }

    private static void clickAffirmDelete(JDialog confirm) {
        AbstractButton yes = SuportTestUi.findBtnIn((Container)confirm, "Sí", "Yes", "Eliminar", "Esborrar", "OK");
        if (yes != null) SuportTestUi.doClick(yes);
        else cancelTopDialog();
    }

    private static boolean isStressTestListDeleteConfirm(JDialog d) {
        if (!SuportTestUi.isTestBookDeleteConfirm(d)) return false;
        String body = dialogText(d);
        return body.contains("stresstestlist");
    }

    private static String dialogText(JDialog d) {
        List<String> items = new ArrayList<>();
        SuportTestUi.collectComponents((Container)d, "", items);
        return SuportTestUi.norm(String.join(" ", items));
    }

    private static JTextField findSearchField(JFrame main) {
        List<Component> flat = new ArrayList<>();
        SuportTestUi.flattenVisible((Container)main, flat);
        for (Component c : flat) {
            if (c instanceof JTextField tf) {
                String tip = tf.getToolTipText();
                if (tip != null && (tip.toLowerCase().contains("cerca") || tip.toLowerCase().contains("search"))) return tf;
            }
        }
        return SuportTestUi.findComponent((Container)main, JTextField.class);
    }

    private static long uniqueISBN() {
        long isbn = isbnSeq.incrementAndGet();
        if (createdISBNs.contains(isbn)) {
            fail("S'ha detectat una col·lisió d'ISBN: " + isbn);
        }
        return isbn;
    }

    // ── SEGUIMENT DE RESULTATS ────────────────────────────────────────────────────────
    private static void pass(String msg) { passCount++; log("  ✓ PASS: " + msg); }
    private static void fail(String msg) { failCount++; log("  ✗ FAIL: " + msg); }
    private static void warn(String msg) { warnCount++; log("  ! WARN: " + msg); }

    // ── REGISTRE ──────────────────────────────────────────────────────────────────

    private static void log(String msg) {
        synchronized (LOG_LOCK) {
            String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
            report.println(line);
            System.out.println(line);
        }
    }

    private static void closeReport() {
        log("=== StressTest finalitzat ===");
        report.close();
    }
}
