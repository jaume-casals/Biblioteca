package herramienta.text;

import domini.Llibre;
import herramienta.i18n.I18n;

public class ValidadorLlibre {

    private ValidadorLlibre() {}

	public static Llibre comprovarLlibreFromString(String isbnStr, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {
		if (isbnStr == null || isbnStr.isBlank())
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		String trimmed = isbnStr.trim();
		String rawDigits = trimmed.replaceAll("[^0-9X]", "").toUpperCase(java.util.Locale.ROOT);
		if (rawDigits.length() != 13 && rawDigits.length() != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		// Validació del dígit de control ISBN-10 ABANS de la normalització
		// a ISBN-13 — la versió anterior convertia primer i mai no veia el
		// dígit de control incorrecte, produint un ISBN-13 amb un dígit
		// de control "recalculat correctament" que emmascarava l'error
		// d'entrada.
		if (rawDigits.length() == 10 && !isValidIsbn10(rawDigits))
			throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));
		String normalized = normalizeIsbn13(trimmed);
		String digits = normalized.replaceAll("[^0-9]", "");
		// Validació del dígit de control ISBN-13 (camí d'entrada de l'usuari)
		if (digits.length() == 13 && (digits.charAt(12) - '0') != checkDigit13(digits))
			throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));
		try {
			return comprovarLlibre(Long.parseLong(digits), nom, autor, any, descripcio, valoracio, preu, llegit, portada);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));
		}
	}

	/** Calcula el dígit de control EAN-13 a partir dels 12 primers caràcters de {@code base12}.
	 *  Package-private per compartir amb {@link Isbn13Normalizer#convertToIsbn13(String)}. */
	static int checkDigit13(String base12) {
		int sum = 0;
		for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
		return (10 - sum % 10) % 10;
	}

	/** Comprova el dígit de control ISBN-10 (suma ponderada × (10 - posició),
	 *  mòd 11). El dígit de control pot ser 'X' (que representa 10). */
	private static boolean isValidIsbn10(String digits) {
		int sum = 0;
		for (int i = 0; i < 9; i++) {
			char c = digits.charAt(i);
			if (c < '0' || c > '9') return false;
			sum += (c - '0') * (10 - i);
		}
		int check = (11 - sum % 11) % 11;
		char last = digits.charAt(9);
		if (last == 'X') return check == 10;
		if (last < '0' || last > '9') return false;
		return (last - '0') == check;
	}

	/** Valida un ISBN cru (10 o 13 dígits, amb 'X' permesa a la posició
	 *  de control ISBN-10) sense convertir-lo a {@code long}. Preserva
	 *  el dígit 'X' que es perd en una conversió a tipus primitiu. */
	public static boolean isValidIsbn(String isbn) {
		if (isbn == null) return false;
		String raw = isbn.replaceAll("[^0-9Xx]", "").toUpperCase(java.util.Locale.ROOT);
		if (raw.length() == 10) return isValidIsbn10(raw);
		if (raw.length() == 13) {
			for (int i = 0; i < 13; i++) {
				char c = raw.charAt(i);
				if (c < '0' || c > '9') return false;
			}
			return (raw.charAt(12) - '0') == checkDigit13(raw);
		}
		return false;
	}

	private static String normalizeIsbn13(String isbn) {
		if (isbn == null) return null;
		// ISBN-10 amb dígit de control X (p. ex. "019853110X") — converteix a ISBN-13
		if (isbn.length() > 0 && Character.toUpperCase(isbn.charAt(isbn.length() - 1)) == 'X') {
			String core = isbn.substring(0, isbn.length() - 1).replaceAll("[^0-9]", "");
			if (core.length() == 9) {
				String base12 = "978" + core;
				isbn = base12 + checkDigit13(base12);
			}
		}
		String digits = isbn.replaceAll("[^0-9]", "");
		// ISBN-10 que comença amb 0 → ISBN-13: evita la pèrdua del zero inicial quan s'emmagatzema com a Long
		if (digits.length() == 10 && digits.charAt(0) == '0') {
			String base12 = "978" + digits.substring(0, 9);
			digits = base12 + checkDigit13(base12);
		}
		return digits.length() == 13 ? digits : isbn;
	}

	/**
	 * Valida i construeix un Llibre.
	 * Només isbn i nom són obligatoris. Tots els altres camps accepten null/buit i
	 * reben valors per defecte segurs (valoracio=0, preu=0, llegit=false, la resta cadenes buides).
	 */
	public static Llibre comprovarLlibre(Long isbn, String nom, String autor, Integer any, String descripcio,
			Double valoracio, Double preu, Boolean llegit, String portada) {

		if (isbn == null)
			throw new IllegalArgumentException(I18n.t("toast_isbn_required"));

		int digits = comptarDig(isbn);
		if (digits == 9) {
			return comprovarLlibreFromString("0" + Long.toString(isbn), nom, autor, any, descripcio,
				valoracio, preu, llegit, portada);
		}
		if (digits != 13 && digits != 10)
			throw new IllegalArgumentException(I18n.t("val_isbn_digits"));

		// Validació del dígit de control ISBN-10. La comprovació X
		// (p.ex. "020161622X") es delega a {@link #isValidIsbn(String)};
		// aquest mètode rep un {@code Long} que ja ha perdut el 'X',
		// per la qual cosa la validació X es fa exclusivament al camí
		// d'entrada de cadena.
		if (digits == 10 && !isValidIsbn10(Long.toString(isbn)))
			throw new IllegalArgumentException(I18n.t("val_isbn_invalid"));

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

	private record LimitCamp(String i18nKey, int max) {}
	private static final LimitCamp[] EXTRA_LIMITS = {
		new LimitCamp("val_editorial_llarg", 255),
		new LimitCamp("val_serie_llarg", 255),
		new LimitCamp("val_idioma_llarg", 100),
		new LimitCamp("val_format_llarg", 50),
		new LimitCamp("val_pais_llarg", 100),
		new LimitCamp("val_estat_llarg", 50),
	};
	private static void checkLimits(String[] vals) {
		for (int i = 0; i < EXTRA_LIMITS.length && i < vals.length; i++) {
			String v = vals[i];
			LimitCamp l = EXTRA_LIMITS[i];
			if (v != null && v.length() > l.max())
				throw new IllegalArgumentException(I18n.t(l.i18nKey()));
		}
	}

	/** Valida els camps de cadena opcionals que tenen un límit VARCHAR a la BBDD. Llança si el superen. */
	public static void validarExtras(String editorial, String serie) {
		checkLimits(new String[]{editorial, serie});
	}

	public static void validarExtrasAll(String editorial, String serie, String idioma, String format, String paisOrigen, String estat) {
		checkLimits(new String[]{editorial, serie, idioma, format, paisOrigen, estat});
	}

	private static int comptarDig(long n) {
		return Long.toString(Math.abs(n)).length();
	}
}
