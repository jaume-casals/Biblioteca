package herramienta;

import domini.Llibre;

public class LlibreValidator {

	/**
	 * Validates and builds a Llibre.
	 * Only isbn and nom are mandatory. All other fields accept null/blank and get
	 * safe defaults (valoracio=0, preu=0, llegit=false, rest empty strings).
	 */
	public static Llibre checkLlibre(Long isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {

		if (isbn == null || countDig(isbn) != 14)
			throw new IllegalArgumentException("L'ISBN ha de tenir exactament 14 dígits (era: "
				+ (isbn == null ? "null" : countDig(isbn)) + ")");

		if (nom == null || nom.isBlank())
			throw new IllegalArgumentException("El nom no pot estar buit");

		if (valoracio != null && (valoracio < 0 || valoracio > 10))
			throw new IllegalArgumentException("La valoració ha d'estar entre 0 i 10 (era: " + valoracio + ")");

		return new Llibre(
			isbn, nom,
			autor != null ? autor : "",
			any != null ? any : 0,
			descripcio != null ? descripcio : "",
			valoracio != null ? valoracio : 0.0,
			preu != null ? preu : 0.0,
			llegit != null ? llegit : false,
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
