package herramienta;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.TreeSet;

public class FieldAutoComplete {

    private FieldAutoComplete() {}

    /** Límit per defecte de sugerències mostrades al popup. Modificable per
     *  {@link #attach(JTextField, java.util.List, int)}. */
    public static final int MAX_SUGGESTIONS = 8;

    public sealed interface Adjunts permits AdjuntsAutoCompletatCamp {}
    private static final class AdjuntsAutoCompletatCamp implements Adjunts { }

    public static Adjunts adjuntarReturning(JTextField field, List<String> suggestions) {
        attach(field, suggestions);
        return new AdjuntsAutoCompletatCamp();
    }

    public static void attach(JTextField field, List<String> suggestions) {
        attach(field, suggestions, MAX_SUGGESTIONS);
    }

    public static void attach(JTextField field, List<String> suggestions, int maxSuggestions) {
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(suggestions);

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) {}

            private void update() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText();
                    popup.removeAll();
                    if (text.isEmpty()) { popup.setVisible(false); return; }
                    String lower = text.toLowerCase(java.util.Locale.ROOT);
                    int count = 0;
                    for (String s : sorted.tailSet(lower, true)) {
                        if (count >= maxSuggestions) break;
                        if (!s.toLowerCase(java.util.Locale.ROOT).startsWith(lower)) break;
                        if (s.equalsIgnoreCase(text)) continue;
                        JMenuItem item = new JMenuItem(s);
                        item.addMouseListener(new MouseAdapter() {
                            @Override public void mousePressed(MouseEvent e) {
                                field.setText(s);
                                popup.setVisible(false);
                            }
                        });
                        popup.add(item);
                        count++;
                    }
                    if (count == 0) { popup.setVisible(false); return; }
                    if (!popup.isVisible()) {
                        java.awt.Dimension popupSize = popup.getPreferredSize();
                        try {
                            java.awt.Point loc = field.getLocationOnScreen();
                            int screenH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
                            int y = (loc.y + field.getHeight() + popupSize.height <= screenH)
                                ? field.getHeight() : -popupSize.height;
                            popup.show(field, 0, y);
                        } catch (java.awt.IllegalComponentStateException ex) {
                            popup.show(field, 0, field.getHeight());
                        }
                    }
                    popup.revalidate();
                    popup.repaint();
                });
            }
        });

        field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popup.isVisible() && popup.getComponentCount() > 0) {
                    popup.getComponent(0).requestFocusInWindow();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                }
            }
        });
    }
}