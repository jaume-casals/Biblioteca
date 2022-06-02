package herramienta;

import domini.Llibre;

public class checkLlibre {

	public static Llibre cheackLlibre(Integer isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {
		Llibre l = null;
		Boolean pass = false;

		if (countDig(isbn) == 10)
			pass = true;

		if (valoracio <= 10 && valoracio >= 0)
			pass = true;

		if (portada.startsWith("portades/"))
			pass = true;

		if (pass) {
			l = new Llibre(isbn, nom, autor, any, descripcio, valoracio, preu, llegit, portada);
			return l;
		}
		return null;

	}

	public static int countDig(int n) {
		int count = 0;
		while (n != 0) {
			n = n / 10;
			count = count + 1;
		}
		return count;
	}
}
