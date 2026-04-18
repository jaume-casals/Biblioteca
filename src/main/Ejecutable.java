package main;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.UIManager;

import herramienta.DialogoError;
import herramienta.UITheme;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;

public class Ejecutable {
	public static void main(String[] args) {
		// Check for headless environment before attempting to create GUI
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("ERROR: This application requires a graphical display.");
			String display = System.getenv("DISPLAY");
			if (display == null || display.isEmpty()) {
				System.err.println("DISPLAY environment variable is not set.");
				System.err.println("Try running with: DISPLAY=:0 java -cp <classpath> main.Ejecutable");
			} else {
				System.err.println("DISPLAY is set to: " + display);
				System.err.println("However, Java cannot access the display. Possible causes:");
				System.err.println("  1. X11 libraries may not be available to Java");
				System.err.println("  2. Display permissions may be incorrect");
				System.err.println("  3. Running in a sandboxed environment without display access");
			}
			System.err.println("\nTo run this application:");
			System.err.println("  1. Set DISPLAY variable: export DISPLAY=:0");
			System.err.println("  2. Run from terminal: java -cp bin main.Ejecutable");
			System.err.println("  Or use: DISPLAY=:0 java -cp bin main.Ejecutable");
			System.exit(1);
		}

		EventQueue.invokeLater(new Runnable() {
			private MainFramePanel vista;
			private MainFrameControl mainControl;

			public void run() {
				try {
					UIManager.put("nimbusBase",                new Color(0x2471A3));
					UIManager.put("nimbusBlueGrey",            new Color(0x5D8AA8));
					UIManager.put("control",                   UITheme.BG_MAIN);
					UIManager.put("text",                      UITheme.TEXT_DARK);
					UIManager.put("nimbusFocus",               UITheme.ACCENT);
					UIManager.put("nimbusSelectionBackground", UITheme.ACCENT);
					UIManager.put("nimbusSelectedText",        Color.WHITE);
					UIManager.put("defaultFont",               UITheme.FONT_BASE);
					UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
					UIManager.put("Table.alternateRowColor",   UITheme.TABLE_ALT);
					this.vista = new MainFramePanel();
					mainControl = MainFrameControl.getInstance(this.vista);
					mainControl.setVisible(true);
				} catch (Exception e) {
					new DialogoError(e).showErrorMessage();
				}
			}
		});
//		ControladorDomini cd = ControladorDomini.getInstance();
//		System.out.println("resultat:");
//		System.out.println(cd.aplicarFiltres(null, null, null, null, null, false));

	}
}
