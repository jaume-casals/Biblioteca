package herramienta;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class FieldAutoComplete {

    private static final int MAX_SUGGESTIONS = 8;

    public static void attach(JTextField field, List<String> suggestions) {
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) {}

            private void update() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText();
                    popup.removeAll();
                    if (text.isEmpty()) { popup.setVisible(false); return; }
                    String lower = text.toLowerCase();
                    int count = 0;
                    for (String s : suggestions) {
                        if (s.toLowerCase().startsWith(lower) && !s.equalsIgnoreCase(text)) {
                            JMenuItem item = new JMenuItem(s);
                            item.addMouseListener(new MouseAdapter() {
                                @Override public void mousePressed(MouseEvent e) {
                                    field.setText(s);
                                    popup.setVisible(false);
                                }
                            });
                            popup.add(item);
                            if (++count >= MAX_SUGGESTIONS) break;
                        }
                    }
                    if (count == 0) { popup.setVisible(false); return; }
                    if (!popup.isVisible()) {
                        popup.show(field, 0, field.getHeight());
                    }
                    popup.revalidate();
                    popup.repaint();
                });
            }
        });

        field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popup.isVisible()) {
                    popup.getComponent(0).requestFocusInWindow();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                }
            }
        });
    }
}
