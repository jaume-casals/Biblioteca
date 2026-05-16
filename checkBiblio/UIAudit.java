package checkBiblio;

/**
 * Biblioteca UI Audit Tool
 *
 * Launches the real Biblioteca app in-process and exposes an interactive
 * command console so an external operator (human or AI) can drive the UI,
 * inspect every component, click buttons, fill fields, take screenshots,
 * and run a built-in automated walkthrough.
 *
 * Compile:  javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar \
 *                 checkBiblio/UIAudit.java -d bin
 * Run:      java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar \
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
 *   screenshot [name]         save screenshot to checkBiblio/screen_<name>.png
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;

public class UIAudit {

    // ── State ──────────────────────────────────────────────────────────────────
    private static Robot robot;
    private static PrintWriter reportFile;
    private static int screenshotSeq = 0;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        boolean autoMode = Arrays.asList(args).contains("--auto");

        Files.createDirectories(Path.of("checkBiblio/screenshots"));
        reportFile = new PrintWriter(new FileWriter("checkBiblio/audit_report.txt", false), true);

        robot = new Robot();
        robot.setAutoDelay(80);

        log("=== Biblioteca UIAudit ===");
        log("Mode: " + (autoMode ? "AUTOMATED" : "INTERACTIVE"));
        log("Started: " + LocalDateTime.now());
        log("Screenshots -> checkBiblio/screenshots/");

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
        JFrame mainFrame = waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: Main window never appeared. Aborting.");
            closeReport();
            System.exit(1);
        }
        log("OK: Main window visible — \"" + mainFrame.getTitle() + "\"");
        sleep(600);

        if (autoMode) {
            runAutomated(mainFrame);
        } else {
            runInteractive(mainFrame);
        }
        closeReport();
        System.exit(0);
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
                    case "screenshot" -> cmdScreenshot(arg.isEmpty() ? "manual" : arg);
                    case "close"      -> cmdClose();
                    case "enter"      -> { robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER); }
                    case "esc"        -> { robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE); }
                    case "wait"       -> sleep(arg.isEmpty() ? 500 : Long.parseLong(arg));
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
            String title = windowTitle(w);
            print("  [" + w.getClass().getSimpleName() + "] \"" + title + "\" size=" + w.getSize());
        }
        if (visible == 0) print("  (no visible windows)");
    }

    private static void cmdScan(String titleFragment) {
        Window w = titleFragment == null ? getTopWindow() : findWindowByTitle(titleFragment);
        if (w == null) { print("No window found" + (titleFragment != null ? ": " + titleFragment : "")); return; }
        print("Scanning: \"" + windowTitle(w) + "\"");
        List<String> found = new ArrayList<>();
        collectComponents((Container)w, "", found);
        found.forEach(UIAudit::print);
        log("SCAN: " + found.size() + " components in \"" + windowTitle(w) + "\"");
    }

    private static void cmdClick(String text) throws Exception {
        if (text.isEmpty()) { print("Usage: click <button_text>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton btn = findButtonContaining((Container)w, text);
            if (btn != null) {
                print("Clicking \"" + btn.getText() + "\" in \"" + windowTitle(w) + "\"");
                log("CLICK: \"" + btn.getText() + "\"");
                clickComponent(btn);
                sleep(400);
                // Report any new dialog
                JDialog d = getTopDialog();
                if (d != null) print("  → Dialog appeared: \"" + d.getTitle() + "\"");
                return;
            }
        }
        print("No button found matching: \"" + text + "\"");
        log("WARN: button not found: \"" + text + "\"");
    }

    private static void cmdType(String text) throws AWTException {
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
        robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);     robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_DELETE);  robot.keyRelease(KeyEvent.VK_DELETE);
        print("Cleared focused field.");
    }

    private static void cmdFocus(String labelHint) throws Exception {
        if (labelHint.isEmpty()) { print("Usage: focus <label_text>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JTextField tf = findTextFieldNear((Container)w, labelHint);
            if (tf != null) {
                clickComponent(tf);
                print("Focused text field near label \"" + labelHint + "\"");
                log("FOCUS: field near \"" + labelHint + "\"");
                return;
            }
        }
        print("No text field found near label: \"" + labelHint + "\"");
    }

    private static void cmdScreenshot(String name) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filename = "checkBiblio/screenshots/screen_" + (++screenshotSeq) + "_" + safe + ".png";
        BufferedImage img = robot.createScreenCapture(
            new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ImageIO.write(img, "PNG", new File(filename));
        print("Screenshot: " + filename);
        log("SCREENSHOT: " + filename);
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
        JTable t = findComponent((Container)mainFrame, JTable.class);
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
        JTable table = findComponent((Container)mainFrame, JTable.class);
        if (table == null) { print("No JTable found."); return; }
        if (rowIndex >= table.getRowCount()) { print("Row " + rowIndex + " out of range (max " + (table.getRowCount()-1) + ")."); return; }

        SwingUtilities.invokeAndWait(() -> {
            table.setRowSelectionInterval(rowIndex, rowIndex);
            table.scrollRectToVisible(table.getCellRect(rowIndex, 0, true));
            table.requestFocusInWindow();
        });
        sleep(200);

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
        sleep(800);

        JDialog dlg = getTopDialog();
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
        cmdScreenshot("00_start");

        // --- Main window scan ---
        log("\n--- MAIN WINDOW ---");
        collectAndLog((Container)mainFrame);

        // --- Sidebar buttons ---
        log("\n--- SIDEBAR BUTTONS ---");
        String[][] sidebar = {
            {"Tots els",   "Tots els Llibres"},
            {"Afegits r",  "Afegits Recentment"},
            {"Llegits",    "Llegits"},
            {"Estad",      "Estadistiques"},      // accent-normalized match
            {"aleatori",   "Aleatori"},
            {"llistes",    "Gestio de Llistes"},  // accent-normalized match
            {"Configur",   "Configuracio"},       // accent-normalized match
            {"Sobre",      "Sobre"},
        };
        for (String[] entry : sidebar) {
            AbstractButton btn = findButtonContaining((Container)mainFrame, entry[0]);
            if (btn != null) {
                log("CLICK: sidebar \"" + btn.getText() + "\"");
                SwingUtilities.invokeLater(btn::doClick);
                sleep(900);
                JDialog d = getTopDialog();
                if (d != null) {
                    log("  -> Dialog: \"" + d.getTitle() + "\" — scanning...");
                    collectAndLog((Container)d);
                    cmdScreenshot("sidebar_" + entry[0]);
                    SwingUtilities.invokeLater(d::dispose);
                    sleep(400);
                } else {
                    log("  -> No dialog (panel update)");
                    cmdScreenshot("sidebar_" + entry[0]);
                }
            } else {
                log("WARN: sidebar button not found for hint \"" + entry[0] + "\"");
            }
        }

        // --- Filter drawer ---
        log("\n--- FILTER DRAWER ---");
        AbstractButton tog = findButtonContaining((Container)mainFrame, "Filtre", "Filter");
        if (tog != null) {
            SwingUtilities.invokeLater(tog::doClick); sleep(400);
            log("Filter drawer toggled open");
            cmdScreenshot("filter_open");
            collectAndLog((Container)mainFrame);
            SwingUtilities.invokeLater(tog::doClick); sleep(300);
        } else {
            log("WARN: filter toggle button not found");
        }

        // Reset to all books before table inspection
        AbstractButton allBtn = findButtonContaining((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); sleep(600); }

        // --- Table inspection ---
        log("\n--- TABLE ---");
        JTable table = findComponent((Container)mainFrame, JTable.class);
        if (table != null) {
            TableModel m = table.getModel();
            log("Rows: " + m.getRowCount() + "  Cols: " + m.getColumnCount());
            StringBuilder header = new StringBuilder("Columns: ");
            for (int c = 0; c < m.getColumnCount(); c++) header.append("[").append(m.getColumnName(c)).append("] ");
            log(header.toString());
        } else {
            log("WARN: No JTable found in main frame");
        }

        // --- Open first book ---
        if (table != null && table.getRowCount() > 0) {
            log("\n--- BOOK DETAILS (row 0) ---");
            cmdOpenRow(mainFrame, 0);
            JDialog detailsDlg = getTopDialog();
            if (detailsDlg != null) {
                cmdScreenshot("book_details");
                collectAndLog((Container)detailsDlg);

                // Try Edit
                AbstractButton editBtn = findButtonContaining((Container)detailsDlg, "Editar", "Edit");
                if (editBtn != null) {
                    log("Clicking Edit...");
                    SwingUtilities.invokeLater(editBtn::doClick); sleep(500);
                    cmdScreenshot("book_edit_mode");
                    log("Edit mode active. Pressing Escape to cancel.");
                    robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE);
                    sleep(300);
                } else {
                    log("WARN: Edit button not found in details dialog");
                }

                // Tags button
                AbstractButton tagsBtn = findButtonContaining((Container)detailsDlg, "Etiqueta", "Tag");
                if (tagsBtn != null) {
                    log("Clicking Tags...");
                    SwingUtilities.invokeLater(tagsBtn::doClick); sleep(600);
                    JDialog tagsDlg = getTopDialog();
                    if (tagsDlg != null) {
                        log("  -> Tags dialog: \"" + tagsDlg.getTitle() + "\"");
                        cmdScreenshot("book_tags");
                        SwingUtilities.invokeLater(tagsDlg::dispose); sleep(300);
                    }
                }

                SwingUtilities.invokeLater(detailsDlg::dispose); sleep(400);
            } else {
                log("WARN: No dialog appeared after opening row 0");
            }
        } else {
            log("WARN: Table empty, skipping book detail test");
        }

        // --- Create new book with random data ---
        testCreateBook(mainFrame);

        // --- Edit first book, change all fields ---
        testEditBook(mainFrame);

        cmdScreenshot("99_final");
        log("\n=== AUTOMATED AUDIT COMPLETE ===");
        log("Report: checkBiblio/audit_report.txt");
        log("Screenshots: checkBiblio/screenshots/");
        print("\n[AUTO] Audit complete. Check checkBiblio/audit_report.txt");
    }

    private static final java.util.Random RNG = new java.util.Random();

    private static String rand(String prefix) {
        return prefix + "_" + Integer.toHexString(RNG.nextInt(0xFFFF));
    }

    /** Open new-book dialog, fill every field with random data, save. */
    private static void testCreateBook(JFrame mainFrame) throws Exception {
        log("\n--- CREATE BOOK (random data) ---");
        AbstractButton nouBtn = findButtonContaining((Container)mainFrame, "Afegir", "Nou", "New");
        if (nouBtn == null) { log("WARN: Add-book button not found"); return; }

        SwingUtilities.invokeLater(nouBtn::doClick); sleep(900);
        JDialog dlg = getTopDialog();
        if (dlg == null) { log("WARN: New-book dialog did not appear"); return; }
        log("Dialog: \"" + dlg.getTitle() + "\"");
        cmdScreenshot("create_book_empty");

        // ISBN must be numeric and unique — use timestamp suffix
        long testIsbn = 9780000000000L + (System.currentTimeMillis() % 1_000_000L);

        String[][] fields = {
            {"ISBN",       String.valueOf(testIsbn)},
            {"Títol",      rand("Títol")},
            {"Autor",      rand("Autor")},
            {"Any",        String.valueOf(1900 + RNG.nextInt(125))},
            {"Descripció", rand("Desc")},
            {"Valoració",  String.format("%.1f", RNG.nextDouble() * 10)},
            {"Preu",       String.format("%.2f", RNG.nextDouble() * 50)},
            {"Editorial",  rand("Edit")},
            {"Sèrie",      rand("Serie")},
            {"Volum",      String.valueOf(RNG.nextInt(10) + 1)},
            {"Idioma",     "Català"},
            {"Portada",    ""},
        };

        for (String[] pair : fields) {
            fillField(dlg, pair[0], pair[1]);
        }

        // Check Llegit checkbox randomly
        JCheckBox chkLlegit = findCheckBox(dlg, "Llegit");
        if (chkLlegit != null && RNG.nextBoolean()) {
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(true));
            log("  Set Llegit = true");
        }

        cmdScreenshot("create_book_filled");
        log("Fields filled. Clicking Save...");

        AbstractButton saveBtn = findButtonContaining((Container)dlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) { log("WARN: Save button not found — cancelling"); SwingUtilities.invokeLater(dlg::dispose); return; }
        SwingUtilities.invokeLater(saveBtn::doClick); sleep(1000);

        JDialog afterDlg = getTopDialog();
        if (afterDlg != null) {
            log("RESULT dialog after save: \"" + afterDlg.getTitle() + "\"");
            collectAndLog((Container)afterDlg);
            cmdScreenshot("create_book_result");
            SwingUtilities.invokeLater(afterDlg::dispose); sleep(400);
        } else {
            log("OK: No error dialog — book likely saved (ISBN=" + testIsbn + ")");
        }
        // If main dialog still open, close it
        JDialog still = getTopDialog();
        if (still != null) { SwingUtilities.invokeLater(still::dispose); sleep(300); }
        log("CREATE BOOK test done. ISBN=" + testIsbn);
    }

    /** Open row 0, enter edit mode, change every editable field with random data, save. */
    private static void testEditBook(JFrame mainFrame) throws Exception {
        log("\n--- EDIT BOOK (random data, row 0) ---");

        // Reset to all books view
        AbstractButton allBtn = findButtonContaining((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); sleep(700); }

        cmdOpenRow(mainFrame, 0);
        JDialog detailsDlg = getTopDialog();
        if (detailsDlg == null) { log("WARN: No details dialog for row 0"); return; }
        log("Details dialog: \"" + detailsDlg.getTitle() + "\"");

        AbstractButton editBtn = findButtonContaining((Container)detailsDlg, "Editar", "Edit");
        if (editBtn == null) { log("WARN: Edit button not found"); SwingUtilities.invokeLater(detailsDlg::dispose); return; }
        SwingUtilities.invokeLater(editBtn::doClick); sleep(600);
        cmdScreenshot("edit_book_before");
        log("Edit mode active — filling fields with random data...");

        String[][] fields = {
            {"Títol",      rand("Títol")},
            {"Autor",      rand("Autor")},
            {"Any",        String.valueOf(1900 + RNG.nextInt(125))},
            {"Descripció", rand("Desc")},
            {"Valoració",  String.format("%.1f", RNG.nextDouble() * 10)},
            {"Preu",       String.format("%.2f", RNG.nextDouble() * 50)},
            {"Editorial",  rand("Edit")},
            {"Sèrie",      rand("Serie")},
            {"Volum",      String.valueOf(RNG.nextInt(10) + 1)},
            {"Idioma",     "Català"},
        };

        for (String[] pair : fields) {
            fillField(detailsDlg, pair[0], pair[1]);
        }

        JCheckBox chkLlegit = findCheckBox(detailsDlg, "Llegit");
        if (chkLlegit != null) {
            boolean newVal = !chkLlegit.isSelected();
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(newVal));
            log("  Toggled Llegit -> " + newVal);
        }

        cmdScreenshot("edit_book_filled");

        AbstractButton saveBtn = findButtonContaining((Container)detailsDlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) { log("WARN: Save button not found — pressing Escape"); robot.keyPress(KeyEvent.VK_ESCAPE); robot.keyRelease(KeyEvent.VK_ESCAPE); sleep(300); return; }
        log("Clicking Save...");
        SwingUtilities.invokeLater(saveBtn::doClick); sleep(1000);

        JDialog afterDlg = getTopDialog();
        if (afterDlg != null) {
            log("RESULT dialog after edit save: \"" + afterDlg.getTitle() + "\"");
            collectAndLog((Container)afterDlg);
            cmdScreenshot("edit_book_result");
            SwingUtilities.invokeLater(afterDlg::dispose); sleep(400);
        } else {
            log("OK: No error dialog — edit likely saved successfully");
        }
        JDialog still = getTopDialog();
        if (still != null) { SwingUtilities.invokeLater(still::dispose); sleep(300); }
        log("EDIT BOOK test done.");
    }

    /** Clear a text field near a label and type new value. */
    private static void fillField(Container dlg, String labelHint, String value) throws Exception {
        if (value.isEmpty()) return;
        JTextField tf = findTextFieldNear(dlg, labelHint);
        if (tf == null) { log("  WARN: field not found for label \"" + labelHint + "\""); return; }
        SwingUtilities.invokeAndWait(() -> {
            tf.requestFocusInWindow();
            tf.selectAll();
            tf.setText(value);
        });
        log("  Set \"" + labelHint + "\" = \"" + value + "\"");
        sleep(60);
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

    // ── Component traversal helpers ───────────────────────────────────────────

    private static void collectAndLog(Container c) {
        List<String> items = new ArrayList<>();
        collectComponents(c, "  ", items);
        items.forEach(UIAudit::log);
    }

    private static void collectComponents(Container c, String indent, List<String> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            if (comp instanceof AbstractButton btn && !(btn instanceof JCheckBox)) {
                out.add(indent + "[BTN] \"" + btn.getText() + "\"" +
                    (btn.getToolTipText() != null ? "  tip:\"" + btn.getToolTipText() + "\"" : ""));
            } else if (comp instanceof JCheckBox chk) {
                out.add(indent + "[CHK] \"" + chk.getText() + "\" selected=" + chk.isSelected());
            } else if (comp instanceof JTextField tf) {
                out.add(indent + "[TF ] value=\"" + tf.getText() + "\"" +
                    (tf.getToolTipText() != null ? "  tip:\"" + tf.getToolTipText() + "\"" : ""));
            } else if (comp instanceof JLabel lbl && lbl.getText() != null && !lbl.getText().isBlank()) {
                out.add(indent + "[LBL] \"" + lbl.getText() + "\"");
            } else if (comp instanceof JComboBox<?> cb) {
                out.add(indent + "[CMB] selected=\"" + cb.getSelectedItem() + "\" items=" + cb.getItemCount());
            } else if (comp instanceof JTable tbl) {
                out.add(indent + "[TBL] rows=" + tbl.getRowCount() + " cols=" + tbl.getColumnCount());
            }
            if (comp instanceof Container sub) collectComponents(sub, indent + "  ", out);
        }
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

    private static AbstractButton findButtonContaining(Container c, String... texts) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton btn && btn.isVisible()) {
                String t = btn.getText();
                if (t != null) for (String text : texts)
                    if (norm(t).contains(norm(text))) return btn;
            }
            if (comp instanceof Container sub) {
                AbstractButton found = findButtonContaining(sub, texts);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Lowercase + strip diacritics so "estadist" matches "Estadístiques". */
    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .toLowerCase();
    }

    private static JTextField findTextFieldNear(Container c, String labelHint) {
        // Walk all components; when we see a label matching the hint, return the next JTextField
        List<Component> flat = new ArrayList<>();
        flattenVisible(c, flat);
        for (int i = 0; i < flat.size(); i++) {
            Component comp = flat.get(i);
            if (comp instanceof JLabel lbl && lbl.getText() != null
                    && norm(lbl.getText()).contains(norm(labelHint))) {
                // Find next JTextField
                for (int j = i + 1; j < Math.min(i + 6, flat.size()); j++) {
                    if (flat.get(j) instanceof JTextField tf) return tf;
                }
            }
        }
        return null;
    }

    private static void flattenVisible(Container c, List<Component> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            out.add(comp);
            if (comp instanceof Container sub) flattenVisible(sub, out);
        }
    }

    // ── Window helpers ─────────────────────────────────────────────────────────

    private static JFrame waitForMainFrame(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame f && f.isVisible() && !f.getTitle().isBlank()) return f;
            }
            Thread.sleep(150);
        }
        return null;
    }

    private static JDialog waitForDialog(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isVisible()) return d;
            }
            Thread.sleep(100);
        }
        return null;
    }

    private static Window getTopWindow() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i].isVisible()) return ws[i];
        return null;
    }

    private static JDialog getTopDialog() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible()) return d;
        return null;
    }

    private static Window findWindowByTitle(String fragment) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            if (windowTitle(w).toLowerCase().contains(fragment.toLowerCase())) return w;
        }
        return null;
    }

    private static String windowTitle(Window w) {
        if (w instanceof JFrame f) return f.getTitle();
        if (w instanceof JDialog d) return d.getTitle();
        return w.getClass().getSimpleName();
    }

    // ── Robot helpers ──────────────────────────────────────────────────────────

    private static void clickComponent(Component c) throws AWTException {
        if (!c.isShowing()) return;
        Point loc = c.getLocationOnScreen();
        int cx = loc.x + c.getWidth() / 2;
        int cy = loc.y + c.getHeight() / 2;
        robot.mouseMove(cx, cy);
        sleep(60);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
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
              screenshot [name]   save screenshot
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

    private static void closeReport() {
        log("=== UIAudit finished ===");
        reportFile.close();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
