package checkBiblio;

/**
 * Eina d'auditoria de la UI de Biblioteca
 *
 * Llança l'aplicació real de Biblioteca en procés i exposa una consola
 * interactiva d'ordres perquè un operador extern (humà o IA) pugui
 * manegar la UI, inspeccionar cada component, clicar botons, omplir
 * camps i executar un recorregut automatitzat predefinit.
 *
 * Compila:  javac -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio/UiTestSupport.java checkBiblio/UIAudit.java checkBiblio/I18nAudit.java -d bin
 * Executa:  java  -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar \
 *                 checkBiblio.UIAudit [--auto]
 *
 * Ordres (mode interactiu):
 *   help                      mostra aquesta llista
 *   windows                   llista totes les finestres obertes
 *   scan [title]              llista tots els botons/etiquetes/camps d'una finestra
 *   click <text>              clica el primer botó el text del qual contingui <text>
 *   type <text>               escriu text al component amb el focus actual
 *   clear                     buida el camp de text amb el focus (Ctrl+A, Delete)
 *   focus <text>              clica un camp de text l'etiqueta del qual contingui <text>
 *   close                     tanca el diàleg visible més alt
 *   enter                     prem Enter
 *   esc                       prem Escape
 *   wait [ms]                 dorm N ms (per defecte 500)
 *   auto                      executa l'auditoria automatitzada completa
 *   rows                      imprimeix les 10 primeres files de la JTable visible
 *   open-row <n>              doble clic a la fila n de la JTable (0-based)
 *   exit                      surt
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

    // ── Estat ──────────────────────────────────────────────────────────────────
    private static Robot robot;
    private static PrintWriter reportFile;
    private static int warnCount = 0;
    private static int failCount = 0;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // ── Punt d'entrada ────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        boolean autoMode = Arrays.asList(args).contains("--auto");

        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            System.getProperty("biblioteca.h2.url",
                "jdbc:h2:mem:uiAudit;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"));

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("FATAL: Entorn headless — cal Robot. Instal·la Xvfb o defineix DISPLAY.");
            System.exit(1);
        }

        reportFile = new PrintWriter(new FileWriter("checkBiblio/audit_report.txt", false), true);

        robot = new Robot();
        robot.setAutoDelay(80);

        log("=== Biblioteca UIAudit ===");
        log("Mode: " + (autoMode ? "AUTOMATITZAT" : "INTERACTIU"));
        log("Iniciat: " + LocalDateTime.now());

        // ── Llança l'app (força mode Swing via --swing, evita el ModeSelectorDialog) ──
        log("Llançant Biblioteca en mode Swing...");
        Thread appThread = new Thread(() -> {
            try { main.Executable.main(new String[]{"--swing"}); }
            catch (Exception e) { log("APP LAUNCH ERROR: " + e); }
        }, "biblioteca-main");
        appThread.setDaemon(true);
        appThread.start();

        // ── Espera el marc principal ──────────────────────────────────────────
        log("Esperant el JFrame principal (fins a 12s)...");
        JFrame mainFrame = SuportTestUi.waitForMainFrame(12000);
        if (mainFrame == null) {
            log("FATAL: La finestra principal no ha aparegut mai. Avorto.");
            closeReport();
            System.exit(1);
        }
        log("OK: Finestra principal visible — \"" + mainFrame.getTitle() + "\"");
        SuportTestUi.sleep(600);

        if (autoMode) {
            runAutomated(mainFrame);
        } else {
            runInteractive(mainFrame);
        }
        closeReport();
        System.exit(failCount > 0 ? 1 : 0);
    }

    // ── Consola interactiva ──────────────────────────────────────────────────
    private static void runInteractive(JFrame mainFrame) throws Exception {
        print("");
        print("======================================================");
        print("  Biblioteca UIAudit — Consola interactiva");
        print("  Escriu 'help' per veure les ordres, 'exit' per sortir.");
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
                    case "exit"       -> { log("Sortint."); return; }
                    case "help"       -> printHelp();
                    case "windows"    -> cmdWindows();
                    case "scan"       -> cmdScan(arg.isEmpty() ? null : arg);
                    case "click"      -> cmdClick(arg);
                    case "type"       -> cmdType(arg);
                    case "clear"      -> cmdClear();
                    case "focus"      -> cmdFocus(arg);
                    case "close"      -> cmdClose();
                    case "enter"      -> SuportTestUi.pressEnter(robot);
                    case "esc"        -> SuportTestUi.pressEscape(robot);
                    case "wait"       -> SuportTestUi.sleep(arg.isEmpty() ? 500 : Long.parseLong(arg));
                    case "auto"       -> runAutomated(mainFrame);
                    case "rows"       -> cmdRows(mainFrame);
                    case "open-row"   -> cmdOpenRow(mainFrame, Integer.parseInt(arg));
                    default           -> print("Ordre desconeguda: " + cmd + " (escriu 'help')");
                }
            } catch (Exception e) {
                print("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                log("ERROR [" + line + "]: " + e);
            }
        }
    }

    // ── Ordres ────────────────────────────────────────────────────────────────

    private static void cmdWindows() {
        Window[] all = Window.getWindows();
        int visible = 0;
        for (Window w : all) {
            if (!w.isVisible()) continue;
            visible++;
            String title = SuportTestUi.windowTitle(w);
            print("  [" + w.getClass().getSimpleName() + "] \"" + title + "\" mida=" + w.getSize());
        }
        if (visible == 0) print("  (cap finestra visible)");
    }

    private static void cmdScan(String titleFragment) {
        Window w = titleFragment == null
            ? SuportTestUi.getTopWindow()
            : SuportTestUi.findWindowByTitle(titleFragment);
        if (w == null) { print("No s'ha trobat la finestra" + (titleFragment != null ? ": " + titleFragment : "")); return; }
        print("Escanejant: \"" + SuportTestUi.windowTitle(w) + "\"");
        List<String> found = new ArrayList<>();
        SuportTestUi.collectComponents((Container)w, "", found);
        found.forEach(UIAudit::print);
        log("SCAN: " + found.size() + " components a \"" + SuportTestUi.windowTitle(w) + "\"");
    }

    private static void cmdClick(String text) throws Exception {
        if (text.isEmpty()) { print("Ús: click <text_del_botó>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton btn = SuportTestUi.findBtnIn((Container)w, text);
            if (btn != null) {
                print("Clicant \"" + btn.getText() + "\" a \"" + SuportTestUi.windowTitle(w) + "\"");
                log("CLICK: \"" + btn.getText() + "\"");
                SuportTestUi.clickComponent(robot, btn);
                SuportTestUi.sleep(400);
                // Informa de qualsevol diàleg nou
                JDialog d = SuportTestUi.getTopDialog();
                if (d != null) print("  → Ha aparegut un diàleg: \"" + d.getTitle() + "\"");
                return;
            }
        }
        print("No s'ha trobat cap botó que coincideixi amb: \"" + text + "\"");
        log("WARN: botó no trobat: \"" + text + "\"");
    }

    private static void cmdType(String text) {
        print("Escrivint: " + text);
        log("TYPE: " + text);
        // Usa el portapapers per fiabilitat amb caràcters no-ASCII
        java.awt.datatransfer.Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new java.awt.datatransfer.StringSelection(text), null);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    private static void cmdClear() {
        SuportTestUi.pressCtrlA(robot);
        SuportTestUi.pressDelete(robot);
        print("S'ha buidat el camp amb el focus.");
    }

    private static void cmdFocus(String labelHint) throws Exception {
        if (labelHint.isEmpty()) { print("Ús: focus <text_etiqueta>"); return; }
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JTextField tf = SuportTestUi.findTextFieldNear((Container)w, labelHint);
            if (tf != null) {
                SuportTestUi.clickComponent(robot, tf);
                print("S'ha donat focus al camp de text proper a l'etiqueta \"" + labelHint + "\"");
                log("FOCUS: camp proper a \"" + labelHint + "\"");
                return;
            }
        }
        print("No s'ha trobat cap camp de text proper a l'etiqueta: \"" + labelHint + "\"");
    }

    private static void cmdClose() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--) {
            if (ws[i] instanceof JDialog d && d.isVisible()) {
                print("Tancant diàleg: \"" + d.getTitle() + "\"");
                log("CLOSE: \"" + d.getTitle() + "\"");
                SwingUtilities.invokeLater(d::dispose);
                return;
            }
        }
        print("No hi ha cap diàleg per tancar.");
    }

    private static void cmdRows(JFrame mainFrame) {
        JTable t = SuportTestUi.findComponent((Container)mainFrame, JTable.class);
        if (t == null) { print("No s'ha trobat cap JTable."); return; }
        TableModel m = t.getModel();
        int rows = Math.min(10, m.getRowCount());
        print("Taula: " + m.getRowCount() + " files, " + m.getColumnCount() + " columnes");
        // Capçalera
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
        if (m.getRowCount() > 10) print("  ... (" + (m.getRowCount() - 10) + " més)");
    }

    private static void cmdOpenRow(JFrame mainFrame, int rowIndex) throws Exception {
        JTable table = SuportTestUi.findComponent((Container)mainFrame, JTable.class);
        if (table == null) { print("No s'ha trobat cap JTable."); return; }
        if (rowIndex >= table.getRowCount()) { print("Fila " + rowIndex + " fora de rang (màx " + (table.getRowCount()-1) + ")."); return; }

        SwingUtilities.invokeAndWait(() -> {
            table.setRowSelectionInterval(rowIndex, rowIndex);
            table.scrollRectToVisible(table.getCellRect(rowIndex, 0, true));
            table.requestFocusInWindow();
        });
        SuportTestUi.sleep(200);

        // Dispara directament l'acció registrada "obrirDetalls" — evita Robot totalment (funciona amb Xvfb).
        // Cal invokeLater (no invokeAndWait): els diàlegs modals bloquegen l'EDT dins de setVisible(true),
        // per la qual cosa invokeAndWait es penjaria esperant que l'EDT acabi.
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
        SuportTestUi.sleep(800);

        JDialog dlg = SuportTestUi.getTopDialog();
        if (dlg != null) {
            print("Diàleg obert: \"" + dlg.getTitle() + "\"");
            log("OPEN-ROW " + rowIndex + " -> diàleg \"" + dlg.getTitle() + "\"");
        } else {
            print("No ha aparegut cap diàleg en obrir la fila " + rowIndex + ".");
        }
    }

    // ── Recorregut automatitzat complet ────────────────────────────────────────
    private static void runAutomated(JFrame mainFrame) throws Exception {
        log("\n=== INICI DE L'AUDITORIA AUTOMATITZADA ===");

        // --- Escaneig de la finestra principal ---
        log("\n--- FINESTRA PRINCIPAL ---");
        collectAndLog((Container)mainFrame);

        // --- Botons de la barra lateral ---
        log("\n--- BOTONS DE LA BARRA LATERAL ---");
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
            AbstractButton btn = SuportTestUi.findBtnIn((Container)mainFrame, entry[0]);
            if (btn != null) {
                log("CLICK: barra lateral \"" + btn.getText() + "\"");
                SwingUtilities.invokeLater(btn::doClick);
                SuportTestUi.sleep(900);
                JDialog d = SuportTestUi.getTopDialog();
                if (d != null) {
                    log("  -> Diàleg: \"" + d.getTitle() + "\" — escanejant...");
                    collectAndLog((Container)d);
                    SwingUtilities.invokeLater(d::dispose);
                    SuportTestUi.sleep(400);
                } else {
                    log("  -> Sense diàleg (actualització del panell)");
                }
            } else {
                warn("botó de barra lateral no trobat per al hint \"" + entry[0] + "\"");
            }
        }

        // --- Barra superior: cerca, galeria, sèries ---
        log("\n--- BARRA SUPERIOR ---");
        testTopBar(mainFrame);

        // --- Calaix de filtres ---
        log("\n--- CALAIX DE FILTRES ---");
        AbstractButton tog = SuportTestUi.findBtnIn((Container)mainFrame, "Filtre", "Filter");
        if (tog != null) {
            SwingUtilities.invokeLater(tog::doClick); SuportTestUi.sleep(400);
            log("Calaix de filtres obert");
            collectAndLog((Container)mainFrame);
            SwingUtilities.invokeLater(tog::doClick); SuportTestUi.sleep(300);
        } else {
            warn("botó de commutar el calaix de filtres no trobat");
        }

        testFilterActions(mainFrame);

        testPagination(mainFrame);

        testIoButtons(mainFrame);

        // Inicialitza la biblioteca per a la BD d'auditoria aïllada i exercita la taula/detalls
        boolean bookCreated = testCreateBook(mainFrame);

        // Torna a la vista de tots els llibres abans d'inspeccionar la taula
        AbstractButton allBtn = SuportTestUi.findBtnIn((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); SuportTestUi.sleep(600); }

        // --- Inspecció de la taula ---
        log("\n--- TAULA ---");
        JTable table = SuportTestUi.findComponent((Container)mainFrame, JTable.class);
        if (table != null) {
            TableModel m = table.getModel();
            log("Files: " + m.getRowCount() + "  Columnes: " + m.getColumnCount());
            StringBuilder header = new StringBuilder("Columnes: ");
            for (int c = 0; c < m.getColumnCount(); c++) header.append("[").append(m.getColumnName(c)).append("] ");
            log(header.toString());
        } else {
            warn("No s'ha trobat cap JTable a la finestra principal");
        }

        // --- Obre el primer llibre ---
        if (bookCreated && table != null && table.getRowCount() > 0) {
            log("\n--- DETALLS DEL LLIBRE (fila 0) ---");
            cmdOpenRow(mainFrame, 0);
            JDialog detailsDlg = SuportTestUi.getTopDialog();
            if (detailsDlg != null) {
                collectAndLog((Container)detailsDlg);

                // Prova Editar
                AbstractButton editBtn = SuportTestUi.findBtnIn((Container)detailsDlg, "Editar", "Edit");
                if (editBtn != null) {
                    log("Clicant Editar...");
                    SwingUtilities.invokeLater(editBtn::doClick); SuportTestUi.sleep(500);
                    log("Mode d'edició actiu. Prement Escape per cancel·lar.");
                    SuportTestUi.pressEscape(robot);
                    SuportTestUi.sleep(300);
                } else {
                    warn("Botó Editar no trobat al diàleg de detalls");
                }

                clickSubDialogButton(detailsDlg, "Llistes");
                clickSubDialogButton(detailsDlg, "Etiquetes");
                clickSubDialogButton(detailsDlg, "Historial");
                clickSubDialogButton(detailsDlg, "Imprimir");

                SwingUtilities.invokeLater(detailsDlg::dispose); SuportTestUi.sleep(400);
            } else {
                warn("No ha aparegut cap diàleg en obrir la fila 0");
            }
        } else if (!bookCreated) {
            warn("Crear llibre ha fallat, es salta el test de detalls");
        } else {
            warn("Taula buida, es salta el test de detalls");
        }

        // --- Edita el primer llibre, canvia tots els camps ---
        if (bookCreated) testEditBook(mainFrame);

        log("\n--- Auditoria estàtica d'i18n ---");
        int[] i18nFail = {0}, i18nWarn = {0};
        I18nAudit.run(reportFile, i18nFail, i18nWarn);
        failCount += i18nFail[0];
        warnCount += i18nWarn[0];
        log("\n=== AUDITORIA AUTOMATITZADA COMPLETADA ===");
        log("FAIL: " + failCount + "  WARN: " + warnCount);
        log("Informe: checkBiblio/audit_report.txt");
        writeJsonReport();
        print("\n[AUTO] Audit complete. FAIL=" + failCount + " WARN=" + warnCount
            + " — mira checkBiblio/audit_report.txt");
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
        AbstractButton btn = SuportTestUi.findBtnIn((Container)detailsDlg, btnHint);
        if (btn == null) { warn("Botó de detalls no trobat: \"" + btnHint + "\""); return; }
        log("Clicant \"" + btn.getText() + "\"...");
        SwingUtilities.invokeLater(btn::doClick);
        SuportTestUi.sleep(700);
        JDialog sub = SuportTestUi.getTopDialog();
        if (sub != null && sub != detailsDlg) {
            log("  -> Subdiàleg: \"" + sub.getTitle() + "\"");
            dismissDialog(sub);
        } else {
            log("  -> Sense subdiàleg per a \"" + btnHint + "\"");
        }
    }

    private static void dismissDialog(JDialog d) {
        AbstractButton close = SuportTestUi.findBtnIn((Container)d, "Tancar", "OK", "Cancel");
        if (close != null) SwingUtilities.invokeLater(close::doClick);
        else SwingUtilities.invokeLater(d::dispose);
        SuportTestUi.sleep(350);
    }

    private static void dismissAllDialogs() {
        for (int i = 0; i < 8; i++) {
            JDialog d = SuportTestUi.getTopDialog();
            if (d == null) break;
            dismissDialog(d);
        }
    }

    private static void testTopBar(JFrame main) throws Exception {
        JTextField search = SuportTestUi.findTextFieldNear(main, "ISBN");
        if (search == null) {
            List<Component> flat = new ArrayList<>();
            SuportTestUi.flattenVisible(main, flat);
            for (Component c : flat) {
                if (c instanceof JTextField tf && tf.getToolTipText() != null
                        && SuportTestUi.norm(tf.getToolTipText()).contains("cerca")) {
                    search = tf;
                    break;
                }
            }
        }
        if (search != null) {
            final JTextField searchField = search;
            SwingUtilities.invokeAndWait(() -> searchField.setText("test"));
            SuportTestUi.sleep(200);
            log("Barra de cerca posada a \"test\"");
            SwingUtilities.invokeAndWait(() -> searchField.setText(""));
            SuportTestUi.sleep(200);
        } else {
            warn("Barra de cerca no trobada");
        }

        AbstractButton galeria = SuportTestUi.findBtnIn(main, "Galeria");
        if (galeria != null) {
            SwingUtilities.invokeLater(galeria::doClick); SuportTestUi.sleep(700);
            log("Mode galeria activat");
            SwingUtilities.invokeLater(galeria::doClick); SuportTestUi.sleep(700);
            log("Mode galeria desactivat");
        } else {
            warn("Botó Galeria no trobat");
        }

        AbstractButton series = SuportTestUi.findBtnIn(main, "Sèrie", "Series");
        if (series != null) {
            SwingUtilities.invokeLater(series::doClick); SuportTestUi.sleep(500);
            SwingUtilities.invokeLater(series::doClick); SuportTestUi.sleep(500);
            log("Agrupació per sèries commutada x2");
        } else {
            warn("Botó Sèries no trobat");
        }
    }

    private static void testPagination(JFrame main) throws Exception {
        AbstractButton next = SuportTestUi.findBtnIn(main, "Seguent", "Next");
        AbstractButton prev = SuportTestUi.findBtnIn(main, "Anterior", "Previous");
        if (next != null) {
            SwingUtilities.invokeLater(next::doClick); SuportTestUi.sleep(600);
            log("Paginació: pàgina següent");
        } else {
            warn("Botó de pàgina següent no trobat");
        }
        if (prev != null) {
            SwingUtilities.invokeLater(prev::doClick); SuportTestUi.sleep(600);
            log("Paginació: pàgina anterior");
        }
    }

    private static void testFilterActions(JFrame main) throws Exception {
        AbstractButton tog = SuportTestUi.findBtnIn(main, "Filtre", "Filter");
        if (tog != null && (tog.getText() == null || !tog.getText().contains("▲"))) {
            SwingUtilities.invokeLater(tog::doClick); SuportTestUi.sleep(400);
        }
        SuportTestUi.setFieldNear(main, "Nom", "audit");
        AbstractButton filtrar = SuportTestUi.findBtnIn(main, "Filtrar");
        if (filtrar != null) {
            SwingUtilities.invokeLater(filtrar::doClick); SuportTestUi.sleep(700);
            log("Filtre aplicat (nom=audit)");
        }
        AbstractButton clear = SuportTestUi.findBtnIn(main, "Treure", "Quitar");
        if (clear != null) {
            SwingUtilities.invokeLater(clear::doClick); SuportTestUi.sleep(600);
            log("Filtres esborrats");
        }
    }

    private static void testIoButtons(JFrame main) throws Exception {
        AbstractButton tog = SuportTestUi.findBtnIn(main, "Filtre", "Filter");
        if (tog != null && (tog.getText() == null || !tog.getText().contains("▲"))) {
            SwingUtilities.invokeLater(tog::doClick); SuportTestUi.sleep(400);
        }
        String[] safe = {"Exportar", "Importar", "Fetch", "Escanejar", "Backup", "Restaurar"};
        for (String hint : safe) {
            AbstractButton btn = SuportTestUi.findBtnIn(main, hint);
            if (btn == null) { warn("Botó d'E/S no trobat: " + hint); continue; }
            log("CLICK I/O: \"" + btn.getText() + "\"");
            if (SuportTestUi.norm(btn.getText()).contains("export") || SuportTestUi.norm(btn.getText()).contains("import")) {
                SwingUtilities.invokeLater(btn::doClick); SuportTestUi.sleep(400);
                AbstractButton item = SuportTestUi.findBtnIn(main, "CSV", "JSON", "HTML");
                if (item != null) { SwingUtilities.invokeLater(item::doClick); SuportTestUi.sleep(500); }
            } else {
                SwingUtilities.invokeLater(btn::doClick); SuportTestUi.sleep(600);
            }
            dismissAllDialogs();
            SuportTestUi.pressEscape(robot);
            SuportTestUi.sleep(250);
        }
    }

    private static final java.util.Random RNG = new java.util.Random();

    private static String rand(String prefix) {
        return prefix + "_" + Integer.toHexString(RNG.nextInt(0xFFFF));
    }

    /** Cadenes amb punt decimal — GuardarLlibresDialogoControl fa servir Double.parseDouble (independent de la localització). */
    private static String randDecimal(int scale, double max) {
        return String.format(java.util.Locale.US, "%." + scale + "f", RNG.nextDouble() * max);
    }

    /** Cert un cop no queda cap diàleg visible amb el títol donat (desar ha funcionat i el diàleg s'ha tancat). */
    private static boolean waitForDialogTitleGone(String title, int maxMs) throws Exception {
        for (int waited = 0; waited < maxMs; waited += 100) {
            JDialog d = SuportTestUi.getTopDialog();
            if (d == null || !title.equals(d.getTitle())) return true;
            SuportTestUi.sleep(100);
        }
        return false;
    }

    /** Obre el diàleg de nou llibre, omple tots els camps amb dades aleatòries i desa. Retorna cert si apareix una fila a la taula. */
    private static boolean testCreateBook(JFrame mainFrame) throws Exception {
        log("\n--- CREAR LLIBRE (dades aleatòries) ---");
        AbstractButton nouBtn = SuportTestUi.findBtnIn((Container)mainFrame, "Afegir", "Nou", "New");
        if (nouBtn == null) { warn("Botó d'afegir llibre no trobat"); return false; }

        SwingUtilities.invokeLater(nouBtn::doClick); SuportTestUi.sleep(900);
        JDialog dlg = SuportTestUi.getTopDialog();
        if (dlg == null) { warn("El diàleg de nou llibre no ha aparegut"); return false; }
        String dlgTitle = dlg.getTitle();
        log("Diàleg: \"" + dlgTitle + "\"");

        // L'ISBN ha de ser numèric i únic — fem servir el sufix del timestamp
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
            SuportTestUi.setFieldNear(dlg, pair[0], pair[1]);
        }

        // Marca la casella Llegit a l'atzar
        JCheckBox chkLlegit = SuportTestUi.findCheckBox(dlg, "Llegit");
        if (chkLlegit != null && RNG.nextBoolean()) {
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(true));
            log("  S'ha posat Llegit = true");
        }

        log("Camps omplerts. Clicant Desa...");

        AbstractButton saveBtn = SuportTestUi.findBtnIn((Container)dlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) {
            warn("Botó Desa no trobat — cancel·lant");
            SwingUtilities.invokeLater(dlg::dispose);
            return false;
        }
        SwingUtilities.invokeLater(saveBtn::doClick);
        if (!waitForDialogTitleGone(dlgTitle, 2500)) {
            warn("El diàleg de crear llibre encara és obert després de desar (la validació pot haver fallat silenciosament en mode test)");
            collectAndLog((Container)dlg);
            SwingUtilities.invokeLater(dlg::dispose);
            SuportTestUi.sleep(400);
            log("Ha fallat el test CREAR LLIBRE. ISBN=" + testIsbn);
            return false;
        }
        SuportTestUi.sleep(400);
        log("OK: Llibre desat (ISBN=" + testIsbn + ")");
        log("Test CREAR LLIBRE acabat. ISBN=" + testIsbn);
        return true;
    }

    /** Obre la fila 0, entra en mode edició, canvia tots els camps editables amb dades aleatòries i desa. */
    private static void testEditBook(JFrame mainFrame) throws Exception {
        log("\n--- EDITAR LLIBRE (dades aleatòries, fila 0) ---");

        // Torna a la vista de tots els llibres
        AbstractButton allBtn = SuportTestUi.findBtnIn((Container)mainFrame, "Tots els");
        if (allBtn != null) { SwingUtilities.invokeLater(allBtn::doClick); SuportTestUi.sleep(700); }

        cmdOpenRow(mainFrame, 0);
        JDialog detailsDlg = SuportTestUi.getTopDialog();
        if (detailsDlg == null) { log("WARN: Cap diàleg de detalls per a la fila 0"); return; }
        log("Diàleg de detalls: \"" + detailsDlg.getTitle() + "\"");

        AbstractButton editBtn = SuportTestUi.findBtnIn((Container)detailsDlg, "Editar", "Edit");
        if (editBtn == null) { log("WARN: Botó Editar no trobat"); SwingUtilities.invokeLater(detailsDlg::dispose); return; }
        SwingUtilities.invokeLater(editBtn::doClick); SuportTestUi.sleep(600);
        log("Mode d'edició actiu — omplint camps amb dades aleatòries...");

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
            SuportTestUi.setFieldNear(detailsDlg, pair[0], pair[1]);
        }

        JCheckBox chkLlegit = SuportTestUi.findCheckBox(detailsDlg, "Llegit");
        if (chkLlegit != null) {
            boolean newVal = !chkLlegit.isSelected();
            SwingUtilities.invokeAndWait(() -> chkLlegit.setSelected(newVal));
            log("  S'ha commutat Llegit -> " + newVal);
        }

        AbstractButton saveBtn = SuportTestUi.findBtnIn((Container)detailsDlg, "Desa", "Guardar", "Save");
        if (saveBtn == null) { log("WARN: Botó Desa no trobat — prement Escape"); SuportTestUi.pressEscape(robot); SuportTestUi.sleep(300); return; }
        log("Clicant Desa...");
        SwingUtilities.invokeLater(saveBtn::doClick); SuportTestUi.sleep(1000);

        JDialog afterDlg = SuportTestUi.getTopDialog();
        if (afterDlg != null) {
            log("Diàleg RESULTAT després de desar l'edició: \"" + afterDlg.getTitle() + "\"");
            collectAndLog((Container)afterDlg);
            SwingUtilities.invokeLater(afterDlg::dispose); SuportTestUi.sleep(400);
        } else {
            log("OK: Cap diàleg d'error — l'edició probablement s'ha desat correctament");
        }
        JDialog still = SuportTestUi.getTopDialog();
        if (still != null) { SwingUtilities.invokeLater(still::dispose); SuportTestUi.sleep(300); }
        log("Test EDITAR LLIBRE acabat.");
    }

    // ── Ajudants de recorregut de components (només els que encara són específics d'UIAudit) ─

    private static void collectAndLog(Container c) {
        List<String> items = new ArrayList<>();
        SuportTestUi.collectComponents(c, "  ", items);
        items.forEach(UIAudit::log);
    }

    // ── Registre ────────────────────────────────────────────────────────────────

    private static void log(String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
        reportFile.println(line);
        // També es mostra a stdout perquè l'usuari ho pugui llegir
        System.out.println(line);
    }

    private static void print(String msg) {
        System.out.println(msg);
        reportFile.println(msg);
    }

    private static void printHelp() {
        print("""
            Ordres:
              windows             llista totes les finestres obertes
              scan [title]        llista tots els components visibles d'una finestra
              click <text>        clica el botó el text del qual conté <text>
              type <text>         enganxa <text> al component amb el focus
              clear               buida el camp de text amb el focus
              focus <label>       clica el camp de text proper a l'etiqueta
              close               tanca el diàleg més alt
              enter               prem Enter
              esc                 prem Escape
              wait [ms]           dorm (per defecte 500 ms)
              rows                imprimeix les 10 primeres files de la taula
              open-row <n>        doble clic a la fila n de la taula
              auto                executa l'auditoria automatitzada completa
              exit                surt
            """);
    }

    private static void writeJsonReport() {
        try (PrintWriter jw = new PrintWriter(new FileWriter("checkBiblio/audit_report.json", false), true)) {
            jw.printf("{\"fail\":%d,\"warn\":%d,\"timestamp\":\"%s\"}%n",
                failCount, warnCount, LocalDateTime.now());
        } catch (IOException e) {
            log("WARN: no s'ha pogut escriure audit_report.json: " + e.getMessage());
        }
    }

    private static void closeReport() {
        log("=== UIAudit finalitzat ===");
        writeJsonReport();
        reportFile.close();
    }
}
