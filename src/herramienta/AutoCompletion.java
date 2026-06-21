package herramienta;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/* This work is hereby released into the Public Domain.
 * To view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/
 */

public class AutoCompletion extends PlainDocument {

	private final JComboBox<String> comboBox;
	private ComboBoxModel<String> model;
	private JTextComponent editor;
	private boolean selecting = false;

	private KeyListener editorKeyListener;
	private FocusListener editorFocusListener;

	public AutoCompletion(final JComboBox<String> comboBox) {
		this.comboBox = comboBox;
		model = comboBox.getModel();
		comboBox.addActionListener(e -> { if (!selecting) highlightCompletedText(0); });
		comboBox.addPropertyChangeListener(e -> {
			if ("editor".equals(e.getPropertyName()))
				configureEditor((ComboBoxEditor) e.getNewValue());
			if ("model".equals(e.getPropertyName()))
				model = (ComboBoxModel<String>) e.getNewValue();
		});
		editorKeyListener = new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				if (comboBox.isDisplayable() && e.getKeyCode() != KeyEvent.VK_ALT && !e.isAltDown())
					comboBox.setPopupVisible(true);
			}
		};
		editorFocusListener = new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) { highlightCompletedText(0); }
			@Override public void focusLost(FocusEvent e) { comboBox.setPopupVisible(false); }
		};
		configureEditor(comboBox.getEditor());
		Object selected = comboBox.getSelectedItem();
		if (selected != null)
			setText(selected.toString());
		highlightCompletedText(0);
	}

	public static void enable(JComboBox<String> comboBox) {
		comboBox.setEditable(true);
		new AutoCompletion(comboBox);
	}

	void configureEditor(ComboBoxEditor newEditor) {
		if (editor != null) {
			editor.removeKeyListener(editorKeyListener);
			editor.removeFocusListener(editorFocusListener);
		}

		if (newEditor != null) {
			editor = (JTextComponent) newEditor.getEditorComponent();
			editor.addKeyListener(editorKeyListener);
			editor.addFocusListener(editorFocusListener);
			editor.setDocument(this);
		}
	}

	@Override
	public void remove(int offs, int len) throws BadLocationException {
		if (selecting)
			return;
		super.remove(offs, len);
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		if (selecting)
			return;
		super.insertString(offs, str, a);
		String prefix = getText(0, getLength());
		// Cerca el primer element del model que comenci pel prefix
		// escrit (insensible a majúscules/minúscules)
		String lowerPrefix = prefix.toLowerCase(java.util.Locale.ROOT);
		for (int i = 0, n = model.getSize(); i < n; i++) {
			Object item = model.getElementAt(i);
			if (item != null && item.toString().toLowerCase(java.util.Locale.ROOT).startsWith(lowerPrefix)) {
				selecting = true;
				setText(item.toString());
				comboBox.setSelectedItem(item);
				selecting = false;
				break;
			}
		}
		highlightCompletedText(offs + str.length());
	}

	private void setText(String text) {
		try {
			super.remove(0, getLength());
			super.insertString(0, text, null);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private void highlightCompletedText(int start) {
		editor.setCaretPosition(getLength());
		editor.moveCaretPosition(start);
	}


}
