package herramienta;

import domini.Llibre;

public class LlibreValidator {

    private LlibreValidator() {}

	public static Llibre checkLlibreFromString(String isbnStr, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {
		if (isbnStr == null || isbnStr.isBlank())
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		String trimmed = isbnStr.trim();
		// ISBN-10 with X check digit (e.g. "019853110X") — convert to ISBN-13
		if (trimmed.length() > 0 && Character.toUpperCase(trimmed.charAt(trimmed.length() - 1)) == 'X') {
			String core = trimmed.substring(0, trimmed.length() - 1).replaceAll("[^0-9]", "");
			if (core.length() == 9) {
				String base12 = "978" + core;
				int sum = 0;
				for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
				trimmed = base12 + (10 - sum % 10) % 10;
			}
		}
		String digits = trimmed.replaceAll("[^0-9]", "");
		// ISBN-10 starting with 0 → ISBN-13: avoids leading-zero loss when stored as Long
		if (digits.length() == 10 && digits.charAt(0) == '0') {
			String base12 = "978" + digits.substring(0, 9);
			int sum = 0;
			for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
			digits = base12 + (10 - sum % 10) % 10;
		}
		if (digits.length() != 13 && digits.length() != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		// ISBN-13 check digit validation (user-input path only)
		if (digits.length() == 13) {
			int sum = 0;
			for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
			if ((digits.charAt(12) - '0') != (10 - sum % 10) % 10)
				throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));
		}
		try {
			return checkLlibre(Long.parseLong(digits), nom, autor, any, descripcio, valoracio, preu, llegit, portada);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		}
	}

	/**
	 * Validates and builds a Llibre.
	 * Only isbn and nom are mandatory. All other fields accept null/blank and get
	 * safe defaults (valoracio=0, preu=0, llegit=false, rest empty strings).
	 */
	public static Llibre checkLlibre(Long isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {

		int digits = isbn == null ? 0 : countDig(isbn);
		if (digits != 13 && digits != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));

		if (nom == null || nom.isBlank())
			throw new IllegalArgumentException(I18n.t("val_nom_buit"));
		if (nom.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_nom_llarg"));

		if (autor != null && autor.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_autor_llarg"));

		int currentYear = java.time.Year.now().getValue();
		if (any != null && any != 0 && (any < 1000 || any > currentYear + 5))
			throw new IllegalArgumentException(I18n.t("val_any_rang", currentYear + 5));

		if (valoracio != null && (valoracio < 0 || valoracio > 10))
			throw new IllegalArgumentException(I18n.t("val_valoracio_rang"));

		if (preu != null && preu < 0)
			throw new IllegalArgumentException(I18n.t("val_preu_negatiu"));

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

	/** Validates optional string fields that have a DB VARCHAR limit. Throws if over limit. */
	public static void validateExtras(String editorial, String serie) {
		if (editorial != null && editorial.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_editorial_llarg"));
		if (serie != null && serie.length() > 255)
			throw new IllegalArgumentException(I18n.t("val_serie_llarg"));
	}

	public static void validateExtrasAll(String editorial, String serie, String idioma, String format, String paisOrigen, String estat) {
		validateExtras(editorial, serie);
		if (idioma != null && idioma.length() > 100)
			throw new IllegalArgumentException(I18n.t("val_idioma_llarg"));
		if (format != null && format.length() > 50)
			throw new IllegalArgumentException(I18n.t("val_format_llarg"));
		if (paisOrigen != null && paisOrigen.length() > 100)
			throw new IllegalArgumentException(I18n.t("val_pais_llarg"));
		if (estat != null && estat.length() > 50)
			throw new IllegalArgumentException(I18n.t("val_estat_llarg"));
	}

	private static int countDig(long n) {
		return String.valueOf(n).length();
	}
}
