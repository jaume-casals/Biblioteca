package herramienta;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

	JComboBox<String> comboBox;
	ComboBoxModel<String> model;
	JTextComponent editor;
	boolean selecting = false;
	boolean hidePopupOnFocusLoss;
	boolean hitBackspace = false;
	boolean hitBackspaceOnSelection;

	KeyListener editorKeyListener;
	FocusListener editorFocusListener;

	public AutoCompletion(final JComboBox<String> comboBox) {
		this.comboBox = comboBox;
		model = comboBox.getModel();
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!selecting)
					highlightCompletedText(0);
			}
		});
		comboBox.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if ("editor".equals(e.getPropertyName()))
					configureEditor((ComboBoxEditor) e.getNewValue());
				if ("model".equals(e.getPropertyName()))
					model = (ComboBoxModel<String>) e.getNewValue();
			}
		});
		editorKeyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (comboBox.isDisplayable() && e.getKeyCode() != KeyEvent.VK_ALT && !e.isAltDown())
					comboBox.setPopupVisible(true);
				hitBackspace = false;
				switch (e.getKeyCode()) {
				case KeyEvent.VK_BACK_SPACE:
					hitBackspace = true;
					hitBackspaceOnSelection = editor.getSelectionStart() != editor.getSelectionEnd();
					break;
				// ignore delete key
				case KeyEvent.VK_DELETE:
					e.consume();
					comboBox.getToolkit().beep();
					break;
				}
			}
		};
		hidePopupOnFocusLoss = true;
		editorFocusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				highlightCompletedText(0);
			}

			public void focusLost(FocusEvent e) {
				comboBox.setPopupVisible(false);
			}
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

	public void remove(int offs, int len) throws BadLocationException {
		if (selecting)
			return;
		if (hitBackspace) {
			if (offs > 0) {
				if (hitBackspaceOnSelection)
					offs--;
			} else {
				comboBox.getToolkit().beep(); // when available use:
												// UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
			}
			highlightCompletedText(offs);
		} else {
			super.remove(offs, len);
		}
	}

	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		if (selecting)
			return;
		super.insertString(offs, str, a);
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


	private boolean startsWithIgnoreCase(String str1, String str2) {
		return str1.toUpperCase().startsWith(str2.toUpperCase());
	}

}
