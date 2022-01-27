package main;

import java.awt.EventQueue;

import javax.swing.UIManager;

import presentacio.MainFrameControl;
import presentacio.MainFramePanel;

public class Ejecutable {
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			private MainFramePanel vista;
			private MainFrameControl mainControl;

			public void run() {
				try {
					UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
					this.vista = new MainFramePanel();
					mainControl = new MainFrameControl(this.vista);
					mainControl.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
