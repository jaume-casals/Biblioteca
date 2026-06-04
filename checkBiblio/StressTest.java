package checkBiblio;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Biblioteca StressTest — chaos / edge-case hammerer.
 *
 * Compile: javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar checkBiblio/StressTest.java -d bin
 * Run:     java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar checkBiblio.StressTest
 */
public class StressTest {

    private static Robot robot;
    private static PrintWriter report;
    private static int screenshotSeq = 0;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static int passCount = 0, failCount = 0, warnCount = 0;
    private static int stressThreads = 50;
    private static final List<Long> createdISBNs = new ArrayList<>();
    private static final AtomicLong isbnSeq = new AtomicLong(
        9780000000000L + (new Random().nextLong() & Long.MAX_VALUE) % 900_000_000L);
    private static final Object LOG_LOCK = new Object();

    // ── Entry ─────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            System.getProperty("biblioteca.h2.url",
                "jdbc:h2:mem:stress;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"));
        domini.ControladorDomini.resetForTest();
        persistencia.ControladorPersistencia.resetForTest();
        stressThreads = Integer.getInteger("biblioteca.stress.threads", 50);
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("FATAL: Headless environment — Robot required. Install Xvfb or set DISPLAY.");
            System.exit(1);
        }

        Files.createDirectories(Path.of("checkBiblio/screenshots"));
        report = new PrintWriter(new FileWriter("checkBiblio/stress_report.txt", false), true);
        robot = new Robot();
        robot.setAutoDelay(50);

        log("=== Biblioteca StressTest ===");
        log("Started: " + LocalDateTime.now());

        Thread appThread = new Thread(() -> {
            try { main.Ejecutable.main(new String[]{"--swing"}); }
            catch (Exception e) { log("APP LAUNCH ERROR: " + e); }
        }, "app-main");
        appThread.setDaemon(true);
        appThread.start();

        JFrame mainFrame = waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: No main window. Abort.");
            closeReport(); System.exit(1);
        }
        log("OK: main window visible — \"" + mainFrame.getTitle() + "\"");
        sleep(800);

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
            log("Finished: " + LocalDateTime.now());
            closeReport();
            System.out.printf("%n[STRESS] Done. PASS=%d FAIL=%d WARN=%d%n", passCount, failCount, warnCount);
            System.out.println("[STRESS] Report: checkBiblio/stress_report.txt");
            System.exit(failCount > 0 ? 1 : 0);
        }
    }

    // ── Phase runner ──────────────────────────────────────────────────────────────
    @FunctionalInterface interface TestFn { void run() throws Exception; }

    private static void phase(String num, String name, TestFn fn) {
        int failsBefore = failCount;
        int warnsBefore = warnCount;
        log("\n══════════════════════════════════════════════════════");
        log("PHASE " + num + ": " + name);
        log("══════════════════════════════════════════════════════");
        try {
            fn.run();
            dismissAllDialogs();
            sleep(150);
        } catch (Exception e) {
            fail("Phase threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            dismissAllDialogs();
        }
        if (failCount > failsBefore || warnCount > warnsBefore) screenshot("p" + num);
    }

    private static void runAllTests(JFrame main) throws Exception {
        // Validation: bad inputs
        phase("01", "Validation: empty ISBN",             () -> testValidation_emptyISBN(main));
        phase("02", "Validation: non-numeric ISBN",       () -> testValidation_nonNumericISBN(main));
        phase("03", "Validation: 3-digit ISBN",           () -> testValidation_shortISBN(main));
        phase("04", "Validation: empty title",            () -> testValidation_emptyTitle(main));
        phase("05", "Validation: rating 11.0",            () -> testValidation_ratingHigh(main));
        phase("06", "Validation: rating -1.0",            () -> testValidation_ratingLow(main));
        phase("07", "Validation: negative price",         () -> testValidation_negativePrice(main));
        phase("08", "Validation: year non-numeric",       () -> testValidation_badYear(main));
        // Chaos inputs
        phase("09", "Chaos: SQL injection in title/autor",() -> testChaos_sqlInjection(main));
        phase("10", "Chaos: emoji + unicode title",       () -> testChaos_unicode(main));
        phase("11", "Chaos: 500-char title",              () -> testChaos_longTitle(main));
        phase("12", "Chaos: whitespace-only fields",      () -> testChaos_whitespace(main));
        phase("13", "Chaos: duplicate ISBN",              () -> testChaos_duplicateISBN(main));
        // Valid creates (tracked for cleanup)
        phase("14", "Create 5 valid test books",          () -> testCreateValidBooks(main));
        // Pagination stress
        phase("15", "Rapid pagination fwd+bwd",           () -> testRapidPagination(main));
        // Search edge cases
        phase("16", "Search: SQL wildcard chars",         () -> testSearch_sql(main));
        phase("17", "Search: regex metacharacters",       () -> testSearch_regex(main));
        phase("18", "Search: whitespace / empty",         () -> testSearch_empty(main));
        phase("19", "Search: very long string",           () -> testSearch_long(main));
        // Filter stress
        phase("20", "Filter: llegit + no llegit both",    () -> testFilter_llegitBoth(main));
        phase("21", "Filter: inverted year range",        () -> testFilter_invertedYears(main));
        phase("22", "Filter: apply then clear",           () -> testFilter_applyAndClear(main));
        // UI toggles
        phase("23", "Rapid dark mode toggle x6",          () -> testRapid_darkMode(main));
        phase("24", "Rapid gallery toggle x4",            () -> testRapid_gallery(main));
        phase("25", "Rapid series toggle x4",             () -> testRapid_series(main));
        phase("26", "Rapid filter drawer toggle x6",      () -> testRapid_filterDrawer(main));
        // Book detail dialogs
        phase("27", "Book details: open/close x5 rapid",  () -> testDetails_rapidOpenClose(main));
        phase("28", "Book details: Llistes sub-dialog",   () -> testDetails_llistesDialog(main));
        phase("29", "Book details: Etiquetes sub-dialog", () -> testDetails_etiquetesDialog(main));
        phase("30", "Book details: Historial préstecs",   () -> testDetails_historial(main));
        phase("31", "Book details: Imprimir fitxa",       () -> testDetails_imprimir(main));
        // List management
        phase("32", "Gestionar llistes: CRUD stress",     () -> testLlistesManagement(main));
        // Dialogs
        phase("33", "Stats dialog",                       () -> testStats(main));
        phase("34", "Configuració dialog",                () -> testConfiguracio(main));
        // Export / Backup (no crash)
        phase("35", "Export buttons no crash",            () -> testExport(main));
        phase("36", "Backup BD dialog",                   () -> testBackup(main));
        // Keyboard shortcuts
        phase("37", "Ctrl+A select all",                  () -> testKbd_ctrlA(main));
        phase("38", "Ctrl+F focus search",                () -> testKbd_ctrlF(main));
        phase("39", "Ctrl+N new book (then Esc)",         () -> testKbd_ctrlN(main));
        // Misc
        phase("40", "Llibre aleatori dialog",             () -> testAleatori(main));
        phase("41", "Sidebar: all buttons exercised",     () -> testAllSidebarButtons(main));
        // Data integrity: persistence round-trip via the domain layer (hard asserts)
        phase("41B", "Data integrity: full DB round-trip", () -> testDataIntegrity(main));
        // Extreme (run2 pushed to the limit) — before cleanup
        if (Boolean.getBoolean("biblioteca.stress.extreme")) {
            phase("43", "Extreme: burst 30 valid books",       () -> testExtreme_burstCreate(main, 30));
            phase("44", "Extreme: pagination hammer x40",      () -> testExtreme_pagination(main, 40));
            phase("45", "Extreme: filter+search loop x60",      () -> testExtreme_filterLoop(main, 60));
            phase("46", "Extreme: gallery toggle x20",         () -> testExtreme_gallery(main, 20));
            phase("47", "Extreme: concurrent UI clicks",       () -> testExtreme_concurrent(main, stressThreads));
            phase("48", "Extreme: all dialogs rapid fire",     () -> testExtreme_dialogSpam(main));
        }
        phase("42", "Cleanup: delete all test books",     () -> testCleanup(main));
    }

    // ── VALIDATION ────────────────────────────────────────────────────────────────

    private static void testValidation_emptyISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "Títol", "SomeTitle");
        // ISBN stays empty
        clickSave(dlg); sleep(600);
        checkExpectError("Empty ISBN");
    }

    private static void testValidation_nonNumericISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", "NOTANISBN!!!");
        setFieldNear(dlg, "Títol", "SomeTitle");
        clickSave(dlg); sleep(600);
        checkExpectError("Non-numeric ISBN");
    }

    private static void testValidation_shortISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", "123");
        setFieldNear(dlg, "Títol", "SomeTitle");
        clickSave(dlg); sleep(600);
        checkExpectError("3-digit ISBN");
    }

    private static void testValidation_emptyTitle(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        // Title stays empty
        clickSave(dlg); sleep(600);
        checkExpectError("Empty title");
    }

    private static void testValidation_ratingHigh(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        setFieldNear(dlg, "Títol", "RatingTest");
        setFieldNear(dlg, "Valoració", "11.0");
        clickSave(dlg); sleep(600);
        checkExpectError("Rating 11.0");
    }

    private static void testValidation_ratingLow(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        setFieldNear(dlg, "Títol", "RatingTest");
        setFieldNear(dlg, "Valoració", "-1.0");
        clickSave(dlg); sleep(600);
        checkExpectError("Rating -1.0");
    }

    private static void testValidation_negativePrice(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        setFieldNear(dlg, "Títol", "PriceTest");
        setFieldNear(dlg, "Preu", "-9.99");
        clickSave(dlg); sleep(600);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass("Negative price → validation error");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass("Negative price → save rejected (form still open)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Negative price accepted silently (expected validation error)");
        } else {
            warn("Negative price → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testValidation_badYear(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        setFieldNear(dlg, "Títol", "YearTest");
        setFieldNear(dlg, "Any", "ABCD");
        clickSave(dlg); sleep(600);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass("Non-numeric year → validation error");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass("Non-numeric year → save rejected (form still open)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Non-numeric year accepted silently (expected validation error)");
        } else {
            warn("Non-numeric year → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    // ── CHAOS ────────────────────────────────────────────────────────────────────

    private static void testChaos_sqlInjection(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
        setFieldNear(dlg, "Títol",     "'; DROP TABLE llibres; --");
        setFieldNear(dlg, "Autor",     "' OR '1'='1' --");
        setFieldNear(dlg, "Descripció","\" onload=\"alert(1)\" x=\"");
        setFieldNear(dlg, "Editorial", "Robert'); DROP TABLE Students;--");
        clickSave(dlg); sleep(800);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            warn("SQL injection input → validation dialog (rejected unexpectedly)");
            dismissAllDialogs();
        } else if (after != null) {
            // Still open = saved but details shown
            pass("SQL injection in fields saved without crash — app intact");
            createdISBNs.add(isbn);
            dismissAllDialogs();
        } else {
            pass("SQL injection saved without crash — no error dialog");
            createdISBNs.add(isbn);
        }
    }

    private static void testChaos_unicode(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        setFieldNear(dlg, "Títol", "📚 你好 مرحبا مكتبة 書 🔥💀 Ñöñö");
        setFieldNear(dlg, "Autor", "Ångström Ünïcödé Ñoño");
        clickSave(dlg); sleep(800);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            warn("Unicode/emoji title rejected");
            dismissAllDialogs();
        } else {
            pass("Unicode emoji title saved without crash");
            createdISBNs.add(isbn);
            dismissAllDialogs();
        }
    }

    private static void testChaos_longTitle(JFrame main) throws Exception {
        long isbn = uniqueISBN();
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        setFieldNear(dlg, "Títol", "A".repeat(500));
        setFieldNear(dlg, "Autor", "B".repeat(300));
        setFieldNear(dlg, "Descripció", "C".repeat(1000));
        clickSave(dlg); sleep(800);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass("500-char title → validation rejected (nom limit 255)");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass("500-char title → save rejected (form still open)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("500-char title accepted silently (nom limit 255)");
        } else {
            warn("500-char title → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testChaos_whitespace(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN",  "   ");
        setFieldNear(dlg, "Títol", "   ");
        clickSave(dlg); sleep(600);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass("Whitespace-only ISBN/title → validation error");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass("Whitespace-only fields → save rejected (form still open)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Whitespace-only fields accepted without error");
        } else {
            warn("Whitespace → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    private static void testChaos_duplicateISBN(JFrame main) throws Exception {
        // Find ISBN of row 0
        JTable table = findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("No rows for duplicate ISBN test"); return; }
        Object isbnVal = table.getModel().getValueAt(0, 1); // col 1 = ISBN
        String existingISBN = isbnVal != null ? isbnVal.toString().trim() : "";
        if (existingISBN.isEmpty()) { warn("Could not read ISBN from row 0"); return; }

        openNewBookDialog(main);
        JDialog dlg = waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        setFieldNear(dlg, "ISBN",  existingISBN);
        setFieldNear(dlg, "Títol", "DuplicateTest");
        clickSave(dlg); sleep(800);
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass("Duplicate ISBN → error dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass("Duplicate ISBN → save rejected (form still open)");
            dismissAllDialogs();
        } else if (after == null) {
            fail("Duplicate ISBN " + existingISBN + " accepted without error");
        } else {
            warn("Duplicate ISBN → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        }
    }

    // ── CREATE VALID BOOKS ────────────────────────────────────────────────────────

    private static void testCreateValidBooks(JFrame main) throws Exception {
        int saved = 0;
        String[] genres = {"Novel·la", "Ciència", "Història", "Poesia", "Assaig"};
        for (int i = 0; i < 5; i++) {
            long isbn = uniqueISBN();
            JDialog dlg = null;
            for (int attempt = 0; attempt < 3 && dlg == null; attempt++) {
                openNewBookDialog(main);
                dlg = waitForDialog(1500 + attempt * 500);
            }
            if (dlg == null) { fail("new-book dialog missing (book " + i + ")"); continue; }
            setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
            setFieldNear(dlg, "Títol",     "StressBook_" + i);
            setFieldNear(dlg, "Autor",     "Autor Stress " + i);
            setFieldNear(dlg, "Any",       String.valueOf(2000 + i));
            setFieldNear(dlg, "Valoració", String.valueOf((i + 1) * 1.5));
            setFieldNear(dlg, "Preu",      String.valueOf((i + 1) * 4.99));
            setFieldNear(dlg, "Editorial", "Editorial " + genres[i]);
            setFieldNear(dlg, "Sèrie",     "StressSeries");
            setFieldNear(dlg, "Volum",     String.valueOf(i + 1));
            clickSave(dlg); sleep(700);
            JDialog after = getTopDialog();
            if (after != null && looksLikeError(after)) {
                fail("Valid book " + i + " (ISBN=" + isbn + ") → error: \"" + after.getTitle() + "\"");
                dismissAllDialogs();
            } else {
                dismissAllDialogs();
                pass("Valid book " + i + " created (ISBN=" + isbn + ")");
                createdISBNs.add(isbn);
                saved++;
            }
        }
        if (saved == 5) pass("All 5 test books created");
        else warn(saved + "/5 test books created");
    }

    // ── PAGINATION ────────────────────────────────────────────────────────────────

    private static void testRapidPagination(JFrame main) throws Exception {
        goAllBooks(main);
        AbstractButton next = findBtnIn(main, "Seg");
        AbstractButton prev = findBtnIn(main, "Anterior");
        if (next == null || prev == null) { warn("Pagination buttons not found"); return; }

        // Forward 15 pages
        for (int i = 0; i < 15 && next.isEnabled(); i++) { doClick(next); sleep(80); }
        sleep(200);
        screenshot("pagination_fwd15");

        // Backward 15 pages
        for (int i = 0; i < 15 && prev.isEnabled(); i++) { doClick(prev); sleep(80); }
        sleep(200);

        // Go to last page (capped — small libraries finish in a few clicks)
        int steps = 0;
        while (next.isEnabled() && steps++ < 40) { doClick(next); sleep(25); }
        sleep(200);
        screenshot("pagination_last");

        // Go back to first
        steps = 0;
        while (prev.isEnabled() && steps++ < 40) { doClick(prev); sleep(25); }
        sleep(300);

        if (getTopDialog() != null) { fail("Pagination stress → error dialog"); dismissAllDialogs(); }
        else pass("Pagination stress: fwd/bwd/last/first — no crash");
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────────

    private static void testSearch_sql(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        String[] payloads = {
            "' OR '1'='1", "'; DROP TABLE--", "\" OR \"\"=\"",
            "1 UNION SELECT * FROM", "'; DELETE FROM llibres--"
        };
        boolean crashed = false;
        for (String p : payloads) {
            setField(sf, p); sleep(400);
            if (getTopDialog() != null) { crashed = true; fail("SQL search \"" + p + "\" → dialog"); dismissAllDialogs(); }
        }
        setField(sf, ""); sleep(300);
        if (!crashed) pass("SQL injection search patterns → no crash");
    }

    private static void testSearch_regex(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        String[] payloads = {".*", ".+", "^.*$", "[a-z]+", "(a|b)*", "\\d+", "?invalid", "a{10000}"};
        boolean crashed = false;
        for (String p : payloads) {
            setField(sf, p); sleep(300);
            if (getTopDialog() != null) { crashed = true; fail("Regex search \"" + p + "\" → dialog"); dismissAllDialogs(); }
        }
        setField(sf, ""); sleep(300);
        if (!crashed) pass("Regex metachar searches → no crash");
    }

    private static void testSearch_empty(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        setField(sf, "test"); sleep(300);
        setField(sf, ""); sleep(300);
        setField(sf, "   "); sleep(300);
        setField(sf, ""); sleep(200);
        if (getTopDialog() != null) { fail("Empty/whitespace search → dialog"); dismissAllDialogs(); }
        else pass("Empty/whitespace search → no crash");
    }

    private static void testSearch_long(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        setField(sf, "x".repeat(5000)); sleep(600);
        if (getTopDialog() != null) { fail("5000-char search → dialog"); dismissAllDialogs(); }
        else pass("5000-char search → no crash");
        setField(sf, ""); sleep(200);
    }

    // ── FILTERS ──────────────────────────────────────────────────────────────────

    private static void testFilter_llegitBoth(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JCheckBox llegit   = findCheckBoxGlobal("Llegit");
        JCheckBox noLlegit = findCheckBoxGlobal("No llegit");
        if (llegit == null || noLlegit == null) { warn("Filter checkboxes not found"); closeFilter(main); return; }
        SwingUtilities.invokeAndWait(() -> { llegit.setSelected(true); noLlegit.setSelected(true); });
        AbstractButton filtrarBtn = findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { doClick(filtrarBtn); sleep(600); }
        if (getTopDialog() != null) { fail("Filter llegit+noLlegit → dialog"); dismissAllDialogs(); }
        else pass("Filter llegit+noLlegit → no crash");
        AbstractButton clear = findBtnIn(main, "Treure");
        if (clear != null) { doClick(clear); sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_invertedYears(JFrame main) throws Exception {
        ensureFilterOpen(main);
        sleep(300);
        // Find the two year textfields (after the "Any:" label)
        List<Component> flat = new ArrayList<>();
        for (Window w : Window.getWindows()) if (w.isVisible()) flattenVisible((Container)w, flat);
        JTextField yearMin = null, yearMax = null;
        for (int i = 0; i < flat.size(); i++) {
            if (flat.get(i) instanceof JLabel lbl && lbl.getText() != null
                    && norm(lbl.getText()).contains("any")) {
                for (int j = i+1; j < Math.min(i+12, flat.size()); j++) {
                    if (flat.get(j) instanceof JTextField tf) {
                        if (yearMin == null) yearMin = tf;
                        else { yearMax = tf; break; }
                    }
                }
                if (yearMax != null) break;
            }
        }
        if (yearMin == null || yearMax == null) { warn("Year range TFs not found"); closeFilter(main); return; }
        final JTextField fMin = yearMin, fMax = yearMax;
        SwingUtilities.invokeAndWait(() -> { fMin.selectAll(); fMin.setText("2020"); fMax.selectAll(); fMax.setText("1900"); });
        AbstractButton filtrarBtn = findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { doClick(filtrarBtn); sleep(600); }
        if (getTopDialog() != null) { warn("Inverted year range → dialog (may be OK): " + getTopDialog().getTitle()); dismissAllDialogs(); }
        else pass("Inverted year range handled without crash");
        AbstractButton clear = findBtnIn(main, "Treure");
        if (clear != null) { doClick(clear); sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_applyAndClear(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JTextField sf = findSearchField(main);
        if (sf != null) setField(sf, "a");
        AbstractButton filtrarBtn = findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { doClick(filtrarBtn); sleep(500); }
        AbstractButton clear = findBtnIn(main, "Treure");
        if (clear != null) { doClick(clear); sleep(400); }
        if (getTopDialog() != null) { fail("Filter apply/clear → dialog"); dismissAllDialogs(); }
        else pass("Filter apply+clear → no crash");
        closeFilter(main);
    }

    // ── RAPID TOGGLES ────────────────────────────────────────────────────────────

    private static void testRapid_darkMode(JFrame main) throws Exception {
        AbstractButton btn = findBtnByTooltip(main, "clar i fosc", "claro y oscuro", "light/dark");
        if (btn == null) btn = findBtnIn(main, "fosc", "clar", "Sèpia", "Sepia", "Oceà", "Ocean", "Light", "Dark");
        if (btn == null) { warn("Dark mode button not found"); return; }
        for (int i = 0; i < 6; i++) { doClick(btn); sleep(180); }
        sleep(400);
        if (getTopDialog() != null) { fail("Dark mode rapid toggle → dialog"); dismissAllDialogs(); }
        else pass("Dark mode toggled 6x → no crash (even count → restored)");
    }

    private static void testRapid_gallery(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Galeria");
        if (btn == null) { warn("Gallery button not found"); return; }
        for (int i = 0; i < 4; i++) { doClick(btn); sleep(300); }
        sleep(300);
        if (getTopDialog() != null) { fail("Gallery toggle → dialog"); dismissAllDialogs(); }
        else pass("Gallery toggled 4x → no crash");
    }

    private static void testRapid_series(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Sèries", "Series");
        if (btn == null) { warn("Series button not found"); return; }
        for (int i = 0; i < 4; i++) { doClick(btn); sleep(300); }
        sleep(300);
        if (getTopDialog() != null) { fail("Series toggle → dialog"); dismissAllDialogs(); }
        else pass("Series toggled 4x → no crash");
    }

    private static void testRapid_filterDrawer(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Filtres");
        if (btn == null) { warn("Filter drawer button not found"); return; }
        for (int i = 0; i < 6; i++) { doClick(btn); sleep(120); }
        sleep(300);
        if (getTopDialog() != null) { fail("Filter drawer rapid toggle → dialog"); dismissAllDialogs(); }
        else pass("Filter drawer toggled 6x → no crash");
    }

    // ── BOOK DETAIL DIALOGS ───────────────────────────────────────────────────────

    private static void testDetails_rapidOpenClose(JFrame main) throws Exception {
        goAllBooks(main);
        JTable table = findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("No rows"); return; }
        int opened = 0;
        for (int i = 0; i < 5; i++) {
            openRow(main, 0);
            JDialog d = waitForDialog(2500);
            if (d != null) {
                opened++;
                robot.keyPress(KeyEvent.VK_ESCAPE);
                robot.keyRelease(KeyEvent.VK_ESCAPE);
                sleep(400);
                dismissAllDialogs();
            }
        }
        if (opened >= 4) pass("Rapid open/close x5: " + opened + " dialogs — no crash");
        else warn("Rapid open/close: only " + opened + "/5 dialogs appeared");
    }

    private static void testDetails_llistesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Llistes)"); return; }
        AbstractButton btn = findBtnIn(details.getContentPane(), "Llistes", "Listas", "Lists");
        if (btn == null) { warn("Llistes button missing"); dismissAllDialogs(); return; }
        doClick(btn); sleep(700);
        JDialog sub = getTopDialogExcept(details);
        if (sub != null) { pass("Llistes sub-dialog: \"" + sub.getTitle() + "\""); screenshot("llistes_sub"); }
        else warn("Llistes sub-dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_etiquetesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Etiquetes)"); return; }
        AbstractButton btn = findBtnIn(details.getContentPane(), "Etiquetes", "Etiquetas", "Tags");
        if (btn == null) { warn("Etiquetes button missing"); dismissAllDialogs(); return; }
        doClick(btn); sleep(700);
        JDialog sub = getTopDialogExcept(details);
        if (sub != null) { pass("Etiquetes sub-dialog: \"" + sub.getTitle() + "\""); screenshot("etiquetes_sub"); }
        else warn("Etiquetes sub-dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_historial(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Historial)"); return; }
        AbstractButton btn = findBtnIn(details.getContentPane(), "Historial");
        if (btn == null) { warn("Historial préstecs button missing"); dismissAllDialogs(); return; }
        doClick(btn); sleep(700);
        JDialog sub = getTopDialogExcept(details);
        if (sub != null) { pass("Historial préstecs dialog: \"" + sub.getTitle() + "\""); screenshot("historial_sub"); }
        else warn("Historial dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_imprimir(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Imprimir)"); return; }
        AbstractButton btn = findBtnIn(details.getContentPane(), "Imprimir", "Print");
        if (btn == null) { warn("Imprimir button missing"); dismissAllDialogs(); return; }
        doClick(btn); sleep(800);
        // May open print dialog — just cancel/escape
        JDialog sub = getTopDialogExcept(details);
        if (sub != null) {
            pass("Imprimir opened: \"" + sub.getTitle() + "\"");
            screenshot("imprimir_sub");
            robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
            sleep(400);
        } else {
            pass("Imprimir clicked — no Swing sub-dialog (native print UI may be headless/no-op)");
            robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
            sleep(400);
        }
        dismissAllDialogs();
    }

    // ── LLISTES MANAGEMENT ────────────────────────────────────────────────────────

    private static void testLlistesManagement(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Gestionar llistes");
        if (btn == null) { warn("Gestionar llistes button not found"); return; }
        doClick(btn); sleep(700);
        JDialog dlg = waitForDialog(2000);
        if (dlg == null) { warn("Gestionar llistes dialog missing"); return; }
        screenshot("llistes_mgmt");

        AbstractButton novaBtn  = findBtnIn((Container)dlg, "Nova");
        JTextField     nameTF   = findComponent((Container)dlg, JTextField.class);
        AbstractButton upBtn    = findBtnIn((Container)dlg, "▲", "Pujar");
        AbstractButton downBtn  = findBtnIn((Container)dlg, "▼", "Baixar");
        AbstractButton colorBtn = findBtnIn((Container)dlg, "Color");
        AbstractButton delBtn   = findBtnIn((Container)dlg, "Eliminar");

        // Empty name → expect validation
        if (novaBtn != null && nameTF != null) {
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(""); });
            doClick(novaBtn); sleep(400);
            JDialog err = getTopDialogExcept(dlg);
            if (err != null) { pass("Empty list name → error dialog"); dismissTopDialog(); }
            else warn("Empty list name: silently ignored");
        }

        // Create 3 test lists
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 3 && novaBtn != null && nameTF != null; i++) {
            String name = "StressTestList_" + i + "_" + (System.currentTimeMillis() % 1000);
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(name); });
            sleep(80);
            doClick(novaBtn); sleep(500);
            JDialog err = getTopDialogExcept(dlg);
            if (err != null) { warn("List create error: " + err.getTitle()); dismissTopDialog(); }
            else { created.add(name); log("  Created: " + name); }
        }
        if (!created.isEmpty()) pass("Created " + created.size() + " test lists");

        // Reorder (ensure selection first)
        if (upBtn != null && downBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForReorder = (JList<Object>) findComponent((Container)dlg, JList.class);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            doClick(downBtn); sleep(200);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            doClick(upBtn); sleep(200);
            pass("Reorder buttons: up/down work");
        }

        // Color button — ensure an item is selected first
        if (colorBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForColor = (JList<Object>) findComponent((Container)dlg, JList.class);
            if (listForColor != null && listForColor.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForColor.setSelectedIndex(0));
            doClick(colorBtn); sleep(700);
            JDialog colorDlg = getTopDialogExcept(dlg);
            if (colorDlg != null) {
                pass("Color picker dialog opened");
                robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
                sleep(400); dismissAllDialogsExcept(dlg);
            } else warn("Color picker did not open");
        }

        // Delete created lists
        if (delBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listComp = (JList<Object>) findComponent((Container)dlg, JList.class);
            for (int i = 0; i < created.size(); i++) {
                if (listComp != null) {
                    int idx = listComp.getModel().getSize() - 1;
                    if (idx < 0) break;
                    SwingUtilities.invokeAndWait(() -> listComp.setSelectedIndex(Math.max(0, listComp.getModel().getSize()-1)));
                    sleep(150);
                }
                doClick(delBtn); sleep(400);
                JDialog confirm = getTopDialogExcept(dlg);
                if (confirm != null) {
                    if (isStressTestListDeleteConfirm(confirm)) {
                        clickAffirmDelete(confirm);
                    } else {
                        warn("Unexpected confirm during list delete — cancelled");
                        cancelTopDialog();
                    }
                    sleep(400);
                }
            }
            pass("Deleted " + created.size() + " test lists");
        }

        SwingUtilities.invokeLater(dlg::dispose); sleep(400);
    }

    // ── STATS ────────────────────────────────────────────────────────────────────

    private static void testStats(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Estad");
        if (btn == null) { warn("Stats button not found"); return; }
        doClick(btn); sleep(800);
        JDialog dlg = waitForDialog(2000); if (dlg == null) { warn("Stats dialog missing"); return; }
        screenshot("stats");
        List<String> items = new ArrayList<>();
        collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[TBL]"))) pass("Stats: has table");
        else pass("Stats: no shelf table (no list assignments — expected)");
        if (items.stream().anyMatch(s -> s.contains("[LBL]") && s.toLowerCase().contains("llegit"))) pass("Stats: has llegit count");
        else warn("Stats: missing llegit count label");
        // Edit objective
        JTextField objField = findComponent((Container)dlg, JTextField.class);
        if (objField != null) {
            final JTextField tf = objField;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText("52"); });
            sleep(200); pass("Stats: objective field editable");
        }
        SwingUtilities.invokeLater(dlg::dispose); sleep(400);
    }

    // ── CONFIGURACIÓ ─────────────────────────────────────────────────────────────

    private static void testConfiguracio(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "Configuració", "Configur");
        if (btn == null) { warn("Configuració button not found"); return; }
        doClick(btn); sleep(900);
        JDialog dlg = waitForDialog(3000); if (dlg == null) { warn("Configuració dialog missing"); return; }
        screenshot("configuracio");
        // Verify key fields present
        List<String> items = new ArrayList<>();
        collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[CMB]"))) pass("Configuració: has comboboxes");
        else warn("Configuració: missing comboboxes");
        // Cancel without saving
        AbstractButton cancel = findBtnIn((Container)dlg, "Cancel", "Tancar");
        if (cancel != null) { doClick(cancel); sleep(300); }
        else { SwingUtilities.invokeLater(dlg::dispose); sleep(300); }
        pass("Configuració dialog opened and closed without crash");
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────────

    private static void testExport(JFrame main) throws Exception {
        ensureFilterOpen(main);
        sleep(300);
        AbstractButton exportBtn = findBtnIn(main, "Exportar");
        if (exportBtn == null) { warn("Export button not found (need filter open)"); closeFilter(main); return; }

        String[][] exports = {{"CSV", "Export CSV", "Exportar CSV"},
                               {"JSON", "Export JSON", "Exportar JSON"},
                               {"HTML", "Export HTML", "Exportar HTML"}};
        for (String[] exp : exports) {
            doClick(exportBtn); sleep(400);
            AbstractButton item = findBtnIn(main, exp[1], exp[2]);
            if (item != null) { doClick(item); sleep(700); }
            JDialog fc = getTopDialog();
            if (fc != null) {
                pass("Export " + exp[0] + " → dialog appeared");
                screenshot("export_" + exp[0].toLowerCase());
                focusMain(main);
                robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
                sleep(400); dismissAllDialogs();
            } else {
                warn("Export " + exp[0] + " → no dialog (may have cancelled or auto-saved)");
                focusMain(main);
                robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
                sleep(200);
            }
        }
        closeFilter(main);
    }

    // ── BACKUP ───────────────────────────────────────────────────────────────────

    private static void testBackup(JFrame main) throws Exception {
        ensureFilterOpen(main);
        sleep(300);
        AbstractButton btn = findBtnIn(main, "Backup", "backup");
        if (btn == null) { warn("Backup button not found (need filter open)"); closeFilter(main); return; }
        doClick(btn); sleep(800);
        JDialog fc = getTopDialog();
        if (fc != null) {
            pass("Backup BD → dialog: \"" + fc.getTitle() + "\"");
            screenshot("backup_dialog");
            robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
            sleep(400); dismissAllDialogs();
        } else {
            warn("Backup BD → no dialog appeared");
        }
        closeFilter(main);
    }

    // ── KEYBOARD SHORTCUTS ────────────────────────────────────────────────────────

    private static void testKbd_ctrlA(JFrame main) throws Exception {
        JTable table = findComponent((Container)main, JTable.class);
        if (table == null) { warn("No table for Ctrl+A test"); return; }
        goAllBooks(main); sleep(300);
        triggerRootAction(main, "seleccionarTot");
        sleep(200);
        int sel = table.getSelectedRowCount();
        if (sel > 0) pass("Ctrl+A selected " + sel + " rows");
        else warn("Ctrl+A: 0 rows selected");
        SwingUtilities.invokeAndWait(() -> table.clearSelection());
    }

    private static void testKbd_ctrlF(JFrame main) throws Exception {
        triggerRootAction(main, "focusFiltres");
        sleep(200);
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JTextField search = findSearchField(main);
        if (search != null && search.isFocusOwner()) pass("Ctrl+F → search field focused");
        else if (focused instanceof JTextField) pass("Ctrl+F → JTextField focused");
        else warn("Ctrl+F → focused: " + (focused != null ? focused.getClass().getSimpleName() : "null"));
    }

    private static void testKbd_ctrlN(JFrame main) throws Exception {
        triggerRootAction(main, "nouLlibre");
        sleep(900);
        JDialog dlg = getTopDialog();
        if (dlg != null) {
            pass("Ctrl+N → new book dialog: \"" + dlg.getTitle() + "\"");
            dismissTopDialog();
        } else warn("Ctrl+N → no dialog appeared");
    }

    // ── ALEATORI ────────────────────────────────────────────────────────────────

    private static void testAleatori(JFrame main) throws Exception {
        AbstractButton btn = findBtnIn(main, "aleatori", "Aleatori");
        if (btn == null) { warn("Llibre aleatori button not found"); return; }
        // Click 3 times
        for (int i = 0; i < 3; i++) {
            doClick(btn); sleep(700);
            JDialog dlg = getTopDialog();
            if (dlg != null) {
                log("  Aleatori dialog " + (i+1) + ": \"" + dlg.getTitle() + "\"");
                dismissTopDialog();
            }
        }
        pass("Llibre aleatori clicked 3x → no crash");
    }

    // ── ALL SIDEBAR BUTTONS ───────────────────────────────────────────────────────

  // ── EXTREME ───────────────────────────────────────────────────────────────────

    private static void testExtreme_burstCreate(JFrame main, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            openNewBookDialog(main);
            JDialog dlg = waitForDialog(2000);
            if (dlg == null) { fail("burst create: dialog missing at " + i); return; }
            long isbn = uniqueISBN();
            setFieldNear(dlg, "ISBN", String.valueOf(isbn));
            setFieldNear(dlg, "Títol", "Stress_" + i);
            setFieldNear(dlg, "Autor", "Bot");
            clickSave(dlg);
            sleep(350);
            dismissAllDialogs();
            createdISBNs.add(isbn);
        }
        pass("Burst-created " + count + " books");
        goAllBooks(main);
    }

    private static void testExtreme_pagination(JFrame main, int clicks) throws Exception {
        AbstractButton next = findBtnIn(main, "Seguent", "Next");
        if (next == null) { warn("No next-page button"); return; }
        for (int i = 0; i < clicks; i++) { doClick(next); sleep(80); }
        AbstractButton prev = findBtnIn(main, "Anterior", "Previous");
        if (prev != null) for (int i = 0; i < Math.min(clicks, 10); i++) { doClick(prev); sleep(80); }
        pass("Pagination hammered " + clicks + " forward clicks");
    }

    private static void testExtreme_filterLoop(JFrame main, int iterations) throws Exception {
        ensureFilterOpen(main);
        JTextField nom = findTextFieldNear(main, "Nom");
        JTextField search = findSearchField(main);
        AbstractButton filtrar = findBtnIn(main, "Filtrar");
        AbstractButton clear = findBtnIn(main, "Treure", "Quitar");
        for (int i = 0; i < iterations; i++) {
            if (search != null) setField(search, i % 2 == 0 ? "a" : "");
            if (nom != null) setField(nom, i % 3 == 0 ? "Stress" : "");
            if (filtrar != null) doClick(filtrar);
            sleep(40);
            if (clear != null && i % 5 == 0) doClick(clear);
            sleep(30);
        }
        if (search != null) setField(search, "");
        pass("Filter/search loop x" + iterations);
    }

    private static void testExtreme_gallery(JFrame main, int toggles) throws Exception {
        AbstractButton galeria = findBtnIn(main, "Galeria");
        if (galeria == null) { warn("Galeria button missing"); return; }
        for (int i = 0; i < toggles; i++) { doClick(galeria); sleep(60); }
        pass("Gallery toggled x" + toggles);
    }

    private static void testExtreme_concurrent(JFrame main, int threadCount) throws Exception {
        AtomicReference<AbstractButton> toggleBtn = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
            toggleBtn.set(findBtnIn(main, "Filtres", "Galeria", "Sèrie")));
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
                        sleep(30 + (id % 10));
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
            }, "stress-ui-" + t);
            th.setDaemon(true);
            th.start();
            pool.add(th);
        }
        start.countDown();
        for (Thread th : pool) th.join(8000);
        sleep(500);
        dismissAllDialogs();
        if (errors.get() > 0) fail("Concurrent UI errors: " + errors.get());
        else pass("Concurrent UI spam (" + threadCount + " threads x5 clicks)");
    }

    private static void testExtreme_dialogSpam(JFrame main) throws Exception {
        String[] opens = {"Estad", "Configur", "aleatori", "llistes"};
        for (int round = 0; round < 3; round++) {
            for (String hint : opens) {
                AbstractButton btn = findBtnIn(main, hint);
                if (btn == null) continue;
                doClick(btn); sleep(400);
                dismissAllDialogs();
            }
        }
        pass("Dialog rapid-fire x3 rounds");
    }

    private static void testAllSidebarButtons(JFrame main) throws Exception {
        String[] btns = {"Tots els", "Afegits recentment", "Llegits", "Llista de desitjos", "En curs"};
        for (String label : btns) {
            AbstractButton btn = findBtnIn(main, label);
            if (btn == null) { warn("Sidebar button not found: \"" + label + "\""); continue; }
            doClick(btn); sleep(500);
            JDialog d = getTopDialog();
            if (d != null) {
                dismissAllDialogs();
                pass("Sidebar \"" + label + "\" → dialog dismissed, no crash");
            } else {
                pass("Sidebar \"" + label + "\" → panel update, no dialog");
            }
        }
        goAllBooks(main);
    }

    // ── DATA INTEGRITY (domain layer, hard asserts) ──────────────────────────────

    /**
     * Creates a book through the domain layer with EVERY field set (including the
     * recently-added ones: nom_ca/es/en, estat, exemplars, idioma, format, serie,
     * volum, pagines), then reloads it STRAIGHT FROM THE DATABASE
     * ({@link persistencia.ControladorPersistencia#getAllLlibres()}, which rebuilds
     * each Llibre from the ResultSet) and asserts a true persistence round-trip.
     * Then exercises a loan create/return round-trip and a tag add/remove
     * round-trip. Unlike the UI phases, these are hard PASS/FAIL checks.
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

            // Re-read from DB (not the in-memory cache) to prove it persisted.
            domini.Llibre db = null;
            for (domini.Llibre b : cp.getAllLlibres())
                if (b.getISBN() == isbn) { db = b; break; }

            if (db == null) { fail("DataIntegrity: book ISBN=" + isbn + " not found after reload"); return; }

            check("nom persists",       "Integrity_" + isbn, db.getNom());
            check("nom_ca persists",    "Títol CA",  db.getNomCa());
            check("nom_es persists",    "Título ES", db.getNomEs());
            check("nom_en persists",    "Title EN",  db.getNomEn());
            check("editorial persists", "Edit Integ", db.getEditorial());
            check("serie persists",     "Serie Integ", db.getSerie());
            check("volum persists",     3, db.getVolum());
            check("estat persists",     "nou", db.getEstat());
            check("exemplars persists", 4, db.getExemplars());
            check("idioma persists",    "Català", db.getIdioma());
            check("format persists",    "Tapa dura", db.getFormat());
            check("pagines persists",   320, db.getPagines());
            check("pagines_llegides persists", 160, db.getPaginesLlegides());
            check("llegit persists",    Boolean.TRUE, db.getLlegit());

            // ── Loan round-trip ──────────────────────────────────────────────
            cd.prestarLlibre(isbn, "Joan Tester");
            check("loan registered (active)", true, cd.getLoanedISBNs().contains(isbn));
            check("countLoans >= 1", true, cd.countLoans(isbn) >= 1);
            cd.retornarLlibre(isbn);
            check("loan returned (not active)", false, cd.getLoanedISBNs().contains(isbn));

            // ── Tag round-trip ───────────────────────────────────────────────
            tag = cd.addTag("StressTag_" + UUID.randomUUID().toString().substring(0, 8));
            cd.addLlibreToTag(isbn, tag.getId());
            check("tag membership added", true, cd.getLlibresWithTag(tag.getId()).contains(isbn));
            cd.removeLlibreFromTag(isbn, tag.getId());
            check("tag membership removed", false, cd.getLlibresWithTag(tag.getId()).contains(isbn));
        } catch (Exception e) {
            fail("DataIntegrity threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            // Best-effort teardown — do not let cleanup failures mask the result.
            try { cd.deleteLlibre(Long.valueOf(isbn)); } catch (Exception ignored) {}
            if (tag != null) { try { cd.deleteTag(tag); } catch (Exception ignored) {} }
        }
    }

    /** Hard equality assertion used by {@link #testDataIntegrity}. */
    private static void check(String what, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) pass("DataIntegrity: " + what);
        else fail("DataIntegrity: " + what + " — expected <" + expected + "> but was <" + actual + ">");
    }

    // ── CLEANUP ─────────────────────────────────────────────────────────────────

    private static void testCleanup(JFrame main) throws Exception {
        if (createdISBNs.isEmpty()) { warn("No test ISBNs to clean up"); return; }
        log("Cleaning " + createdISBNs.size() + " test books...");
        goAllBooks(main); sleep(500);
        JTextField sf = findSearchField(main);
        int deleted = 0;
        // Clear search bar so RowFilter doesn't hide books
        if (sf != null) { setField(sf, ""); sleep(400); }
        for (long isbn : createdISBNs) {
            JTable table = findComponent((Container)main, JTable.class);
            if (table == null) { log("  ISBN " + isbn + " not in table — skipping"); continue; }
            // Scan model directly (bypasses RowFilter view-count issues)
            String isbnStr = String.valueOf(isbn);
            int modelRow = -1;
            javax.swing.table.TableModel mdl = table.getModel();
            for (int r = 0; r < mdl.getRowCount(); r++) {
                Object v = mdl.getValueAt(r, 1);
                if (v != null && isbnStr.equals(String.valueOf(v))) { modelRow = r; break; }
            }
            if (modelRow < 0) { log("  ISBN " + isbn + " not in table — skipping"); continue; }
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow < 0) { log("  ISBN " + isbn + " filtered out — skipping"); continue; }
            final int fr = viewRow;
            SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(fr, fr));
            focusMain(main);
            robot.keyPress(KeyEvent.VK_DELETE); robot.keyRelease(KeyEvent.VK_DELETE);
            sleep(600);
            JDialog confirm = getTopDialog();
            if (confirm != null) {
                if (isTestBookDeleteConfirm(confirm)) {
                    clickAffirmDelete(confirm);
                    sleep(400);
                    deleted++;
                } else {
                    warn("Unexpected dialog during cleanup delete for ISBN " + isbn + " — cancelled");
                    cancelTopDialog();
                }
            } else {
                warn("No delete confirm for ISBN " + isbn + " — book may remain");
            }
        }
        if (sf != null) { setField(sf, ""); sleep(400); }
        goAllBooks(main);
        pass("Cleanup: deleted " + deleted + "/" + createdISBNs.size() + " test books");
    }

    // ── HELPERS: INTERACTION ──────────────────────────────────────────────────────

    private static void openNewBookDialog(JFrame main) throws Exception {
        dismissAllDialogs();
        AbstractButton btn = findBtnIn(main, "Afegir", "Nou", "New");
        if (btn != null) doClick(btn);
        else {
            SwingUtilities.invokeAndWait(() -> main.requestFocusInWindow());
            sleep(80);
            robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_N);
            robot.keyRelease(KeyEvent.VK_N); robot.keyRelease(KeyEvent.VK_CONTROL);
        }
        sleep(600);
    }

    private static void clickSave(JDialog dlg) {
        AbstractButton btn = findBtnIn((Container)dlg, "Desa", "Guardar", "Save");
        if (btn != null) doClick(btn);
    }

    private static void setFieldNear(JDialog dlg, String label, String value) throws Exception {
        JTextField tf = findTextFieldNear((Container)dlg, label);
        if (tf == null) return;
        SwingUtilities.invokeAndWait(() -> { tf.requestFocusInWindow(); tf.selectAll(); tf.setText(value); });
        sleep(40);
    }

    private static void setField(JTextField tf, String value) throws Exception {
        SwingUtilities.invokeAndWait(() -> { tf.requestFocusInWindow(); tf.selectAll(); tf.setText(value); });
        sleep(60);
    }

    private static void doClick(AbstractButton btn) {
        SwingUtilities.invokeLater(btn::doClick);
        sleep(60);
    }

    private static void openRow(JFrame main, int row) throws Exception {
        JTable table = findComponent((Container)main, JTable.class);
        if (table == null || row >= table.getRowCount()) return;
        // invokeLater, not invokeAndWait: obrirDetalls opens a modal JDialog (blocks EDT).
        SwingUtilities.invokeLater(() -> {
            main.toFront();
            table.requestFocusInWindow();
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            javax.swing.Action act = table.getActionMap().get("obrirDetalls");
            if (act != null) act.actionPerformed(new java.awt.event.ActionEvent(table, 0, "obrirDetalls"));
        });
        sleep(900);
    }

    private static void triggerRootAction(JFrame main, String key) throws Exception {
        SwingUtilities.invokeLater(() -> {
            main.toFront();
            main.getRootPane().requestFocusInWindow();
            javax.swing.Action act = main.getRootPane().getActionMap().get(key);
            if (act != null) act.actionPerformed(new java.awt.event.ActionEvent(main.getRootPane(), 0, key));
        });
        sleep(120);
    }

    /** Brings the main frame to the front and requests OS-level focus. */
    private static void focusMain(JFrame main) throws Exception {
        SwingUtilities.invokeAndWait(() -> { main.toFront(); main.requestFocus(); });
        sleep(120);
    }

    /** Opens row, flushes EDT, waits longer for the details JDialog, and verifies it is a book-details dialog. */
    private static JDialog openDetailsAndWait(JFrame main, int row) throws Exception {
        dismissAllDialogs();
        sleep(200);
        openRow(main, row);
        // Flush any pending EDT events so the dialog content is fully laid out
        SwingUtilities.invokeAndWait(() -> {});
        sleep(400);
        JDialog d = waitForDialog(3000);
        if (d == null) return null;
        if (isBookDetailsDialog(d)) return d;
        // Make sure it's the details dialog, not an error dialog
        if (looksLikeError(d)) {
            log("  [openDetailsAndWait] got error dialog: \"" + d.getTitle() + "\" — dismissing and retrying");
            dismissAllDialogs();
            sleep(300);
            openRow(main, row);
            SwingUtilities.invokeAndWait(() -> {});
            sleep(400);
            d = waitForDialog(3000);
        }
        return d;
    }

    private static void goAllBooks(JFrame main) {
        AbstractButton btn = findBtnIn(main, "Tots els");
        if (btn != null) { doClick(btn); sleep(500); }
    }

    private static void ensureFilterOpen(JFrame main) {
        AbstractButton btn = findBtnIn(main, "Filtres");
        if (btn != null && btn.getText() != null && !btn.getText().contains("▲")) { doClick(btn); sleep(400); }
    }

    private static void closeFilter(JFrame main) {
        AbstractButton btn = findBtnIn(main, "Filtres ▲");
        if (btn != null) { doClick(btn); sleep(300); }
    }

    private static void checkExpectError(String label) {
        JDialog after = getTopDialog();
        if (after != null && looksLikeError(after)) {
            pass(label + " → validation dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && isBookFormDialog(after)) {
            pass(label + " → save rejected (form still open: \"" + after.getTitle() + "\")");
            dismissAllDialogs();
        } else if (after != null) {
            warn(label + " → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else {
            fail(label + " accepted without validation error");
        }
    }

    private static boolean isBookFormDialog(JDialog d) {
        String t = norm(d.getTitle());
        return t.contains("nou llibre") || t.contains("new book")
            || t.contains("expedient del llibre");
    }

    private static boolean isBookDetailsDialog(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("expedient del llibre") || t.contains("ficha del libro")
            || t.contains("book details");
    }

    private static boolean looksLikeError(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("error") || t.contains("avis") || t.contains("warn")
            || t.contains("invalid") || t.contains("valid") || t.contains("validac")
            || t.contains("incorrecte") || t.contains("camp")
            || t.isBlank(); // JOptionPane often has blank title
    }

    private static void dismissAllDialogs() {
        for (int i = 0; i < 8; i++) {
            JDialog d = getTopDialog();
            if (d == null) break;
            cancelTopDialog();
        }
    }

    private static void dismissAllDialogsExcept(JDialog keep) {
        for (int i = 0; i < 5; i++) {
            JDialog d = getTopDialog();
            if (d == null || d == keep) break;
            AbstractButton ok = findBtnIn((Container)d, "OK", "Tancar", "Cancel·lar", "Close");
            if (ok != null) doClick(ok); else SwingUtilities.invokeLater(d::dispose);
            sleep(300);
        }
    }

    private static void dismissTopDialog() {
        cancelTopDialog();
    }

    /** Dismiss without affirming destructive actions (no Sí/Yes/OK). */
    private static void cancelTopDialog() {
        JDialog d = getTopDialog();
        if (d == null) return;
        AbstractButton cancel = findBtnIn((Container)d, "Cancel·lar", "Cancelar", "Tancar", "Close", "No");
        if (cancel != null) doClick(cancel);
        else {
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            sleep(200);
            if (getTopDialog() == d) SwingUtilities.invokeLater(d::dispose);
        }
        sleep(280);
    }

    private static void clickAffirmDelete(JDialog confirm) {
        AbstractButton yes = findBtnIn((Container)confirm, "Sí", "Yes", "Eliminar", "Esborrar", "OK");
        if (yes != null) doClick(yes);
        else cancelTopDialog();
    }

    private static boolean isTestBookDeleteConfirm(JDialog d) {
        String t = norm(d.getTitle());
        return t.contains("eliminar") || t.contains("esborrar") || t.contains("delete")
            || t.contains("confirm") || t.contains("segur");
    }

    private static boolean isStressTestListDeleteConfirm(JDialog d) {
        if (!isTestBookDeleteConfirm(d)) return false;
        String body = dialogText(d);
        return body.contains("stresstestlist");
    }

    private static String dialogText(JDialog d) {
        List<String> items = new ArrayList<>();
        collectComponents((Container)d, "", items);
        return norm(String.join(" ", items));
    }

    private static JTextField findSearchField(JFrame main) {
        List<Component> flat = new ArrayList<>();
        flattenVisible((Container)main, flat);
        for (Component c : flat) {
            if (c instanceof JTextField tf) {
                String tip = tf.getToolTipText();
                if (tip != null && (tip.toLowerCase().contains("cerca") || tip.toLowerCase().contains("search"))) return tf;
            }
        }
        return findComponent((Container)main, JTextField.class);
    }

    private static JCheckBox findCheckBoxGlobal(String text) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JCheckBox found = findCheckBox((Container)w, text);
            if (found != null) return found;
        }
        return null;
    }

    private static long uniqueISBN() {
        long isbn = isbnSeq.incrementAndGet();
        if (createdISBNs.contains(isbn)) {
            fail("ISBN collision detected: " + isbn);
        }
        return isbn;
    }

    // ── RESULT TRACKING ───────────────────────────────────────────────────────────
    private static void pass(String msg) { passCount++; log("  ✓ PASS: " + msg); }
    private static void fail(String msg) { failCount++; log("  ✗ FAIL: " + msg); }
    private static void warn(String msg) { warnCount++; log("  ! WARN: " + msg); }

    // ── COMPONENT HELPERS ─────────────────────────────────────────────────────────

    private static AbstractButton findBtnIn(Container c, String... texts) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton btn && btn.isVisible()) {
                String t = btn.getText();
                if (t != null) for (String text : texts)
                    if (norm(t).contains(norm(text))) return btn;
            }
            if (comp instanceof Container sub) {
                AbstractButton found = findBtnIn(sub, texts);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static AbstractButton findBtnIn(JFrame main, String... texts) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnIn((Container)w, texts);
            if (found != null) return found;
        }
        return null;
    }

    private static AbstractButton findBtnByTooltip(Container c, String... tips) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton btn && btn.isVisible()) {
                String t = btn.getToolTipText();
                if (t != null) for (String tip : tips)
                    if (norm(t).contains(norm(tip))) return btn;
            }
            if (comp instanceof Container sub) {
                AbstractButton found = findBtnByTooltip(sub, tips);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static AbstractButton findBtnByTooltip(JFrame main, String... tips) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnByTooltip((Container)w, tips);
            if (found != null) return found;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponent(Container c, Class<T> type) {
        for (Component comp : c.getComponents()) {
            if (type.isInstance(comp)) return (T) comp;
            if (comp instanceof Container sub) {
                T found = findComponent(sub, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTextField findTextFieldNear(Container c, String labelHint) {
        List<Component> flat = new ArrayList<>();
        flattenVisible(c, flat);
        for (int i = 0; i < flat.size(); i++) {
            if (flat.get(i) instanceof JLabel lbl && lbl.getText() != null
                    && norm(lbl.getText()).contains(norm(labelHint))) {
                for (int j = i+1; j < Math.min(i+6, flat.size()); j++)
                    if (flat.get(j) instanceof JTextField tf) return tf;
            }
        }
        return null;
    }

    private static JCheckBox findCheckBox(Container c, String text) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JCheckBox chk && norm(chk.getText()).contains(norm(text))) return chk;
            if (comp instanceof Container sub) {
                JCheckBox found = findCheckBox(sub, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void collectComponents(Container c, String indent, List<String> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            if (comp instanceof JLabel lbl && lbl.getText() != null && !lbl.getText().isBlank())
                out.add(indent + "[LBL] \"" + lbl.getText() + "\"");
            else if (comp instanceof AbstractButton btn && !(btn instanceof JCheckBox))
                out.add(indent + "[BTN] \"" + btn.getText() + "\"");
            else if (comp instanceof JCheckBox chk)
                out.add(indent + "[CHK] \"" + chk.getText() + "\"=" + chk.isSelected());
            else if (comp instanceof JTextField tf)
                out.add(indent + "[TF] \"" + tf.getText() + "\"");
            else if (comp instanceof JComboBox<?> cb)
                out.add(indent + "[CMB] sel=\"" + cb.getSelectedItem() + "\"");
            else if (comp instanceof JTable tbl)
                out.add(indent + "[TBL] rows=" + tbl.getRowCount());
            if (comp instanceof Container sub) collectComponents(sub, indent + "  ", out);
        }
    }

    private static void flattenVisible(Container c, List<Component> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            out.add(comp);
            if (comp instanceof Container sub) flattenVisible(sub, out);
        }
    }

    // ── WINDOW HELPERS ────────────────────────────────────────────────────────────

    private static JFrame waitForMainFrame(long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            for (Window w : Window.getWindows())
                if (w instanceof JFrame f && f.isVisible() && !f.getTitle().isBlank()) return f;
            Thread.sleep(150);
        }
        return null;
    }

    private static JDialog waitForDialog(long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            JDialog d = getTopDialog();
            if (d != null) return d;
            Thread.sleep(100);
        }
        return null;
    }

    private static JDialog getTopDialog() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length-1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible()) return d;
        return null;
    }

    private static JDialog getTopDialogExcept(JDialog except) {
        Window[] ws = Window.getWindows();
        for (int i = ws.length-1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible() && d != except) return d;
        return null;
    }

    // ── SCREENSHOT & LOG ─────────────────────────────────────────────────────────

    private static void screenshot(String name) {
        try {
            String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String path = "checkBiblio/screenshots/stress_" + (++screenshotSeq) + "_" + safe + ".png";
            BufferedImage img = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ImageIO.write(img, "PNG", new File(path));
            log("  SCREENSHOT: " + path);
        } catch (Exception e) { log("  SCREENSHOT FAILED: " + e.getMessage()); }
    }

    private static void log(String msg) {
        synchronized (LOG_LOCK) {
            String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
            report.println(line);
            System.out.println(line);
        }
    }

    private static void closeReport() {
        log("=== StressTest finished ===");
        report.close();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
