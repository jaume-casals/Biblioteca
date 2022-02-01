package main;

import java.awt.EventQueue;

import javax.swing.UIManager;

import domini.ControladorDomini;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;

public class Ejecutable {
	public static void main(String[] args) {
		/*

		EventQueue.invokeLater(new Runnable() {
			private MainFramePanel vista;

			public void run() {
				try {
					UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
					vista = new MainFramePanel();
					vista.setVisible(true);
					new MainFrameControl(vista);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		*/
		ControladorDomini cd = ControladorDomini.getInstance();
		System.out.println("resultat:");
		System.out.println(cd.aplicarFiltres("0", "1", 1000, 2000, 3000));
	}
}
