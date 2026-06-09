package checkBiblio;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Shared helpers for the {@code checkBiblio} audit / stress harness.
 *
 * <p>UIAudit (interactive console) and StressTest (chaos / edge-case hammer)
 * were nearly-identical twins: each carried its own copy of "find a button
 * by text", "find a JTextField by neighbouring label", "wait for the main
 * JFrame", "flatten visible component tree", and a dozen more small
 * utilities. This class is the consolidated home for them.</p>
 *
 * <p>All methods are {@code public static}; the harness keeps its own
 * mutable state (Robot, PrintWriter, pass/warn/fail counters) and threads
 * it through the helpers as parameters where needed.</p>
 *
 * <p>Deliberately NOT here: the log/print writer (each harness has its own
 * format and lock policy), the run-counter for screenshots (the harness
 * no longer writes screenshots — that path was removed alongside this
 * extraction), and any test-specific assertion helpers.</p>
 */
public final class UiTestSupport {

    private UiTestSupport() {}

    // ── String utilities ─────────────────────────────────────────────────────

    /** Lowercase + strip diacritics so "estadist" matches "Estadístiques". */
    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .toLowerCase();
    }

    // ── Sleep ─────────────────────────────────────────────────────────────────

    /** Interrupt-swallowing sleep used everywhere instead of raw Thread.sleep. */
    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ── Component traversal ───────────────────────────────────────────────────

    /**
     * Walks the visible tree under {@code c} and returns the first visible
     * {@link AbstractButton} whose text contains (after {@link #norm}) any
     * of the given hints. Returns {@code null} if none match.
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

    /** Same as {@link #findBtnIn(Container, String...)} but searches across
     *  all visible top-level windows. */
    public static AbstractButton findBtnIn(JFrame main, String... texts) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnIn((Container) w, texts);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Walks the visible tree under {@code c} and returns the first visible
     * {@link AbstractButton} whose tooltip text contains (after {@link #norm})
     * any of the given hints.
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

    /** Cross-window variant of {@link #findBtnByTooltip(Container, String...)}. */
    public static AbstractButton findBtnByTooltip(JFrame main, String... tips) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            AbstractButton found = findBtnByTooltip((Container) w, tips);
            if (found != null) return found;
        }
        return null;
    }

    /** Generic deep-component lookup by runtime class. */
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
     * Finds the first {@link JTextField} appearing in document order within
     * the next five visible components after a {@link JLabel} whose text
     * contains the given hint. Used to attach to form fields by their
     * adjacent label without needing to know the form's internal layout.
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

    /** Walks the visible tree and returns the first {@link JCheckBox} whose
     *  text contains the given hint. */
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

    /** Same as {@link #findCheckBox} but searches across all visible windows. */
    public static JCheckBox findCheckBoxGlobal(String text) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            JCheckBox found = findCheckBox((Container) w, text);
            if (found != null) return found;
        }
        return null;
    }

    /** Appends every visible component under {@code c} (depth-first) to {@code out}. */
    public static void flattenVisible(Container c, List<Component> out) {
        for (Component comp : c.getComponents()) {
            if (!comp.isVisible()) continue;
            out.add(comp);
            if (comp instanceof Container sub) flattenVisible(sub, out);
        }
    }

    /**
     * Walks the visible tree and appends a one-line, log-friendly description
     * of each labelled / input component to {@code out}. Indent grows by
     * two spaces per nesting level. Used by the harness for component
     * inventories.
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

    // ── Window helpers ────────────────────────────────────────────────────────

    /**
     * Polls every 150 ms for up to {@code timeoutMs} and returns the first
     * visible {@link JFrame} with a non-blank title. Returns {@code null}
     * if no such frame appears.
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
     * Polls every 100 ms for up to {@code timeoutMs} and returns the first
     * visible {@link JDialog} (top-down). Returns {@code null} if no dialog
     * appears.
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

    /** Returns the topmost visible {@link JDialog} in z-order, or {@code null}. */
    public static JDialog getTopDialog() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible()) return d;
        return null;
    }

    /** Same as {@link #getTopDialog()} but ignores the supplied dialog. */
    public static JDialog getTopDialogExcept(JDialog except) {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i] instanceof JDialog d && d.isVisible() && d != except) return d;
        return null;
    }

    /** Returns the topmost visible {@link Window} in z-order, or {@code null}. */
    public static Window getTopWindow() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--)
            if (ws[i].isVisible()) return ws[i];
        return null;
    }

    /** Returns the first visible window whose title contains {@code fragment}
     *  (case-insensitive), or {@code null}. */
    public static Window findWindowByTitle(String fragment) {
        for (Window w : Window.getWindows()) {
            if (!w.isVisible()) continue;
            if (windowTitle(w).toLowerCase().contains(fragment.toLowerCase())) return w;
        }
        return null;
    }

    /** Best-effort window title: JFrame → getTitle(), JDialog → getTitle(),
     *  anything else → simple class name. */
    public static String windowTitle(Window w) {
        if (w instanceof JFrame f) return f.getTitle();
        if (w instanceof JDialog d) return d.getTitle();
        return w.getClass().getSimpleName();
    }

    // ── Robot helpers ─────────────────────────────────────────────────────────

    /**
     * Moves the mouse to the centre of {@code c} and clicks it. Caller passes
     * the {@link Robot} so the helper does not own shared state.
     * No-op if the component is not currently showing on screen.
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

    // ── Form interaction ──────────────────────────────────────────────────────

    /**
     * Schedules a focus / select-all / setText on the EDT, blocks until the
     * EDT has applied it, then sleeps briefly. Use for both book-form fields
     * and the top-bar search field.
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
     * Convenience: find the JTextField next to a label matching
     * {@code labelHint} and call {@link #setField(JTextField, String)} on it.
     * No-op if the field cannot be located.
     */
    public static void setFieldNear(Container c, String labelHint, String value) throws Exception {
        JTextField tf = findTextFieldNear(c, labelHint);
        if (tf == null) return;
        setField(tf, value);
    }

    /** Schedules a {@code doClick()} on the EDT (non-blocking) and sleeps
     *  briefly so the EDT can dispatch it. */
    public static void doClick(AbstractButton btn) {
        SwingUtilities.invokeLater(btn::doClick);
        sleep(60);
    }

    /** Clicks the first save / guardar / desa button found in {@code dlg}. */
    public static void clickSave(JDialog dlg) {
        AbstractButton btn = findBtnIn((Container) dlg, "Desa", "Guardar", "Save");
        if (btn != null) doClick(btn);
    }

    // ── Dialog classification ─────────────────────────────────────────────────

    /**
     * Heuristic: does {@code d} look like a book-details dialog?
     * Matches localised titles for "book details" / "ficha del libro".
     */
    public static boolean isBookDetailsDialog(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("expedient del llibre")
            || t.contains("ficha del libro")
            || t.contains("book details");
    }

    /** Heuristic: does {@code d} look like a book-form dialog (new / edit)? */
    public static boolean isBookFormDialog(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("nou llibre")
            || t.contains("new book")
            || t.contains("expedient del llibre");
    }

    /**
     * Heuristic: does {@code d} look like a validation / error dialog?
     * Matches the localised error/avís/warn/invalid vocabulary and the
     * blank-title JOptionPane pattern.
     */
    public static boolean looksLikeError(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("error") || t.contains("avis") || t.contains("warn")
            || t.contains("invalid") || t.contains("valid") || t.contains("validac")
            || t.contains("incorrecte") || t.contains("camp")
            || t.isBlank();
    }

    /** Heuristic: does {@code d} look like a delete / esborrar confirm? */
    public static boolean isTestBookDeleteConfirm(JDialog d) {
        if (d == null) return false;
        String t = norm(d.getTitle());
        return t.contains("eliminar") || t.contains("esborrar") || t.contains("delete")
            || t.contains("confirm") || t.contains("segur");
    }

    // ── Key shortcuts (wrapped here so the harness avoids raw VK_ constants) ─

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
