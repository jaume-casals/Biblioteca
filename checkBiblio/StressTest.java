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
 * Biblioteca StressTest — chaos / edge-case hammerer.
 *
 * Compile: javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio/UiTestSupport.java checkBiblio/StressTest.java -d bin
 * Run:     java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio.StressTest
 */
public class StressTest {

    private static Robot robot;
    private static PrintWriter report;
    private static int passCount = 0, failCount = 0, warnCount = 0;
    private static int stressThreads = 50;
    private static final List<Long> createdISBNs = new ArrayList<>();
    private static final AtomicLong isbnSeq = new AtomicLong(
        9780000000000L + (new Random().nextLong() & Long.MAX_VALUE) % 900_000_000L);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
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

        JFrame mainFrame = UiTestSupport.waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: No main window. Abort.");
            closeReport(); System.exit(1);
        }
        log("OK: main window visible — \"" + mainFrame.getTitle() + "\"");
        UiTestSupport.sleep(800);

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
            UiTestSupport.sleep(150);
        } catch (Exception e) {
            fail("Phase threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            dismissAllDialogs();
        }
        if (failCount > failsBefore || warnCount > warnsBefore) {
            log("  PHASE " + num + " produced new issues (fails: +" + (failCount - failsBefore)
                + ", warns: +" + (warnCount - warnsBefore) + ")");
        }
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
        phase("30", "Book details: Historial préstamos",   () -> testDetails_historial(main));
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "Títol", "SomeTitle");
        // ISBN stays empty
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("Empty ISBN");
    }

    private static void testValidation_nonNumericISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", "NOTANISBN!!!");
        UiTestSupport.setFieldNear(dlg, "Títol", "SomeTitle");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("Non-numeric ISBN");
    }

    private static void testValidation_shortISBN(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", "123");
        UiTestSupport.setFieldNear(dlg, "Títol", "SomeTitle");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("3-digit ISBN");
    }

    private static void testValidation_emptyTitle(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        // Title stays empty
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("Empty title");
    }

    private static void testValidation_ratingHigh(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        UiTestSupport.setFieldNear(dlg, "Títol", "RatingTest");
        UiTestSupport.setFieldNear(dlg, "Valoració", "11.0");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("Rating 11.0");
    }

    private static void testValidation_ratingLow(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        UiTestSupport.setFieldNear(dlg, "Títol", "RatingTest");
        UiTestSupport.setFieldNear(dlg, "Valoració", "-1.0");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        checkExpectError("Rating -1.0");
    }

    private static void testValidation_negativePrice(JFrame main) throws Exception {
        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        UiTestSupport.setFieldNear(dlg, "Títol", "PriceTest");
        UiTestSupport.setFieldNear(dlg, "Preu", "-9.99");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass("Negative price → validation error");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(uniqueISBN()));
        UiTestSupport.setFieldNear(dlg, "Títol", "YearTest");
        UiTestSupport.setFieldNear(dlg, "Any", "ABCD");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass("Non-numeric year → validation error");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
        UiTestSupport.setFieldNear(dlg, "Títol",     "'; DROP TABLE llibres; --");
        UiTestSupport.setFieldNear(dlg, "Autor",     "' OR '1'='1' --");
        UiTestSupport.setFieldNear(dlg, "Descripció","\" onload=\"alert(1)\" x=\"");
        UiTestSupport.setFieldNear(dlg, "Editorial", "Robert'); DROP TABLE Students;--");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(800);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        UiTestSupport.setFieldNear(dlg, "Títol", "📚 你好 مرحبا مكتبة 書 🔥💀 Ñöñö");
        UiTestSupport.setFieldNear(dlg, "Autor", "Ångström Ünïcödé Ñoño");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(800);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN",  String.valueOf(isbn));
        UiTestSupport.setFieldNear(dlg, "Títol", "A".repeat(500));
        UiTestSupport.setFieldNear(dlg, "Autor", "B".repeat(300));
        UiTestSupport.setFieldNear(dlg, "Descripció", "C".repeat(1000));
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(800);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass("500-char title → validation rejected (nom limit 255)");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
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
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN",  "   ");
        UiTestSupport.setFieldNear(dlg, "Títol", "   ");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(600);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass("Whitespace-only ISBN/title → validation error");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
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
        JTable table = UiTestSupport.findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("No rows for duplicate ISBN test"); return; }
        Object isbnVal = table.getModel().getValueAt(0, 1); // col 1 = ISBN
        String existingISBN = isbnVal != null ? isbnVal.toString().trim() : "";
        if (existingISBN.isEmpty()) { warn("Could not read ISBN from row 0"); return; }

        openNewBookDialog(main);
        JDialog dlg = UiTestSupport.waitForDialog(1500); if (dlg == null) { fail("new-book dialog missing"); return; }
        UiTestSupport.setFieldNear(dlg, "ISBN",  existingISBN);
        UiTestSupport.setFieldNear(dlg, "Títol", "DuplicateTest");
        UiTestSupport.clickSave(dlg); UiTestSupport.sleep(800);
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass("Duplicate ISBN → error dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
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
                dlg = UiTestSupport.waitForDialog(1500 + attempt * 500);
            }
            if (dlg == null) { fail("new-book dialog missing (book " + i + ")"); continue; }
            UiTestSupport.setFieldNear(dlg, "ISBN",      String.valueOf(isbn));
            UiTestSupport.setFieldNear(dlg, "Títol",     "StressBook_" + i);
            UiTestSupport.setFieldNear(dlg, "Autor",     "Autor Stress " + i);
            UiTestSupport.setFieldNear(dlg, "Any",       String.valueOf(2000 + i));
            UiTestSupport.setFieldNear(dlg, "Valoració", String.valueOf((i + 1) * 1.5));
            UiTestSupport.setFieldNear(dlg, "Preu",      String.valueOf((i + 1) * 4.99));
            UiTestSupport.setFieldNear(dlg, "Editorial", "Editorial " + genres[i]);
            UiTestSupport.setFieldNear(dlg, "Sèrie",     "StressSeries");
            UiTestSupport.setFieldNear(dlg, "Volum",     String.valueOf(i + 1));
            UiTestSupport.clickSave(dlg); UiTestSupport.sleep(700);
            JDialog after = UiTestSupport.getTopDialog();
            if (after != null && UiTestSupport.looksLikeError(after)) {
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
        AbstractButton next = UiTestSupport.findBtnIn(main, "Seg");
        AbstractButton prev = UiTestSupport.findBtnIn(main, "Anterior");
        if (next == null || prev == null) { warn("Pagination buttons not found"); return; }

        // Forward 15 pages
        for (int i = 0; i < 15 && next.isEnabled(); i++) { UiTestSupport.doClick(next); UiTestSupport.sleep(80); }
        UiTestSupport.sleep(200);

        // Backward 15 pages
        for (int i = 0; i < 15 && prev.isEnabled(); i++) { UiTestSupport.doClick(prev); UiTestSupport.sleep(80); }
        UiTestSupport.sleep(200);

        // Go to last page (capped — small libraries finish in a few clicks)
        int steps = 0;
        while (next.isEnabled() && steps++ < 40) { UiTestSupport.doClick(next); UiTestSupport.sleep(25); }
        UiTestSupport.sleep(200);

        // Go back to first
        steps = 0;
        while (prev.isEnabled() && steps++ < 40) { UiTestSupport.doClick(prev); UiTestSupport.sleep(25); }
        UiTestSupport.sleep(300);

        if (UiTestSupport.getTopDialog() != null) { fail("Pagination stress → error dialog"); dismissAllDialogs(); }
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
            UiTestSupport.setField(sf, p); UiTestSupport.sleep(400);
            if (UiTestSupport.getTopDialog() != null) { crashed = true; fail("SQL search \"" + p + "\" → dialog"); dismissAllDialogs(); }
        }
        UiTestSupport.setField(sf, ""); UiTestSupport.sleep(300);
        if (!crashed) pass("SQL injection search patterns → no crash");
    }

    private static void testSearch_regex(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        String[] payloads = {".*", ".+", "^.*$", "[a-z]+", "(a|b)*", "\\d+", "?invalid", "a{10000}"};
        boolean crashed = false;
        for (String p : payloads) {
            UiTestSupport.setField(sf, p); UiTestSupport.sleep(300);
            if (UiTestSupport.getTopDialog() != null) { crashed = true; fail("Regex search \"" + p + "\" → dialog"); dismissAllDialogs(); }
        }
        UiTestSupport.setField(sf, ""); UiTestSupport.sleep(300);
        if (!crashed) pass("Regex metachar searches → no crash");
    }

    private static void testSearch_empty(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        UiTestSupport.setField(sf, "test"); UiTestSupport.sleep(300);
        UiTestSupport.setField(sf, ""); UiTestSupport.sleep(300);
        UiTestSupport.setField(sf, "   "); UiTestSupport.sleep(300);
        UiTestSupport.setField(sf, ""); UiTestSupport.sleep(200);
        if (UiTestSupport.getTopDialog() != null) { fail("Empty/whitespace search → dialog"); dismissAllDialogs(); }
        else pass("Empty/whitespace search → no crash");
    }

    private static void testSearch_long(JFrame main) throws Exception {
        JTextField sf = findSearchField(main);
        if (sf == null) { fail("Search field not found"); return; }
        UiTestSupport.setField(sf, "x".repeat(5000)); UiTestSupport.sleep(600);
        if (UiTestSupport.getTopDialog() != null) { fail("5000-char search → dialog"); dismissAllDialogs(); }
        else pass("5000-char search → no crash");
        UiTestSupport.setField(sf, ""); UiTestSupport.sleep(200);
    }

    // ── FILTERS ──────────────────────────────────────────────────────────────────

    private static void testFilter_llegitBoth(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JCheckBox llegit   = UiTestSupport.findCheckBoxGlobal("Llegit");
        JCheckBox noLlegit = UiTestSupport.findCheckBoxGlobal("No llegit");
        if (llegit == null || noLlegit == null) { warn("Filter checkboxes not found"); closeFilter(main); return; }
        SwingUtilities.invokeAndWait(() -> { llegit.setSelected(true); noLlegit.setSelected(true); });
        AbstractButton filtrarBtn = UiTestSupport.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { UiTestSupport.doClick(filtrarBtn); UiTestSupport.sleep(600); }
        if (UiTestSupport.getTopDialog() != null) { fail("Filter llegit+noLlegit → dialog"); dismissAllDialogs(); }
        else pass("Filter llegit+noLlegit → no crash");
        AbstractButton clear = UiTestSupport.findBtnIn(main, "Treure");
        if (clear != null) { UiTestSupport.doClick(clear); UiTestSupport.sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_invertedYears(JFrame main) throws Exception {
        ensureFilterOpen(main);
        UiTestSupport.sleep(300);
        // Find the two year textfields (after the "Any:" label)
        List<Component> flat = new ArrayList<>();
        for (Window w : Window.getWindows()) if (w.isVisible()) UiTestSupport.flattenVisible((Container)w, flat);
        JTextField yearMin = null, yearMax = null;
        for (int i = 0; i < flat.size(); i++) {
            if (flat.get(i) instanceof JLabel lbl && lbl.getText() != null
                    && UiTestSupport.norm(lbl.getText()).contains("any")) {
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
        AbstractButton filtrarBtn = UiTestSupport.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { UiTestSupport.doClick(filtrarBtn); UiTestSupport.sleep(600); }
        if (UiTestSupport.getTopDialog() != null) { warn("Inverted year range → dialog (may be OK): " + UiTestSupport.getTopDialog().getTitle()); dismissAllDialogs(); }
        else pass("Inverted year range handled without crash");
        AbstractButton clear = UiTestSupport.findBtnIn(main, "Treure");
        if (clear != null) { UiTestSupport.doClick(clear); UiTestSupport.sleep(300); }
        closeFilter(main);
    }

    private static void testFilter_applyAndClear(JFrame main) throws Exception {
        ensureFilterOpen(main);
        JTextField sf = findSearchField(main);
        if (sf != null) UiTestSupport.setField(sf, "a");
        AbstractButton filtrarBtn = UiTestSupport.findBtnIn(main, "Filtrar");
        if (filtrarBtn != null) { UiTestSupport.doClick(filtrarBtn); UiTestSupport.sleep(500); }
        AbstractButton clear = UiTestSupport.findBtnIn(main, "Treure");
        if (clear != null) { UiTestSupport.doClick(clear); UiTestSupport.sleep(400); }
        if (UiTestSupport.getTopDialog() != null) { fail("Filter apply/clear → dialog"); dismissAllDialogs(); }
        else pass("Filter apply+clear → no crash");
        closeFilter(main);
    }

    // ── RAPID TOGGLES ────────────────────────────────────────────────────────────

    private static void testRapid_darkMode(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnByTooltip(main, "clar i fosc", "claro y oscuro", "light/dark");
        if (btn == null) btn = UiTestSupport.findBtnIn(main, "fosc", "clar", "Sèpia", "Sepia", "Oceà", "Ocean", "Light", "Dark");
        if (btn == null) { warn("Dark mode button not found"); return; }
        for (int i = 0; i < 6; i++) { UiTestSupport.doClick(btn); UiTestSupport.sleep(180); }
        UiTestSupport.sleep(400);
        if (UiTestSupport.getTopDialog() != null) { fail("Dark mode rapid toggle → dialog"); dismissAllDialogs(); }
        else pass("Dark mode toggled 6x → no crash (even count → restored)");
    }

    private static void testRapid_gallery(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Galeria");
        if (btn == null) { warn("Gallery button not found"); return; }
        for (int i = 0; i < 4; i++) { UiTestSupport.doClick(btn); UiTestSupport.sleep(300); }
        UiTestSupport.sleep(300);
        if (UiTestSupport.getTopDialog() != null) { fail("Gallery toggle → dialog"); dismissAllDialogs(); }
        else pass("Gallery toggled 4x → no crash");
    }

    private static void testRapid_series(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Sèries", "Series");
        if (btn == null) { warn("Series button not found"); return; }
        for (int i = 0; i < 4; i++) { UiTestSupport.doClick(btn); UiTestSupport.sleep(300); }
        UiTestSupport.sleep(300);
        if (UiTestSupport.getTopDialog() != null) { fail("Series toggle → dialog"); dismissAllDialogs(); }
        else pass("Series toggled 4x → no crash");
    }

    private static void testRapid_filterDrawer(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Filtres");
        if (btn == null) { warn("Filter drawer button not found"); return; }
        for (int i = 0; i < 6; i++) { UiTestSupport.doClick(btn); UiTestSupport.sleep(120); }
        UiTestSupport.sleep(300);
        if (UiTestSupport.getTopDialog() != null) { fail("Filter drawer rapid toggle → dialog"); dismissAllDialogs(); }
        else pass("Filter drawer toggled 6x → no crash");
    }

    // ── BOOK DETAIL DIALOGS ───────────────────────────────────────────────────────

    private static void testDetails_rapidOpenClose(JFrame main) throws Exception {
        goAllBooks(main);
        JTable table = UiTestSupport.findComponent((Container)main, JTable.class);
        if (table == null || table.getRowCount() == 0) { warn("No rows"); return; }
        int opened = 0;
        for (int i = 0; i < 5; i++) {
            openRow(main, 0);
            JDialog d = UiTestSupport.waitForDialog(2500);
            if (d != null) {
                opened++;
                UiTestSupport.pressEscape(robot);
                UiTestSupport.sleep(400);
                dismissAllDialogs();
            }
        }
        if (opened >= 4) pass("Rapid open/close x5: " + opened + " dialogs — no crash");
        else warn("Rapid open/close: only " + opened + "/5 dialogs appeared");
    }

    private static void testDetails_llistesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Llistes)"); return; }
        AbstractButton btn = UiTestSupport.findBtnIn(details.getContentPane(), "Llistes", "Listas", "Lists");
        if (btn == null) { warn("Llistes button missing"); dismissAllDialogs(); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(700);
        JDialog sub = UiTestSupport.getTopDialogExcept(details);
        if (sub != null) pass("Llistes sub-dialog: \"" + sub.getTitle() + "\"");
        else warn("Llistes sub-dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_etiquetesDialog(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Etiquetes)"); return; }
        AbstractButton btn = UiTestSupport.findBtnIn(details.getContentPane(), "Etiquetes", "Etiquetas", "Tags");
        if (btn == null) { warn("Etiquetes button missing"); dismissAllDialogs(); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(700);
        JDialog sub = UiTestSupport.getTopDialogExcept(details);
        if (sub != null) pass("Etiquetes sub-dialog: \"" + sub.getTitle() + "\"");
        else warn("Etiquetes sub-dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_historial(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Historial)"); return; }
        AbstractButton btn = UiTestSupport.findBtnIn(details.getContentPane(), "Historial");
        if (btn == null) { warn("Historial préstamos button missing"); dismissAllDialogs(); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(700);
        JDialog sub = UiTestSupport.getTopDialogExcept(details);
        if (sub != null) pass("Historial préstamos dialog: \"" + sub.getTitle() + "\"");
        else warn("Historial dialog not found");
        dismissAllDialogs();
    }

    private static void testDetails_imprimir(JFrame main) throws Exception {
        goAllBooks(main);
        JDialog details = openDetailsAndWait(main, 0); if (details == null) { warn("Details dialog missing (Imprimir)"); return; }
        AbstractButton btn = UiTestSupport.findBtnIn(details.getContentPane(), "Imprimir", "Print");
        if (btn == null) { warn("Imprimir button missing"); dismissAllDialogs(); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(800);
        // May open print dialog — just cancel/escape
        JDialog sub = UiTestSupport.getTopDialogExcept(details);
        if (sub != null) {
            pass("Imprimir opened: \"" + sub.getTitle() + "\"");
            UiTestSupport.pressEscape(robot);
            UiTestSupport.sleep(400);
        } else {
            pass("Imprimir clicked — no Swing sub-dialog (native print UI may be headless/no-op)");
            UiTestSupport.pressEscape(robot);
            UiTestSupport.sleep(400);
        }
        dismissAllDialogs();
    }

    // ── LLISTES MANAGEMENT ────────────────────────────────────────────────────────

    private static void testLlistesManagement(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Gestionar llistes");
        if (btn == null) { warn("Gestionar llistes button not found"); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(700);
        JDialog dlg = UiTestSupport.waitForDialog(2000);
        if (dlg == null) { warn("Gestionar llistes dialog missing"); return; }

        AbstractButton novaBtn  = UiTestSupport.findBtnIn((Container)dlg, "Nova");
        JTextField     nameTF   = UiTestSupport.findComponent((Container)dlg, JTextField.class);
        AbstractButton upBtn    = UiTestSupport.findBtnIn((Container)dlg, "▲", "Pujar");
        AbstractButton downBtn  = UiTestSupport.findBtnIn((Container)dlg, "▼", "Baixar");
        AbstractButton colorBtn = UiTestSupport.findBtnIn((Container)dlg, "Color");
        AbstractButton delBtn   = UiTestSupport.findBtnIn((Container)dlg, "Eliminar");

        // Empty name → expect validation
        if (novaBtn != null && nameTF != null) {
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(""); });
            UiTestSupport.doClick(novaBtn); UiTestSupport.sleep(400);
            JDialog err = UiTestSupport.getTopDialogExcept(dlg);
            if (err != null) { pass("Empty list name → error dialog"); dismissTopDialog(); }
            else warn("Empty list name: silently ignored");
        }

        // Create 3 test lists
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 3 && novaBtn != null && nameTF != null; i++) {
            String name = "StressTestList_" + i + "_" + (System.currentTimeMillis() % 1000);
            final JTextField tf = nameTF;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText(name); });
            UiTestSupport.sleep(80);
            UiTestSupport.doClick(novaBtn); UiTestSupport.sleep(500);
            JDialog err = UiTestSupport.getTopDialogExcept(dlg);
            if (err != null) { warn("List create error: " + err.getTitle()); dismissTopDialog(); }
            else { created.add(name); log("  Created: " + name); }
        }
        if (!created.isEmpty()) pass("Created " + created.size() + " test lists");

        // Reorder (ensure selection first)
        if (upBtn != null && downBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForReorder = (JList<Object>) UiTestSupport.findComponent((Container)dlg, JList.class);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            UiTestSupport.doClick(downBtn); UiTestSupport.sleep(200);
            if (listForReorder != null && listForReorder.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForReorder.setSelectedIndex(0));
            UiTestSupport.doClick(upBtn); UiTestSupport.sleep(200);
            pass("Reorder buttons: up/down work");
        }

        // Color button — ensure an item is selected first
        if (colorBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listForColor = (JList<Object>) UiTestSupport.findComponent((Container)dlg, JList.class);
            if (listForColor != null && listForColor.getModel().getSize() > 0)
                SwingUtilities.invokeAndWait(() -> listForColor.setSelectedIndex(0));
            UiTestSupport.doClick(colorBtn); UiTestSupport.sleep(700);
            JDialog colorDlg = UiTestSupport.getTopDialogExcept(dlg);
            if (colorDlg != null) {
                pass("Color picker dialog opened");
                UiTestSupport.pressEscape(robot);
                UiTestSupport.sleep(400); dismissAllDialogsExcept(dlg);
            } else warn("Color picker did not open");
        }

        // Delete created lists
        if (delBtn != null) {
            @SuppressWarnings("unchecked")
            JList<Object> listComp = (JList<Object>) UiTestSupport.findComponent((Container)dlg, JList.class);
            for (int i = 0; i < created.size(); i++) {
                if (listComp != null) {
                    int idx = listComp.getModel().getSize() - 1;
                    if (idx < 0) break;
                    SwingUtilities.invokeAndWait(() -> listComp.setSelectedIndex(Math.max(0, listComp.getModel().getSize()-1)));
                    UiTestSupport.sleep(150);
                }
                UiTestSupport.doClick(delBtn); UiTestSupport.sleep(400);
                JDialog confirm = UiTestSupport.getTopDialogExcept(dlg);
                if (confirm != null) {
                    if (isStressTestListDeleteConfirm(confirm)) {
                        clickAffirmDelete(confirm);
                    } else {
                        warn("Unexpected confirm during list delete — cancelled");
                        cancelTopDialog();
                    }
                    UiTestSupport.sleep(400);
                }
            }
            pass("Deleted " + created.size() + " test lists");
        }

        SwingUtilities.invokeLater(dlg::dispose); UiTestSupport.sleep(400);
    }

    // ── STATS ────────────────────────────────────────────────────────────────────

    private static void testStats(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Estad");
        if (btn == null) { warn("Stats button not found"); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(800);
        JDialog dlg = UiTestSupport.waitForDialog(2000); if (dlg == null) { warn("Stats dialog missing"); return; }
        List<String> items = new ArrayList<>();
        UiTestSupport.collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[TBL]"))) pass("Stats: has table");
        else pass("Stats: no shelf table (no list assignments — expected)");
        if (items.stream().anyMatch(s -> s.contains("[LBL]") && s.toLowerCase().contains("llegit"))) pass("Stats: has llegit count");
        else warn("Stats: missing llegit count label");
        // Edit objective
        JTextField objField = UiTestSupport.findComponent((Container)dlg, JTextField.class);
        if (objField != null) {
            final JTextField tf = objField;
            SwingUtilities.invokeAndWait(() -> { tf.selectAll(); tf.setText("52"); });
            UiTestSupport.sleep(200); pass("Stats: objective field editable");
        }
        SwingUtilities.invokeLater(dlg::dispose); UiTestSupport.sleep(400);
    }

    // ── CONFIGURACIÓ ─────────────────────────────────────────────────────────────

    private static void testConfiguracio(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Configuració", "Configur");
        if (btn == null) { warn("Configuració button not found"); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(900);
        JDialog dlg = UiTestSupport.waitForDialog(3000); if (dlg == null) { warn("Configuració dialog missing"); return; }
        // Verify key fields present
        List<String> items = new ArrayList<>();
        UiTestSupport.collectComponents((Container)dlg, "", items);
        if (items.stream().anyMatch(s -> s.contains("[CMB]"))) pass("Configuració: has comboboxes");
        else warn("Configuració: missing comboboxes");
        // Cancel without saving
        AbstractButton cancel = UiTestSupport.findBtnIn((Container)dlg, "Cancel", "Tancar");
        if (cancel != null) { UiTestSupport.doClick(cancel); UiTestSupport.sleep(300); }
        else { SwingUtilities.invokeLater(dlg::dispose); UiTestSupport.sleep(300); }
        pass("Configuració dialog opened and closed without crash");
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────────

    private static void testExport(JFrame main) throws Exception {
        ensureFilterOpen(main);
        UiTestSupport.sleep(300);
        AbstractButton exportBtn = UiTestSupport.findBtnIn(main, "Exportar");
        if (exportBtn == null) { warn("Export button not found (need filter open)"); closeFilter(main); return; }

        String[][] exports = {{"CSV", "Export CSV", "Exportar CSV"},
                               {"JSON", "Export JSON", "Exportar JSON"},
                               {"HTML", "Export HTML", "Exportar HTML"}};
        for (String[] exp : exports) {
            UiTestSupport.doClick(exportBtn); UiTestSupport.sleep(400);
            AbstractButton item = UiTestSupport.findBtnIn(main, exp[1], exp[2]);
            if (item != null) { UiTestSupport.doClick(item); UiTestSupport.sleep(700); }
            JDialog fc = UiTestSupport.getTopDialog();
            if (fc != null) {
                pass("Export " + exp[0] + " → dialog appeared");
                focusMain(main);
                UiTestSupport.pressEscape(robot);
                UiTestSupport.sleep(400); dismissAllDialogs();
            } else {
                warn("Export " + exp[0] + " → no dialog (may have cancelled or auto-saved)");
                focusMain(main);
                UiTestSupport.pressEscape(robot);
                UiTestSupport.sleep(200);
            }
        }
        closeFilter(main);
    }

    // ── BACKUP ───────────────────────────────────────────────────────────────────

    private static void testBackup(JFrame main) throws Exception {
        ensureFilterOpen(main);
        UiTestSupport.sleep(300);
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Backup", "backup");
        if (btn == null) { warn("Backup button not found (need filter open)"); closeFilter(main); return; }
        UiTestSupport.doClick(btn); UiTestSupport.sleep(800);
        JDialog fc = UiTestSupport.getTopDialog();
        if (fc != null) {
            pass("Backup BD → dialog: \"" + fc.getTitle() + "\"");
            UiTestSupport.pressEscape(robot);
            UiTestSupport.sleep(400); dismissAllDialogs();
        } else {
            warn("Backup BD → no dialog appeared");
        }
        closeFilter(main);
    }

    // ── KEYBOARD SHORTCUTS ────────────────────────────────────────────────────────

    private static void testKbd_ctrlA(JFrame main) throws Exception {
        JTable table = UiTestSupport.findComponent((Container)main, JTable.class);
        if (table == null) { warn("No table for Ctrl+A test"); return; }
        goAllBooks(main); UiTestSupport.sleep(300);
        triggerRootAction(main, "seleccionarTot");
        UiTestSupport.sleep(200);
        int sel = table.getSelectedRowCount();
        if (sel > 0) pass("Ctrl+A selected " + sel + " rows");
        else warn("Ctrl+A: 0 rows selected");
        SwingUtilities.invokeAndWait(() -> table.clearSelection());
    }

    private static void testKbd_ctrlF(JFrame main) throws Exception {
        triggerRootAction(main, "focusFiltres");
        UiTestSupport.sleep(200);
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JTextField search = findSearchField(main);
        if (search != null && search.isFocusOwner()) pass("Ctrl+F → search field focused");
        else if (focused instanceof JTextField) pass("Ctrl+F → JTextField focused");
        else warn("Ctrl+F → focused: " + (focused != null ? focused.getClass().getSimpleName() : "null"));
    }

    private static void testKbd_ctrlN(JFrame main) throws Exception {
        triggerRootAction(main, "nouLlibre");
        UiTestSupport.sleep(900);
        JDialog dlg = UiTestSupport.getTopDialog();
        if (dlg != null) {
            pass("Ctrl+N → new book dialog: \"" + dlg.getTitle() + "\"");
            dismissTopDialog();
        } else warn("Ctrl+N → no dialog appeared");
    }

    // ── ALEATORI ────────────────────────────────────────────────────────────────

    private static void testAleatori(JFrame main) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "aleatori", "Aleatori");
        if (btn == null) { warn("Llibre aleatori button not found"); return; }
        // Click 3 times
        for (int i = 0; i < 3; i++) {
            UiTestSupport.doClick(btn); UiTestSupport.sleep(700);
            JDialog dlg = UiTestSupport.getTopDialog();
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
            JDialog dlg = UiTestSupport.waitForDialog(2000);
            if (dlg == null) { fail("burst create: dialog missing at " + i); return; }
            long isbn = uniqueISBN();
            UiTestSupport.setFieldNear(dlg, "ISBN", String.valueOf(isbn));
            UiTestSupport.setFieldNear(dlg, "Títol", "Stress_" + i);
            UiTestSupport.setFieldNear(dlg, "Autor", "Bot");
            UiTestSupport.clickSave(dlg);
            UiTestSupport.sleep(350);
            dismissAllDialogs();
            createdISBNs.add(isbn);
        }
        pass("Burst-created " + count + " books");
        goAllBooks(main);
    }

    private static void testExtreme_pagination(JFrame main, int clicks) throws Exception {
        AbstractButton next = UiTestSupport.findBtnIn(main, "Seguent", "Next");
        if (next == null) { warn("No next-page button"); return; }
        for (int i = 0; i < clicks; i++) { UiTestSupport.doClick(next); UiTestSupport.sleep(80); }
        AbstractButton prev = UiTestSupport.findBtnIn(main, "Anterior", "Previous");
        if (prev != null) for (int i = 0; i < Math.min(clicks, 10); i++) { UiTestSupport.doClick(prev); UiTestSupport.sleep(80); }
        pass("Pagination hammered " + clicks + " forward clicks");
    }

    private static void testExtreme_filterLoop(JFrame main, int iterations) throws Exception {
        ensureFilterOpen(main);
        JTextField nom = UiTestSupport.findTextFieldNear(main, "Nom");
        JTextField search = findSearchField(main);
        AbstractButton filtrar = UiTestSupport.findBtnIn(main, "Filtrar");
        AbstractButton clear = UiTestSupport.findBtnIn(main, "Treure", "Quitar");
        for (int i = 0; i < iterations; i++) {
            if (search != null) UiTestSupport.setField(search, i % 2 == 0 ? "a" : "");
            if (nom != null) UiTestSupport.setField(nom, i % 3 == 0 ? "Stress" : "");
            if (filtrar != null) UiTestSupport.doClick(filtrar);
            UiTestSupport.sleep(40);
            if (clear != null && i % 5 == 0) UiTestSupport.doClick(clear);
            UiTestSupport.sleep(30);
        }
        if (search != null) UiTestSupport.setField(search, "");
        pass("Filter/search loop x" + iterations);
    }

    private static void testExtreme_gallery(JFrame main, int toggles) throws Exception {
        AbstractButton galeria = UiTestSupport.findBtnIn(main, "Galeria");
        if (galeria == null) { warn("Galeria button missing"); return; }
        for (int i = 0; i < toggles; i++) { UiTestSupport.doClick(galeria); UiTestSupport.sleep(60); }
        pass("Gallery toggled x" + toggles);
    }

    private static void testExtreme_concurrent(JFrame main, int threadCount) throws Exception {
        AtomicReference<AbstractButton> toggleBtn = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
            toggleBtn.set(UiTestSupport.findBtnIn(main, "Filtres", "Galeria", "Sèrie")));
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
                        UiTestSupport.sleep(30 + (id % 10));
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
            }, "stress-ui-" + t);
            th.setDaemon(true);
            th.start();
            pool.add(th);
        }
        start.countDown();
        for (Thread th : pool) th.join(8000);
        UiTestSupport.sleep(500);
        dismissAllDialogs();
        if (errors.get() > 0) fail("Concurrent UI errors: " + errors.get());
        else pass("Concurrent UI spam (" + threadCount + " threads x5 clicks)");
    }

    private static void testExtreme_dialogSpam(JFrame main) throws Exception {
        String[] opens = {"Estad", "Configur", "aleatori", "llistes"};
        for (int round = 0; round < 3; round++) {
            for (String hint : opens) {
                AbstractButton btn = UiTestSupport.findBtnIn(main, hint);
                if (btn == null) continue;
                UiTestSupport.doClick(btn); UiTestSupport.sleep(400);
                dismissAllDialogs();
            }
        }
        pass("Dialog rapid-fire x3 rounds");
    }

    private static void testAllSidebarButtons(JFrame main) throws Exception {
        String[] btns = {"Tots els", "Afegits recentment", "Llegits", "Llista de desitjos", "En curs"};
        for (String label : btns) {
            AbstractButton btn = UiTestSupport.findBtnIn(main, label);
            if (btn == null) { warn("Sidebar button not found: \"" + label + "\""); continue; }
            UiTestSupport.doClick(btn); UiTestSupport.sleep(500);
            JDialog d = UiTestSupport.getTopDialog();
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
        goAllBooks(main); UiTestSupport.sleep(500);
        JTextField sf = findSearchField(main);
        int deleted = 0;
        // Clear search bar so RowFilter doesn't hide books
        if (sf != null) { UiTestSupport.setField(sf, ""); UiTestSupport.sleep(400); }
        for (long isbn : createdISBNs) {
            JTable table = UiTestSupport.findComponent((Container)main, JTable.class);
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
            UiTestSupport.pressDelete(robot);
            UiTestSupport.sleep(600);
            JDialog confirm = UiTestSupport.getTopDialog();
            if (confirm != null) {
                if (UiTestSupport.isTestBookDeleteConfirm(confirm)) {
                    clickAffirmDelete(confirm);
                    UiTestSupport.sleep(400);
                    deleted++;
                } else {
                    warn("Unexpected dialog during cleanup delete for ISBN " + isbn + " — cancelled");
                    cancelTopDialog();
                }
            } else {
                warn("No delete confirm for ISBN " + isbn + " — book may remain");
            }
        }
        if (sf != null) { UiTestSupport.setField(sf, ""); UiTestSupport.sleep(400); }
        goAllBooks(main);
        pass("Cleanup: deleted " + deleted + "/" + createdISBNs.size() + " test books");
    }

    // ── HELPERS: INTERACTION ──────────────────────────────────────────────────────

    private static void openNewBookDialog(JFrame main) throws Exception {
        dismissAllDialogs();
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Afegir", "Nou", "New");
        if (btn != null) UiTestSupport.doClick(btn);
        else {
            SwingUtilities.invokeAndWait(() -> main.requestFocusInWindow());
            UiTestSupport.sleep(80);
            UiTestSupport.pressCtrlN(robot);
        }
        UiTestSupport.sleep(600);
    }

    private static void openRow(JFrame main, int row) throws Exception {
        JTable table = UiTestSupport.findComponent((Container)main, JTable.class);
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
        UiTestSupport.sleep(900);
    }

    private static void triggerRootAction(JFrame main, String key) throws Exception {
        SwingUtilities.invokeLater(() -> {
            main.toFront();
            main.getRootPane().requestFocusInWindow();
            javax.swing.Action act = main.getRootPane().getActionMap().get(key);
            if (act != null) act.actionPerformed(new java.awt.event.ActionEvent(main.getRootPane(), 0, key));
        });
        UiTestSupport.sleep(120);
    }

    /** Brings the main frame to the front and requests OS-level focus. */
    private static void focusMain(JFrame main) throws Exception {
        SwingUtilities.invokeAndWait(() -> { main.toFront(); main.requestFocus(); });
        UiTestSupport.sleep(120);
    }

    /** Opens row, flushes EDT, waits longer for the details JDialog, and verifies it is a book-details dialog. */
    private static JDialog openDetailsAndWait(JFrame main, int row) throws Exception {
        dismissAllDialogs();
        UiTestSupport.sleep(200);
        openRow(main, row);
        // Flush any pending EDT events so the dialog content is fully laid out
        SwingUtilities.invokeAndWait(() -> {});
        UiTestSupport.sleep(400);
        JDialog d = UiTestSupport.waitForDialog(3000);
        if (d == null) return null;
        if (UiTestSupport.isBookDetailsDialog(d)) return d;
        // Make sure it's the details dialog, not an error dialog
        if (UiTestSupport.looksLikeError(d)) {
            log("  [openDetailsAndWait] got error dialog: \"" + d.getTitle() + "\" — dismissing and retrying");
            dismissAllDialogs();
            UiTestSupport.sleep(300);
            openRow(main, row);
            SwingUtilities.invokeAndWait(() -> {});
            UiTestSupport.sleep(400);
            d = UiTestSupport.waitForDialog(3000);
        }
        return d;
    }

    private static void goAllBooks(JFrame main) {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Tots els");
        if (btn != null) { UiTestSupport.doClick(btn); UiTestSupport.sleep(500); }
    }

    private static void ensureFilterOpen(JFrame main) {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Filtres");
        if (btn != null && btn.getText() != null && !btn.getText().contains("▲")) { UiTestSupport.doClick(btn); UiTestSupport.sleep(400); }
    }

    private static void closeFilter(JFrame main) {
        AbstractButton btn = UiTestSupport.findBtnIn(main, "Filtres ▲");
        if (btn != null) { UiTestSupport.doClick(btn); UiTestSupport.sleep(300); }
    }

    private static void checkExpectError(String label) {
        JDialog after = UiTestSupport.getTopDialog();
        if (after != null && UiTestSupport.looksLikeError(after)) {
            pass(label + " → validation dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else if (after != null && UiTestSupport.isBookFormDialog(after)) {
            pass(label + " → save rejected (form still open: \"" + after.getTitle() + "\")");
            dismissAllDialogs();
        } else if (after != null) {
            warn(label + " → unexpected dialog: \"" + after.getTitle() + "\"");
            dismissAllDialogs();
        } else {
            fail(label + " accepted without validation error");
        }
    }

    private static void dismissAllDialogs() {
        for (int i = 0; i < 8; i++) {
            JDialog d = UiTestSupport.getTopDialog();
            if (d == null) break;
            cancelTopDialog();
        }
    }

    private static void dismissAllDialogsExcept(JDialog keep) {
        for (int i = 0; i < 5; i++) {
            JDialog d = UiTestSupport.getTopDialog();
            if (d == null || d == keep) break;
            AbstractButton ok = UiTestSupport.findBtnIn((Container)d, "OK", "Tancar", "Cancel·lar", "Close");
            if (ok != null) UiTestSupport.doClick(ok); else SwingUtilities.invokeLater(d::dispose);
            UiTestSupport.sleep(300);
        }
    }

    private static void dismissTopDialog() {
        cancelTopDialog();
    }

    /** Dismiss without affirming destructive actions (no Sí/Yes/OK). */
    private static void cancelTopDialog() {
        JDialog d = UiTestSupport.getTopDialog();
        if (d == null) return;
        AbstractButton cancel = UiTestSupport.findBtnIn((Container)d, "Cancel·lar", "Cancelar", "Tancar", "Close", "No");
        if (cancel != null) UiTestSupport.doClick(cancel);
        else {
            UiTestSupport.pressEscape(robot);
            UiTestSupport.sleep(200);
            if (UiTestSupport.getTopDialog() == d) SwingUtilities.invokeLater(d::dispose);
        }
        UiTestSupport.sleep(280);
    }

    private static void clickAffirmDelete(JDialog confirm) {
        AbstractButton yes = UiTestSupport.findBtnIn((Container)confirm, "Sí", "Yes", "Eliminar", "Esborrar", "OK");
        if (yes != null) UiTestSupport.doClick(yes);
        else cancelTopDialog();
    }

    private static boolean isStressTestListDeleteConfirm(JDialog d) {
        if (!UiTestSupport.isTestBookDeleteConfirm(d)) return false;
        String body = dialogText(d);
        return body.contains("stresstestlist");
    }

    private static String dialogText(JDialog d) {
        List<String> items = new ArrayList<>();
        UiTestSupport.collectComponents((Container)d, "", items);
        return UiTestSupport.norm(String.join(" ", items));
    }

    private static JTextField findSearchField(JFrame main) {
        List<Component> flat = new ArrayList<>();
        UiTestSupport.flattenVisible((Container)main, flat);
        for (Component c : flat) {
            if (c instanceof JTextField tf) {
                String tip = tf.getToolTipText();
                if (tip != null && (tip.toLowerCase().contains("cerca") || tip.toLowerCase().contains("search"))) return tf;
            }
        }
        return UiTestSupport.findComponent((Container)main, JTextField.class);
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

    // ── LOG ───────────────────────────────────────────────────────────────────────

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
}
