package herramienta;

import java.awt.Dimension;
import java.awt.Toolkit;

public class ConfiguracionPantalla {
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private static double width = 1600;
	private static double height = 900;

	public static double alturaS(double d) {
		return (width * d) / 100;
	}

	public static double ampladaS(double a) {
		return (height * a) / 100;
	}

	public static double getHeightS() {
		return height;
	}

	public static double getWidthS() {
		return width;
	}

	public double altura(double a) {
		return (width * a) / 100;
	}

	public double amplada(double a) {
		return (height * a) / 100;
	}

	public double getHeight() {
		return height;
	}

	public double getWidth() {
		return width;
	}
}
