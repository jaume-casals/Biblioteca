package herramienta;

import domini.Llibre;

public class checkLlibre {

	public static Llibre cheackLlibre(Long isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {

		if (isbn == null || countDig(isbn) != 14)
			throw new IllegalArgumentException("L'ISBN ha de tenir exactament 14 dígits (era: "
				+ (isbn == null ? "null" : countDig(isbn)) + ")");

		if (valoracio == null || valoracio < 0 || valoracio > 10)
			throw new IllegalArgumentException("La valoració ha d'estar entre 0 i 10 (era: " + valoracio + ")");

		if (nom == null || nom.isBlank())
			throw new IllegalArgumentException("El nom no pot estar buit");

		return new Llibre(isbn, nom, autor, any, descripcio, valoracio, preu, llegit,
				portada != null ? portada : "");
	}

	public static int countDig(long n) {
		int count = 0;
		while (n != 0) {
			n = n / 10;
			count = count + 1;
		}
		return count;
	}
}
