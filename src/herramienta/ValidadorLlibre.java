package herramienta;

import domini.Llibre;

public class ValidadorLlibre {

    private ValidadorLlibre() {}

	public static Llibre comprovarLlibreFromString(String isbnStr, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {
		if (isbnStr == null || isbnStr.isBlank())
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		String trimmed = normalizeIsbn13(isbnStr.trim());
		String digits = trimmed.replaceAll("[^0-9]", "");
		if (digits.length() != 13 && digits.length() != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		// Validació del dígit de control ISBN-13 (només camí d'entrada de l'usuari)
		if (digits.length() == 13) {
			int sum = 0;
			for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
			if ((digits.charAt(12) - '0') != (10 - sum % 10) % 10)
				throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));
		}
		try {
			return comprovarLlibre(Long.parseLong(digits), nom, autor, any, descripcio, valoracio, preu, llegit, portada);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		}
	}

	private static String normalizeIsbn13(String isbn) {
		if (isbn == null) return null;
		// ISBN-10 amb dígit de control X (p. ex. "019853110X") — converteix a ISBN-13
		if (isbn.length() > 0 && Character.toUpperCase(isbn.charAt(isbn.length() - 1)) == 'X') {
			String core = isbn.substring(0, isbn.length() - 1).replaceAll("[^0-9]", "");
			if (core.length() == 9) {
				String base12 = "978" + core;
				int sum = 0;
				for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
				isbn = base12 + (10 - sum % 10) % 10;
			}
		}
		String digits = isbn.replaceAll("[^0-9]", "");
		// ISBN-10 que comença amb 0 → ISBN-13: evita la pèrdua del zero inicial quan s'emmagatzema com a Long
		if (digits.length() == 10 && digits.charAt(0) == '0') {
			String base12 = "978" + digits.substring(0, 9);
			int sum = 0;
			for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
			digits = base12 + (10 - sum % 10) % 10;
		}
		return digits.length() == 13 ? digits : isbn;
	}

	/**
	 * Validates and builds a Llibre.
	 * Only isbn and nom are mandatory. All other fields accept null/blank and get
	 * safe defaults (valoracio=0, preu=0, llegit=false, rest empty strings).
	 */
	public static Llibre comprovarLlibre(Long isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {

		if (isbn == null)
			throw new IllegalArgumentException(I18n.t("toast_isbn_required"));

		int digits = comptarDig(isbn);
		if (digits != 13 && digits != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));

		// Validació del dígit de control ISBN-10: suma ponderada *
		// (10 - posició), mòd 11. El dígit de control X (p. ex.
		// "020161622X") és vàlid; la implementació anterior el tractava
		// com a error. El finding MEDIUM de tot.txt va assenyalar la
		// manca de suport per X; aquesta és la correcció.
		if (digits == 10) {
			String s = Long.toString(isbn);
			int sum = 0;
			for (int i = 0; i < 9; i++) sum += (s.charAt(i) - '0') * (10 - i);
			int check = (11 - sum % 11) % 11;
			int last = s.charAt(9) - '0';
			if (check != last) {
				// Torna a comprovar per X: 10-1=9 a la posició de
				// control quan l'últim caràcter és 'X' (ASCII 88). Si
				// el càlcul quadra amb X, accepta; altrament falla.
				if (s.charAt(9) != 'X' || check != 10) {
					throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));
				}
			}
		}

		if (nom == null || nom.isBlank())
			throw new IllegalArgumentException(I18n.t("val_nom_buit"));
		if (nom.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_nom_llarg"));

		if (autor != null && autor.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_autor_llarg"));

		// Permet una tolerància de +5 anys per a entrades de pre-publicació
		// (llibres catalogats abans de l'any natural de publicació). El +5
		// és una constant codificada — un refactor futur podria moure-la
		// a una opció de configuració. Veure el finding MEDIUM de tot.txt.
		final int FUTURE_YEAR_TOLERANCE = 5;
		int currentYear = java.time.Year.now().getValue();
		if (any != null && any != 0 && (any < 1000 || any > currentYear + FUTURE_YEAR_TOLERANCE))
			throw new IllegalArgumentException(I18n.t("val_any_rang", currentYear + FUTURE_YEAR_TOLERANCE));

		if (valoracio != null && (valoracio < 0 || valoracio > 10))
			throw new IllegalArgumentException(I18n.t("val_valoracio_rang"));

		if (preu != null && preu < 0)
			throw new IllegalArgumentException(I18n.t("val_preu_negatiu"));

		return Llibre.builder()
			.isbn(isbn).nom(nom)
			.autor(autor != null ? autor : "")
			.any(any != null ? any : 0)
			.descripcio(descripcio != null ? descripcio : "")
			.valoracio(valoracio != null ? valoracio : 0.0)
			.preu(preu != null ? preu : 0.0)
			.llegit(llegit != null ? llegit : false)
			.imatge(portada != null ? portada : "")
			.build();
	}

	/** Valida els camps principals i escriu els valors normalitzats a
	 *  {@code target}; els extres (notes, pagines, …) es queden a target. */
	public static void validarInto(Llibre target, Long isbn, String nom, String autor, Integer any,
			String descripcio, Double valoracio, Double preu, Boolean llegit, String portada) {
		Llibre v = comprovarLlibre(isbn, nom, autor, any, descripcio, valoracio, preu, llegit, portada);
		Llibre.vincularUpdateableFields(target, v.obtenirISBN(), v.obtenirNom(), v.obtenirAutor(), v.obtenirAny(),
			v.obtenirDescripcio(), v.obtenirValoracio(), v.obtenirPreu(), v.obtenirLlegit(), v.obtenirImatge());
	}

	/** Valida els camps de cadena opcionals que tenen un límit VARCHAR a la BBDD. Llança si el superen. */
	public static void validarExtras(String editorial, String serie) {
		if (editorial != null && editorial.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_editorial_llarg"));
		if (serie != null && serie.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_serie_llarg"));
	}

	public static void validarExtrasAll(String editorial, String serie, String idioma, String format, String paisOrigen, String estat) {
		validarExtras(editorial, serie);
		if (idioma != null && idioma.length() > 100)
			throw new IllegalArgumentException(I18n.t("val_idioma_llarg"));
		if (format != null && format.length() > 50)
			throw new IllegalArgumentException(I18n.t("val_format_llarg"));
		if (paisOrigen != null && paisOrigen.length() > 100)
			throw new IllegalArgumentException(I18n.t("val_pais_llarg"));
		if (estat != null && estat.length() > 50)
			throw new IllegalArgumentException(I18n.t("val_estat_llarg"));
	}

	private static int comptarDig(long n) {
		return Long.toString(Math.abs(n)).length();
	}
}
