package checkBiblio;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Utilitats compartides per a l'entorn d'auditoria / estrès de {@code checkBiblio}.
 *
 * <p>UIAudit (consola interactiva) i StressTest (martell de caòs / casos límit)
 * eren bessons gairebé idèntics: cadascun duia la seva pròpia còpia de
 * "troba un botó per text", "troba un JTextField per etiqueta veïna",
 * "espera el JFrame principal", "aplana l'arbre de components visibles",
 * i una dotzena més de petites utilitats. Aquesta classe n'és la llar
 * consolidada.</p>
 *
 * <p>Tots els mètodes són {@code public static}; l'entorn conserva el seu
 * propi estat mutable (Robot, PrintWriter, comptadors pass/warn/fail) i el
 * passa als ajudants com a paràmetres quan cal.</p>
 *
 * <p>Deliberadament NO és aquí: el log/print writer (cada entorn té el seu
 * propi format i política de bloqueig), el comptador d'execucions per a
 * captures (l'entorn ja no escriu captures — aquest camí es va eliminar
 * alhora que aquesta extracció), ni cap ajudant d'asserció específic de test.</p>
 */
public final class SuportTestUi {

    private SuportTestUi() {}

    // ── Utilitats de cadenes ──────────────────────────────────────────────────

    /** Minúscules + elimina diacrítics perquè "estadist" coincideixi amb "Estadístiques". */
    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .toLowerCase();
    }

    // ── Sleep ─────────────────────────────────────────────────────────────────

    /** Sleep que ignora interrupcions, usat arreu en lloc del Thread.sleep directe. */
    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ── Recorregut de components ──────────────────────────────────────────────

    /**
     * Recorre l'arbre visible sota {@code c} i retorna el primer
     * {@link AbstractButton} visible el text del qual conté (després de
     * {@link #norm}) qualsevol dels hints donats. Retorna {@code null} si
     * no en coincideix cap.
     */
    public static AbstractButton findBtnIn(Container c, String... texts) {
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

    /** Igual que {@link #findBtnIn(Container, String...)} però cercant entre
     *  totes les finestres de nivell superior visibles. */
    public static AbstractButton findBtnIn(JFrame main, String... texts) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnIn((Container) w, texts);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Recorre l'arbre visible sota {@code c} i retorna el primer
     * {@link AbstractButton} visible el text del tooltip del qual conté
     * (després de {@link #norm}) qualsevol dels hints donats.
     */
    public static AbstractButton findBtnByTooltip(Container c, String... tips) {
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

    /** Variant multi-finestra de {@link #findBtnByTooltip(Container, String...)}. */
    public static AbstractButton findBtnByTooltip(JFrame main, String... tips) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnByTooltip((Container) w, tips);
            if (found != null) return found;
        }
        return null;
    }

    /** Cerca genèrica profunda de components per classe en temps d'execució. */
    @SuppressWarnings("unchecked")
    public static <T extends Component> T findComponent(Container c, Class<T> type) {
        for (Component comp : c.getComponents()) {
            if (type.isInstance(comp)) return (T) comp;
            if (comp instanceof Container sub) {
                T found = findComponent(sub, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Troba el primer {@link JTextField} que aparegui en ordre de document
     * dins dels cinc components visibles següents a un {@link JLabel} el
     * text del qual contingui el hint donat. S'usa per accedir als camps
     * de formulari per la seva etiqueta adjacent sense necessitat de
     * conèixer la disposició interna del formulari.
     */
    public static JTextField findTextFieldNear(Container c, String labelHint) {
        List<Component> flat = new ArrayList<>();
        flattenVisible(c, flat);
        for (int i = 0; i < flat.size(); i++) {
            Component comp = flat.get(i);
            if (comp instanceof JLabel lbl && lbl.getText() != null
                    && norm(lbl.getText()).contains(norm(labelHint))) {
                for (int j = i + 1; j < Math.min(i + 6, flat.size()); j++) {
                    if (flat.get(j) instanceof JTextField tf) return tf;
                }
            }
        }
        return null;
    }

    /** Recorre l'arbre visible i retorna el primer {@link JCheckBox} el text
     *  del qual contingui el hint donat. */
    public static JCheckBox findCheckBox(Container c, String text) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JCheckBox chk
                    && norm(chk.getText()).contains(norm(text))) return chk;
            if (comp instanceof Container sub) {
                JCheckBox found = findCheckBox(sub, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Igual que {@link #findCheckBox} però cercant entre totes les finestres visibles. */
    public static JCheckBox findCheckBoxGlobal(String text) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JCheckBox found = findCheckBox((Container) w, text);
            if (found != null) return found;
        }
        return null;
    }

    /** Afegeix cada component visible sota {@code c} (en profunditat) a {@code out}. */
    public static void flattenVisible(Container c, List<Component> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            out.add(comp);
            if (comp instanceof Container sub) flattenVisible(sub, out);
        }
    }

    /**
     * Recorre l'arbre visible i afegeix una descripció d'una línia, apta
     * per a log, de cada component etiquetat / d'entrada a {@code out}.
     * El sagnat creix dos espais per nivell d'anidament. L'entorn l'usa
     * per inventariar components.
     */
    public static void collectComponents(Container c, String indent, List<String> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            if (comp instanceof AbstractButton btn && !(btn instanceof JCheckBox)) {
                out.add(indent + "[BTN] \"" + btn.getText() + "\""
                    + (btn.getToolTipText() != null ? "  tip:\"" + btn.getToolTipText() + "\"" : ""));
            } else if (comp instanceof JCheckBox chk) {
                out.add(indent + "[CHK] \"" + chk.getText() + "\" selected=" + chk.isSelected());
            } else if (comp instanceof JTextField tf) {
                out.add(indent + "[TF ] value=\"" + tf.getText() + "\""
                    + (tf.getToolTipText() != null ? "  tip:\"" + tf.getToolTipText() + "\"" : ""));
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

    // ── Ajudants de finestra ──────────────────────────────────────────────────

    /**
     * Sonda cada 150 ms durant fins a {@code timeoutMs} i retorna el primer
     * {@link JFrame} visible amb títol no buit. Retorna {@code null} si no
     * n'apareix cap.
     */
    public static JFrame waitForMainFrame(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame f && f.isVisible() && !f.getTitle().isBlank()) return f;
            }
            Thread.sleep(150);
        }
        return null;
    }

    /**
     * Sonda cada 100 ms durant fins a {@code timeoutMs} i retorna el primer
     * {@link JDialog} visible (de dalt a baix). Retorna {@code null} si no
     * n'apareix cap.
     */
    public static JDialog waitForDialog(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            JDialog d = getTopDialog();
            if (d != null) return d;
            Thread.sleep(100);
        }
        return null;
    }

    /** Retorna el {@link JDialog} visible més alt en ordre z, o {@code null}. */
    public static JDialog getTopDialog() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible()) return d;
        return null;
    }

    /** Igual que {@link #getTopDialog()} però ignorant el diàleg subministrat. */
    public static JDialog getTopDialogExcept(JDialog except) {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible() && d != except) return d;
        return null;
    }

    /** Retorna la {@link Window} visible més alta en ordre z, o {@code null}. */
    public static Window getTopWindow() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i].isVisible()) return ws[i];
        return null;
    }

    /** Retorna la primera finestra visible el títol de la qual contingui
     *  {@code fragment} (sense distingir majúscules/minúscules), o {@code null}. */
    public static Window findWindowByTitle(String fragment) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            if (windowTitle(w).toLowerCase().contains(fragment.toLowerCase())) return w;
        }
        return null;
    }

    /** Títol de finestra a millor esforç: JFrame → getTitle(), JDialog → getTitle(),
     *  qualsevol altre → nom simple de la classe. */
    public static String windowTitle(Window w) {
        if (w instanceof JFrame f) return f.getTitle();
        if (w instanceof JDialog d) return d.getTitle();
        return w.getClass().getSimpleName();
    }

    // ── Ajudants de Robot ─────────────────────────────────────────────────────

    /**
     * Mou el ratolí al centre de {@code c} i hi clica. El caller passa el
     * {@link Robot} perquè l'ajudant no posseeix estat compartit.
     * No-op si el component no és visible a pantalla en aquest moment.
     */
    public static void clickComponent(Robot robot, Component c) throws AWTException {
        if (!c.isShowing()) return;
        Point loc = c.getLocationOnScreen();
        int cx = loc.x + c.getWidth() / 2;
        int cy = loc.y + c.getHeight() / 2;
        robot.mouseMove(cx, cy);
        sleep(60);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    // ── Interacció amb formularis ────────────────────────────────────────────

    /**
     * Programa un focus / select-all / setText a l'EDT, espera fins que
     * l'EDT l'hagi aplicat i dorm breument. S'usa tant per als camps del
     * formulari de llibre com per al camp de cerca de la barra superior.
     */
    public static void setField(JTextField tf, String value) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            tf.requestFocusInWindow();
            tf.selectAll();
            tf.setText(value);
        });
        sleep(40);
    }

    /**
     * Comoditat: troba el JTextField al costat d'una etiqueta que
     * coincideixi amb {@code labelHint} i hi crida
     * {@link #setField(JTextField, String)}. No-op si no es pot localitzar.
     */
    public static void setFieldNear(Container c, String labelHint, String value) throws Exception {
        JTextField tf = findTextFieldNear(c, labelHint);
        if (tf == null) return;
        setField(tf, value);
    }

    /** Programa un {@code doClick()} a l'EDT (no bloquejant) i dorm breument
     *  perquè l'EDT el dispari. */
    public static void doClick(AbstractButton btn) {
        SwingUtilities.invokeLater(btn::doClick);
        sleep(60);
    }

    /** Clica el primer botó de desar / guardar / desa trobat a {@code dlg}. */
    public static void clickSave(JDialog dlg) {
        AbstractButton btn = findBtnIn((Container) dlg, "Desa", "Guardar", "Save");
        if (btn != null) doClick(btn);
    }

    // ── Classificació de diàlegs ───────────────────────────────────────────────

    /**
     * Heurística: sembla un diàleg de detalls de llibre?
     * Coincideix amb títols localitzats de "book details" / "ficha del libro".
     */
    public static boolean isBookDetailsDialog(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("expedient del llibre")
            || t.contains("ficha del libro")
            || t.contains("book details");
    }

    /** Heurística: sembla un diàleg de formulari de llibre (nou / editar)? */
    public static boolean isBookFormDialog(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("nou llibre")
            || t.contains("new book")
            || t.contains("expedient del llibre");
    }

    /**
     * Heurística: sembla un diàleg de validació / error?
     * Coincideix amb el vocabulari localitzat error/avís/warn/invalid i
     * amb el patró JOptionPane de títol buit.
     */
    public static boolean looksLikeError(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("error") || t.contains("avis") || t.contains("warn")
            || t.contains("invalid") || t.contains("valid") || t.contains("validac")
            || t.contains("incorrecte") || t.contains("camp")
            || t.isBlank();
    }

    /** Heurística: sembla una confirmació d'esborrar / eliminar? */
    public static boolean isTestBookDeleteConfirm(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("eliminar") || t.contains("esborrar") || t.contains("delete")
            || t.contains("confirm") || t.contains("segur");
    }

    // ── Dreceres de teclat (envoltades aquí perquè l'entorn no usa constants VK_ directes) ─

    public static void pressKey(Robot robot, int vk) {
        robot.keyPress(vk);
        robot.keyRelease(vk);
    }

    public static void pressCtrlN(Robot robot) {
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_N);
        robot.keyRelease(KeyEvent.VK_N);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    public static void pressCtrlA(Robot robot) {
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    public static void pressEscape(Robot robot) {
        pressKey(robot, KeyEvent.VK_ESCAPE);
    }

    public static void pressEnter(Robot robot) {
        pressKey(robot, KeyEvent.VK_ENTER);
    }

    public static void pressDelete(Robot robot) {
        pressKey(robot, KeyEvent.VK_DELETE);
    }
}
