package herramienta.text;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
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
        TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(suggestions);
        attach(field, sorted, maxSuggestions);
    }

    /** Sobrecàrrega que accepta un {@link Set} ja ordenat perquè el
     *  consumidor pugui amortir la classificació quan adjunta molts
     *  camps al mateix temps. */
    public static void attach(JTextField field, java.util.NavigableSet<String> sorted, int maxSuggestions) {
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);

        // Mida de pantalla capturada una sola vegada quan s'adjunta, en
        // lloc de cada actualització (cada keystroke); la mida de pantalla
        // no canvia mentre el procés és viu.
        final int screenH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;

        // Pool de JMenuItem reusables — la quantitat mai supera
        // maxSuggestions, i una entrada per posició conserva el seu MouseAdapter.
        final JMenuItem[] itemPool = new JMenuItem[Math.max(1, maxSuggestions)];

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
                        final JMenuItem item;
                        if (itemPool[count] == null) {
                            item = new JMenuItem();
                            final String chosen = s;
                            item.addActionListener((ActionEvent ev) -> {
                                field.setText(chosen);
                                popup.setVisible(false);
                            });
                            item.addMouseListener(new MouseAdapter() {
                                @Override public void mousePressed(MouseEvent e) {
                                    field.setText(chosen);
                                    popup.setVisible(false);
                                }
                            });
                            itemPool[count] = item;
                        } else {
                            item = itemPool[count];
                            item.setText(s);
                            // L'ActionListener de cada pool captura un `chosen`
                            // diferent; el reemplacem perquè seleccioni el text
                            // actual del JMenuItem quan es cliqui.
                            for (java.awt.event.ActionListener al : item.getActionListeners())
                                item.removeActionListener(al);
                            item.addActionListener((ActionEvent ev) -> {
                                field.setText(item.getText());
                                popup.setVisible(false);
                            });
                        }
                        popup.add(item);
                        count++;
                    }
                    if (count == 0) { popup.setVisible(false); return; }
                    if (!popup.isVisible()) {
                        java.awt.Dimension popupSize = popup.getPreferredSize();
                        try {
                            java.awt.Point loc = field.getLocationOnScreen();
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