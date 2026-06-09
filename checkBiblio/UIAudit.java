package checkBiblio;

/**
 * Biblioteca UI Audit Tool
 *
 * Launches the real Biblioteca app in-process and exposes an interactive
 * command console so an external operator (human or AI) can drive the UI,
 * inspect every component, click buttons, fill fields, and run a built-in
 * automated walkthrough.
 *
 * Compile:  javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio/UiTestSupport.java checkBiblio/UIAudit.java checkBiblio/I18nAudit.java -d bin
 * Run:      java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio.UIAudit [--auto]
 *
 * Commands (interactive mode):
 *   help                      show this list
 *   windows                   list all open windows
 *   scan [title]              list all buttons/labels/fields in a window
 *   click <text>              click first button whose text contains <text>
 *   type <text>               type text into the currently-focused component
 *   clear                     clear the focused text field (Ctrl+A, Delete)
 *   focus <text>              click a text field whose label contains <text>
 *   close                     dispose the topmost visible dialog
 *   enter                     press Enter
 *   esc                       press Escape
 *   wait [ms]                 sleep N ms (default 500)
 *   auto                      run the built-in automated full audit
 *   rows                      print first 10 rows of the visible JTable
 *   open-row <n>              double-click row n in the JTable (0-based)
 *   exit                      quit
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;

public class UIAudit {

    // ── State ──────────────────────────────────────────────────────────────────
    private static Robot robot;
    private static PrintWriter reportFile;
    private static int warnCount = 0;
    private static int failCount = 0;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        boolean autoMode = Arrays.asList(args).contains("--auto");

        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            System.getProperty("biblioteca.h2.url",
                "jdbc:h2:mem:uiAudit;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"));

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("FATAL: Headless environment — Robot required. Install Xvfb or set DISPLAY.");
            System.exit(1);
        }

        reportFile = new PrintWriter(new FileWriter("checkBiblio/audit_report.txt", false), true);

        robot = new Robot();
        robot.setAutoDelay(80);

        log("=== Biblioteca UIAudit ===");
        log("Mode: " + (autoMode ? "AUTOMATED" : "INTERACTIVE"));
        log("Started: " + LocalDateTime.now());

        // ── Launch app (force Swing mode via --swing arg, skips ModeSelectorDialog) ──
        log("Launching Biblioteca in Swing mode...");
        Thread appThread = new Thread(() -> {
            try { main.Ejecutable.main(new String[]{"--swing"}); }
            catch (Exception e) { log("APP LAUNCH ERROR: " + e); }
        }, "biblioteca-main");
        appThread.setDaemon(true);
        appThread.start();

        // ── Wait for main frame ───────────────────────────────────────────────
        log("Waiting for main JFrame (up to 12s)...");
        JFrame mainFrame = UiTestSupport.waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: Main window never appeared. Aborting.");
            closeReport();
            System.exit(1);
        }
        log("OK: Main window visible — \"" + mainFrame.getTitle() + "\"");
        UiTestSupport.sleep(600);

        if (autoMode) {
            runAutomated(mainFrame);
        } else {
            runInteractive(mainFrame);
        }
        closeReport();
        System.exit(failCount > 0 ? 1 : 0);
    }

    // ── Interactive console ───────────────────────────────────────────────────
    private static void runInteractive(JFrame mainFrame) throws Exception {
        print("");
        print("======================================================");
        print("  Biblioteca UIAudit — Interactive Console");
        print("  Type 'help' for command list, 'exit' to quit.");
        print("======================================================");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\naudit> ");
            System.out.flush();
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            log("CMD: " + line);
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg  = parts.length > 1 ? parts[1] : "";

            try {
                switch (cmd) {
                    case "exit"       -> { log("Exiting."); return; }
                    case "help"       -> printHelp();
                    case "windows"    -> cmdWindows();
                    case "scan"       -> cmdScan(arg.isEmpty() ? null : arg);
                    case "click"      -> cmdClick(arg);
                    case "type"       -> cmdType(arg);
                    case "clear"      -> cmdClear();
                    case "focus"      -> cmdFocus(arg);
                    case "close"      -> cmdClose();
                    case "enter"      -> UiTestSupport.pressEnter(robot);
                    case "esc"        -> UiTestSupport.pressEscape(robot);
                    case "wait"       -> UiTestSupport.sleep(arg.isEmpty() ? 500 : Long.parseLong(arg));
                    case "auto"       -> runAutomated(mainFrame);
                    case "rows"       -> cmdRows(mainFrame);
                    case "open-row"   -> cmdOpenRow(mainFrame, Integer.parseInt(arg));
                    default           -> print("Unknown command: " + cmd + " (type 'help')");
                }
            } catch (Exception e) {
                print("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                log("ERROR [" + line + "]: " + e);
            }
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    private static void cmdWindows() {
        Window[] all = Window.getWindows();
        int visible = 0;
        for (Window w : all) {
            if (!w.isVisible()) continue;
            visible++;
            String title = UiTestSupport.windowTitle(w);
            print("  [" + w.getClass().getSimpleName() + "] \"" + title + "\" size=" + w.getSize());
        }
        if (visible == 0) print("  (no visible windows)");
    }

    private static void cmdScan(String titleFragment) {
        Window w = titleFragment == null
            ? UiTestSupport.getTopWindow()
            : UiTestSupport.findWindowByTitle(titleFragment);
        if (w == null) { print("No window found" + (titleFragment != null ? ": " + titleFragment : "")); return; }
        print("Scanning: \"" + UiTestSupport.windowTitle(w) + "\"");
        List<String> found = new ArrayList<>();
        UiTestSupport.collectComponents((Container)w, "", found);
        found.forEach(UIAudit::print);
        log("SCAN: " + found.size() + " components in \"" + UiTestSupport.windowTitle(w) + "\"");
    }

    private static void cmdClick(String text) throws Exception {
        if (text.isEmpty()) { print("Usage: click <button_text>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton btn = UiTestSupport.findBtnIn((Container)w, text);
            if (btn != null) {
                print("Clicking \"" + btn.getText() + "\" in \"" + UiTestSupport.windowTitle(w) + "\"");
                log("CLICK: \"" + btn.getText() + "\"");
                UiTestSupport.clickComponent(robot, btn);
                UiTestSupport.sleep(400);
                // Report any new dialog
                JDialog d = UiTestSupport.getTopDialog();
                if (d != null) print("  → Dialog appeared: \"" + d.getTitle() + "\"");
                return;
            }
        }
        print("No button found matching: \"" + text + "\"");
        log("WARN: button not found: \"" + text + "\"");
    }

    private static void cmdType(String text) {
        print("Typing: " + text);
        log("TYPE: " + text);
        // Use clipboard for reliability with non-ASCII chars
        java.awt.datatransfer.Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new java.awt.datatransfer.StringSelection(text), null);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    private static void cmdClear() {
        UiTestSupport.pressCtrlA(robot);
        UiTestSupport.pressDelete(robot);
        print("Cleared focused field.");
    }

    private static void cmdFocus(String labelHint) throws Exception {
        if (labelHint.isEmpty()) { print("Usage: focus <label_text>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JTextField tf = UiTestSupport.findTextFieldNear((Container)w, labelHint);
            if (tf != null) {
                UiTestSupport.clickComponent(robot, tf);
                print("Focused text field near label \"" + labelHint + "\"");
                log("FOCUS: field near \"" + labelHint + "\"");
                return;
            }
        }
        print("No text field found near label: \"" + labelHint + "\"");
    }

    private static void cmdClose() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--) {
            if (ws[i] instanceof JDialog d && d.isVisible()) {
                print("Closing dialog: \"" + d.getTitle() + "\"");
                log("CLOSE: \"" + d.getTitle() + "\"");
                SwingUtilities.invokeLater(d::dispose);
                return;
            }
        }
        print("No dialog to close.");
    }

    private static void cmdRows(JFrame mainFrame) {
        JTable t = UiTestSupport.findComponent((Container)mainFrame, JTable.class);
        if (t == null) { print("No JTable found."); return; }
        TableModel m = t.getModel();
        int rows = Math.min(10, m.getRowCount());
        print("Table: " + m.getRowCount() + " rows, " + m.getColumnCount() + " cols");
        // Header
        StringBuilder header = new StringBuilder("  #  | ");
        for (int c = 0; c < m.getColumnCount(); c++) header.append(m.getColumnName(c)).append(" | ");
        print(header.toString());
        for (int r = 0; r < rows; r++) {
            StringBuilder row = new StringBuilder("  " + r + "  | ");
            for (int c = 0; c < m.getColumnCount(); c++) {
                Object v = m.getValueAt(r, c);
                row.append(v != null ? v.toString() : "").append(" | ");
            }
            print(row.toString());
        }
        if (m.getRowCount() > 10) print("  ... (" + (m.getRowCount() - 10) + " more)");
    }

    private static void cmdOpenRow(JFrame mainFrame, int rowIndex) throws Exception {
        JTable table = UiTestSupport.findComponent((Container)mainFrame, JTable.class);
        if (table == null) { print("No JTable found."); return; }
        if (rowIndex >= table.getRowCount()) { print("Row " + rowIndex + " out of range (max " + (table.getRowCount()-1) + ")."); return; }

        SwingUtilities.invokeAndWait(() -> {
            table.setRowSelectionInterval(rowIndex, rowIndex);
            table.scrollRectToVisible(table.getCellRect(rowIndex, 0, true));
            table.requestFocusInWindow();
        });
        UiTestSupport.sleep(200);

        // Fire the registered "obrirDetalls" action directly — avoids Robot entirely (works on Xvfb).
        // Must use invokeLater (not invokeAndWait): modal dialogs block the EDT inside setVisible(true),
        // so invokeAndWait would deadlock waiting for the EDT to finish.
        SwingUtilities.invokeLater(() -> {
            javax.swing.Action act = table.getActionMap().get("obrirDetalls");
            if (act != null) {
                act.actionPerformed(new java.awt.event.ActionEvent(
                    table, java.awt.event.ActionEvent.ACTION_PERFORMED, "obrirDetalls"));
            } else {
                table.dispatchEvent(new KeyEvent(table, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
            }
        });
        UiTestSupport.sleep(800);

        JDialog dlg = UiTestSupport.getTopDialog();
        if (dlg != null) {
            print("Opened dialog: \"" + dlg.getTitle() + "\"");
            log("OPEN-ROW " + rowIndex + " -> dialog \"" + dlg.getTitle() + "\"");
        } else {
            print("No dialog appeared after opening row " + rowIndex + ".");
        }
    }

    // ── Automated full walkthrough ─────────────────────────────────────────────
    private static void runAutomated(JFrame mainFrame) throws Exception {
        log("\n=== AUTOMATED AUDIT START ===");

        // --- Main window scan ---
        log("\n--- MAIN WINDOW ---");
        collectAndLog((Container)mainFrame);

        // --- Sidebar buttons ---
        log("\n--- SIDEBAR BUTTONS ---");
        String[][] sidebar = {
            {"Tots els",   "Tots els Llibres"},
            {"Afegits r",  "Afegits Recentment"},
            {"Llegits",    "Llegits"},
            {"desitjos",   "Llista de desitjos"},
            {"En curs",    "En curs"},
            {"Estad",      "Estadistiques"},
            {"aleatori",   "Aleatori"},
            {"llistes",    "Gestio de Llistes"},
            {"Tema",       "Tema"},
            {"Configur",   "Configuracio"},
            {"Sobre",      "Sobre"},
        };
        for (String[] entry : sidebar) {
            AbstractButton btn = UiTestSupport.findBtnIn((Container)mainFrame, entry[0]);
            if (btn != null) {
                log("CLICK: sidebar \"" + btn.getText() + "\"");
                SwingUtilities.invokeLater(btn::doClick);
                UiTestSupport.sleep(900);
                JDialog d = UiTestSupport.getTopDialog();
                if (d != null) {
                    log("  -> Dialog: \"" + d.getTitle() + "\" — scanning...");
                    collectAndLog((Container)d);
                    SwingUtilities.invokeLater(d::dispose);
                    UiTestSupport.sleep(400);
                } else {
                    log("  -> No dialog (panel update)");
                }
            } else {
                warn("sidebar button not found for hint \"" + entry[0] + "\"");
            }
        }

        // --- Top bar: search, gallery, series ---
        log("\n--- TOP BAR ---");
        testTopBar(mainFrame);

        // --- Filter drawer ---
        log("\n--- FILTER DRAWER ---");
        AbstractButton tog = UiTestSupport.findBtnIn((Container)mainFrame, "Filtre", "Filter");
        if (tog != null) {
            SwingUtilities.invokeLater(tog::doClick); UiTestSupport.sleep(400);
            log("Filter drawer toggled open");
            collectAndLog((Container)mainFrame);
            SwingUtilities.invokeLater(tog::doClick); UiTestSupport.sleep(300);
        } else {
            warn("filter toggle button not found");
        }

        testFilterActions(mainFrame);

        testPagination(mainFrame);

        testIoButtons(mainFrame);

        // Seed library for isolated audit DB, then exercise table/details
        boolean bookCreated = testCreateBook(mainFrame);

        // Reset to all books before table inspection
        AbstractButton allBtn = UiTestSupport.findBtnIn((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); UiTestSupport.sleep(600); }

        // --- Table inspection ---
        log("\n--- TABLE ---");
        JTable table = UiTestSupport.findComponent((Container)mainFrame, JTable.class);
        if (table != null) {
            TableModel m = table.getModel();
            log("Rows: " + m.getRowCount() + "  Cols: " + m.getColumnCount());
            StringBuilder header = new StringBuilder("Columns: ");
            for (int c = 0; c < m.getColumnCount(); c++) header.append("[").append(m.getColumnName(c)).append("] ");
            log(header.toString());
        } else {
            warn("No JTable found in main frame");
        }

        // --- Open first book ---
        if (bookCreated && table != null && table.getRowCount() > 0) {
            log("\n--- BOOK DETAILS (row 0) ---");
            cmdOpenRow(mainFrame, 0);
            JDialog detailsDlg = UiTestSupport.getTopDialog();
            if (detailsDlg != null) {
                collectAndLog((Container)detailsDlg);

                // Try Edit
                AbstractButton editBtn = UiTestSupport.findBtnIn((Container)detailsDlg, "Editar", "Edit");
                if (editBtn != null) {
                    log("Clicking Edit...");
                    SwingUtilities.invokeLater(editBtn::doClick); UiTestSupport.sleep(500);
                    log("Edit mode active. Pressing Escape to cancel.");
                    UiTestSupport.pressEscape(robot);
                    UiTestSupport.sleep(300);
                } else {
                    warn("Edit button not found in details dialog");
                }

                clickSubDialogButton(detailsDlg, "Llistes");
                clickSubDialogButton(detailsDlg, "Etiquetes");
                clickSubDialogButton(detailsDlg, "Historial");
                clickSubDialogButton(detailsDlg, "Imprimir");

                SwingUtilities.invokeLater(detailsDlg::dispose); UiTestSupport.sleep(400);
            } else {
                warn("No dialog appeared after opening row 0");
            }
        } else if (!bookCreated) {
            warn("Create-book failed, skipping book detail test");
        } else {
            warn("Table empty, skipping book detail test");
        }

        // --- Edit first book, change all fields ---
        if (bookCreated) testEditBook(mainFrame);

        log("\n--- I18n static audit ---");
        int[] i18nFail = {0}, i18nWarn = {0};
        I18nAudit.run(reportFile, i18nFail, i18nWarn);
        failCount += i18nFail[0];
        warnCount += i18nWarn[0];
        log("\n=== AUTOMATED AUDIT COMPLETE ===");
        log("FAIL: " + failCount + "  WARN: " + warnCount);
        log("Report: checkBiblio/audit_report.txt");
        writeJsonReport();
        print("\n[AUTO] Audit complete. FAIL=" + failCount + " WARN=" + warnCount
            + " — see checkBiblio/audit_report.txt");
    }

    private static void warn(String msg) {
        warnCount++;
        log("WARN: " + msg);
    }

    private static void fail(String msg) {
        failCount++;
        log("FAIL: " + msg);
    }

    private static void clickSubDialogButton(JDialog detailsDlg, String btnHint) throws Exception {
        AbstractButton btn = UiTestSupport.findBtnIn((Container)detailsDlg, btnHint);
        if (btn == null) { warn("Details button not found: \"" + btnHint + "\""); return; }
        log("Clicking \"" + btn.getText() + "\"...");
        SwingUtilities.invokeLater(btn::doClick);
        UiTestSupport.sleep(700);
        JDialog sub = UiTestSupport.getTopDialog();
        if (sub != null && sub != detailsDlg) {
            log("  -> Sub-dialog: \"" + sub.getTitle() + "\"");
            dismissDialog(sub);
        } else {
            log("  -> No sub-dialog for \"" + btnHint + "\"");
        }
    }

    private static void dismissDialog(JDialog d) {
        AbstractButton close = UiTestSupport.findBtnIn((Container)d, "Tancar", "OK", "Cancel");
        if (close != null) SwingUtilities.invokeLater(close::doClick);
        else SwingUtilities.invokeLater(d::dispose);
        UiTestSupport.sleep(350);
    }

    private static void dismissAllDialogs() {
        for (int i = 0; i < 8; i++) {
            JDialog d = UiTestSupport.getTopDialog();
            if (d == null) break;
            dismissDialog(d);
        }
    }

    private static void testTopBar(JFrame main) throws Exception {
        JTextField search = UiTestSupport.findTextFieldNear(main, "ISBN");
        if (search == null) {
            List<Component> flat = new ArrayList<>();
            UiTestSupport.flattenVisible(main, flat);
            for (Component c : flat) {
                if (c instanceof JTextField tf && tf.getToolTipText() != null
                        && UiTestSupport.norm(tf.getToolTipText()).contains("cerca")) {
                    search = tf;
                    break;
                }
            }
        }
        if (search != null) {
            final JTextField searchField = search;
            SwingUtilities.invokeAndWait(() -> searchField.setText("test"));
            UiTestSupport.sleep(200);
            log("Search bar set to \"test\"");
            SwingUtilities.invokeAndWait(() -> searchField.setText(""));
            UiTestSupport.sleep(200);
        } else {
            warn("Search bar not found");
        }

        AbstractButton galeria = UiTestSupport.findBtnIn(main, "Galeria");
        if (galeria != null) {
            SwingUtilities.invokeLater(galeria::doClick); UiTestSupport.sleep(700);
            log("Gallery mode toggled on");
            SwingUtilities.invokeLater(galeria::doClick); UiTestSupport.sleep(700);
            log("Gallery mode toggled off");
        } else {
            warn("Galeria button not found");
        }

        AbstractButton series = UiTestSupport.findBtnIn(main, "Sèrie", "Series");
        if (series != null) {
            SwingUtilities.invokeLater(series::doClick); UiTestSupport.sleep(500);
            SwingUtilities.invokeLater(series::doClick); UiTestSupport.sleep(500);
            log("Series grouping toggled x2");
        } else {
            warn("Series button not found");
        }
    }

    private static void testPagination(JFrame main) throws Exception {
        AbstractButton next = UiTestSupport.findBtnIn(main, "Seguent", "Next");
        AbstractButton prev = UiTestSupport.findBtnIn(main, "Anterior", "Previous");
        if (next != null) {
            SwingUtilities.invokeLater(next::doClick); UiTestSupport.sleep(600);
            log("Pagination: next page");
        } else {
            warn("Next page button not found");
        }
        if (prev != null) {
            SwingUtilities.invokeLater(prev::doClick); UiTestSupport.sleep(600);
            log("Pagination: previous page");
        }
    }

    private static void testFilterActions(JFrame main) throws Exception {
        AbstractButton tog = UiTestSupport.findBtnIn(main, "Filtre", "Filter");
        if (tog != null && (tog.getText() == null || !tog.getText().contains("▲"))) {
            SwingUtilities.invokeLater(tog::doClick); UiTestSupport.sleep(400);
        }
        UiTestSupport.setFieldNear(main, "Nom", "audit");
        AbstractButton filtrar = UiTestSupport.findBtnIn(main, "Filtrar");
        if (filtrar != null) {
            SwingUtilities.invokeLater(filtrar::doClick); UiTestSupport.sleep(700);
            log("Filter applied (nom=audit)");
        }
        AbstractButton clear = UiTestSupport.findBtnIn(main, "Treure", "Quitar");
        if (clear != null) {
            SwingUtilities.invokeLater(clear::doClick); UiTestSupport.sleep(600);
            log("Filters cleared");
        }
    }

    private static void testIoButtons(JFrame main) throws Exception {
        AbstractButton tog = UiTestSupport.findBtnIn(main, "Filtre", "Filter");
        if (tog != null && (tog.getText() == null || !tog.getText().contains("▲"))) {
            SwingUtilities.invokeLater(tog::doClick); UiTestSupport.sleep(400);
        }
        String[] safe = {"Exportar", "Importar", "Fetch", "Escanejar", "Backup", "Restaurar"};
        for (String hint : safe) {
            AbstractButton btn = UiTestSupport.findBtnIn(main, hint);
            if (btn == null) { warn("I/O button not found: " + hint); continue; }
            log("CLICK I/O: \"" + btn.getText() + "\"");
            if (UiTestSupport.norm(btn.getText()).contains("export") || UiTestSupport.norm(btn.getText()).contains("import")) {
                SwingUtilities.invokeLater(btn::doClick); UiTestSupport.sleep(400);
                AbstractButton item = UiTestSupport.findBtnIn(main, "CSV", "JSON", "HTML");
                if (item != null) { SwingUtilities.invokeLater(item::doClick); UiTestSupport.sleep(500); }
            } else {
                SwingUtilities.invokeLater(btn::doClick); UiTestSupport.sleep(600);
            }
            dismissAllDialogs();
            UiTestSupport.pressEscape(robot);
            UiTestSupport.sleep(250);
        }
    }

    private static final java.util.Random RNG = new java.util.Random();

    private static String rand(String prefix) {
        return prefix + "_" + Integer.toHexString(RNG.nextInt(0xFFFF));
    }

    /** Dot-decimal strings — GuardarLlibresDialogoControl uses Double.parseDouble (locale-independent). */
    private static String randDecimal(int scale, double max) {
        return String.format(java.util.Locale.US, "%." + scale + "f", RNG.nextDouble() * max);
    }

    /** True once no visible dialog has the given title (save succeeded and dialog closed). */
    private static boolean waitForDialogTitleGone(String title, int maxMs) throws Exception {
        for (int waited = 0; waited < maxMs; waited += 100) {
            JDialog d = UiTestSupport.getTopDialog();
            if (d == null || !title.equals(d.getTitle())) return true;
            UiTestSupport.sleep(100);
        }
        return false;
    }

    /** Open new-book dialog, fill every field with random data, save. Returns true if a row appears in the table. */
    private static boolean testCreateBook(JFrame mainFrame) throws Exception {
        log("\n--- CREATE BOOK (random data) ---");
        AbstractButton nouBtn = UiTestSupport.findBtnIn((Container)mainFrame, "Afegir", "Nou", "New");
        if (nouBtn == null) { warn("Add-book button not found"); return false; }

        SwingUtilities.invokeLater(nouBtn::doClick); UiTestSupport.sleep(900);
        JDialog dlg = UiTestSupport.getTopDialog();
        if (dlg == null) { warn("New-book dialog did not appear"); return false; }
        String dlgTitle = dlg.getTitle();
        log("Dialog: \"" + dlgTitle + "\"");

        // ISBN must be numeric and unique — use timestamp suffix
        long testIsbn = 9780000000000L + (System.currentTimeMillis() % 1_000_000L);

        String[][] fields = {
            {"ISBN",       String.valueOf(testIsbn)},
            {"Títol",      rand("Títol")},
            {"Autor",      rand("Autor")},
            {"Any",        String.valueOf(1900 + RNG.nextInt(125))},
            {"Descripció", rand("Desc")},
            {"Valoració",  randDecimal(1, 10)},
            {"Preu",       randDecimal(2, 50)},
            {"Editorial",  rand("Edit")},
            {"Sèrie",      rand("Serie")},
            {"Volum",      String.valueOf(RNG.nextInt(10) + 1)},
            {"Idioma",     "Català"},
            {"Portada",    ""},
        };

        for (String[] pair : fields) {
            UiTestSupport.setFieldNear(dlg, pair[0], pair[1]);
        }

        // Check Llegit checkbox randomly
        JCheckBox chkLlegit = UiTestSupport.findCheckBox(dlg, "Llegit");
        if (chkLlegit != null && RNG.nextBoolean()) {
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(true));
            log("  Set Llegit = true");
        }

        log("Fields filled. Clicking Save...");

        AbstractButton saveBtn = UiTestSupport.findBtnIn((Container)dlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) {
            warn("Save button not found — cancelling");
            SwingUtilities.invokeLater(dlg::dispose);
            return false;
        }
        SwingUtilities.invokeLater(saveBtn::doClick);
        if (!waitForDialogTitleGone(dlgTitle, 2500)) {
            warn("Create-book dialog still open after save (validation may have failed silently in test mode)");
            collectAndLog((Container)dlg);
            SwingUtilities.invokeLater(dlg::dispose);
            UiTestSupport.sleep(400);
            log("CREATE BOOK test failed. ISBN=" + testIsbn);
            return false;
        }
        UiTestSupport.sleep(400);
        log("OK: Book saved (ISBN=" + testIsbn + ")");
        log("CREATE BOOK test done. ISBN=" + testIsbn);
        return true;
    }

    /** Open row 0, enter edit mode, change every editable field with random data, save. */
    private static void testEditBook(JFrame mainFrame) throws Exception {
        log("\n--- EDIT BOOK (random data, row 0) ---");

        // Reset to all books view
        AbstractButton allBtn = UiTestSupport.findBtnIn((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); UiTestSupport.sleep(700); }

        cmdOpenRow(mainFrame, 0);
        JDialog detailsDlg = UiTestSupport.getTopDialog();
        if (detailsDlg == null) { log("WARN: No details dialog for row 0"); return; }
        log("Details dialog: \"" + detailsDlg.getTitle() + "\"");

        AbstractButton editBtn = UiTestSupport.findBtnIn((Container)detailsDlg, "Editar", "Edit");
        if (editBtn == null) { log("WARN: Edit button not found"); SwingUtilities.invokeLater(detailsDlg::dispose); return; }
        SwingUtilities.invokeLater(editBtn::doClick); UiTestSupport.sleep(600);
        log("Edit mode active — filling fields with random data...");

        String[][] fields = {
            {"Títol",      rand("Títol")},
            {"Autor",      rand("Autor")},
            {"Any",        String.valueOf(1900 + RNG.nextInt(125))},
            {"Descripció", rand("Desc")},
            {"Valoració",  randDecimal(1, 10)},
            {"Preu",       randDecimal(2, 50)},
            {"Editorial",  rand("Edit")},
            {"Sèrie",      rand("Serie")},
            {"Volum",      String.valueOf(RNG.nextInt(10) + 1)},
            {"Idioma",     "Català"},
        };

        for (String[] pair : fields) {
            UiTestSupport.setFieldNear(detailsDlg, pair[0], pair[1]);
        }

        JCheckBox chkLlegit = UiTestSupport.findCheckBox(detailsDlg, "Llegit");
        if (chkLlegit != null) {
            boolean newVal = !chkLlegit.isSelected();
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(newVal));
            log("  Toggled Llegit -> " + newVal);
        }

        AbstractButton saveBtn = UiTestSupport.findBtnIn((Container)detailsDlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) { log("WARN: Save button not found — pressing Escape"); UiTestSupport.pressEscape(robot); UiTestSupport.sleep(300); return; }
        log("Clicking Save...");
        SwingUtilities.invokeLater(saveBtn::doClick); UiTestSupport.sleep(1000);

        JDialog afterDlg = UiTestSupport.getTopDialog();
        if (afterDlg != null) {
            log("RESULT dialog after edit save: \"" + afterDlg.getTitle() + "\"");
            collectAndLog((Container)afterDlg);
            SwingUtilities.invokeLater(afterDlg::dispose); UiTestSupport.sleep(400);
        } else {
            log("OK: No error dialog — edit likely saved successfully");
        }
        JDialog still = UiTestSupport.getTopDialog();
        if (still != null) { SwingUtilities.invokeLater(still::dispose); UiTestSupport.sleep(300); }
        log("EDIT BOOK test done.");
    }

    // ── Component traversal helpers (only the ones still specific to UIAudit) ─

    private static void collectAndLog(Container c) {
        List<String> items = new ArrayList<>();
        UiTestSupport.collectComponents(c, "  ", items);
        items.forEach(UIAudit::log);
    }

    // ── Logging ────────────────────────────────────────────────────────────────

    private static void log(String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
        reportFile.println(line);
        // Also mirror to stdout so the user can read it
        System.out.println(line);
    }

    private static void print(String msg) {
        System.out.println(msg);
        reportFile.println(msg);
    }

    private static void printHelp() {
        print("""
            Commands:
              windows             list all open windows
              scan [title]        list all visible components in a window
              click <text>        click button whose text contains <text>
              type <text>         paste <text> into focused component
              clear               clear focused text field
              focus <label>       click text field near matching label
              close               dispose topmost dialog
              enter               press Enter
              esc                 press Escape
              wait [ms]           sleep (default 500ms)
              rows                print first 10 table rows
              open-row <n>        double-click row n in the table
              auto                run full automated audit
              exit                quit
            """);
    }

    private static void writeJsonReport() {
        try (PrintWriter jw = new PrintWriter(new FileWriter("checkBiblio/audit_report.json", false), true)) {
            jw.printf("{\"fail\":%d,\"warn\":%d,\"timestamp\":\"%s\"}%n",
                failCount, warnCount, LocalDateTime.now());
        } catch (IOException e) {
            log("WARN: could not write audit_report.json: " + e.getMessage());
        }
    }

    private static void closeReport() {
        log("=== UIAudit finished ===");
        writeJsonReport();
        reportFile.close();
    }
}
