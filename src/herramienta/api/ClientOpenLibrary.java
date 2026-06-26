package herramienta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import herramienta.io.JsonHelpers;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientOpenLibrary {

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClientOpenLibrary.class.getName());

	private ClientOpenLibrary() {}

	private static final long MIN_REQUEST_INTERVAL_MS = 300;
	private static final AtomicLong ultimRequestMs = new AtomicLong(0);

	/** Timeout de connexió (ms) per a cada crida HTTP a OL — manté l'EDT
	 *  responsiu quan OL penja. */
	private static final int CONNECT_TIMEOUT_MS = 6000;
	/** Timeout de lectura (ms) per a cada crida HTTP a OL — resposta lenta
	 *  d'OL = IOException, no penjat indefinit. */
	private static final int READ_TIMEOUT_MS = 10000;

	private static void rateLimit() throws InterruptedException {
		long now = System.currentTimeMillis();
		long next = ultimRequestMs.getAndUpdate(prev -> Math.max(prev + MIN_REQUEST_INTERVAL_MS, now));
		long wait = next - now;
		if (wait > 0) Thread.sleep(wait);
	}

	// @VisibleForTesting — override de la URL base / política de reintents
	// només per a tests. Public perquè l'arbre de tests (que viu al
	// package per defecte, no a herramienta) pugui llegir el valor
	// anterior per restaurar-lo després d'un test. Volatile perquè un
	// test concurrent que posa testBaseUrl després que un consumidor de
	// producció ja l'hagi llegit observi el nou valor (o, a la inversa,
	// un test a mig muntatge no sigui observat amb un estat mig
	// establert per una crida de producció). Els tests no poden executar
	// OpenLibraryClient en paral·lel mentre aquests valors estiguin
	// posats — el Javadoc de cada camp documenta el contracte.
	public static volatile String testBaseUrl = null;
	public static volatile int testMaxRetries = -1;
	public static volatile long testRetryBaseMs = -1;

	private static final int   MAX_RETRIES    = 3;
	private static final long  RETRY_BASE_MS  = 500;

	private static String base() {
		return testBaseUrl != null ? testBaseUrl : "https://openlibrary.org";
	}

	/** Cerca per ISBN. Retorna un mapa amb les claus: title, autor, any,
	 *  descripcio, pagines, editorial, idioma. En cas d'error de xarxa/HTTP
	 *  afegeix la clau "error". */
	public static Map<String, String> lookupByISBN(String isbn) {
		Map<String, String> r = new HashMap<>();
		try {
			String json = fetchWithRetry(base() + "/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data");
			try {
				r.putAll(AnalitzadorOpenLibrary.analitzarIsbnResponse(json));
			} catch (RuntimeException e) {
				r.put("error", "Malformed JSON response: " + e.getMessage());
				return r;
			}
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
		}
		if (!r.containsKey("title") && !r.containsKey("error")) {
			try { mergeGoogleBooks(isbn, r); } catch (Exception e) {
				java.util.logging.Logger.getLogger(ClientOpenLibrary.class.getName())
					.log(java.util.logging.Level.FINE, "Ha fallat la fusió amb Google Books per a l'ISBN " + isbn, e);
			}
		}
		return r;
	}

	private static void mergeGoogleBooks(String isbn, Map<String, String> r) throws java.io.IOException {
		String encoded = java.net.URLEncoder.encode("isbn:" + isbn, StandardCharsets.UTF_8);
		String json = fetch("https://www.googleapis.com/books/v1/volumes?q=" + encoded + "&maxResults=1");
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (!root.has("totalItems") || root.get("totalItems").getAsInt() == 0) return;
		if (!root.has("items")) return;
		JsonObject info = root.getAsJsonArray("items").get(0).getAsJsonObject()
			.getAsJsonObject("volumeInfo");
		put(r, "title",      jsonStr(info, "title"));
		put(r, "editorial",  jsonStr(info, "publisher"));
		put(r, "descripcio", jsonStr(info, "description"));
		put(r, "idioma",     jsonStr(info, "language"));
		if (info.has("authors") && info.getAsJsonArray("authors").size() > 0)
			r.put("autor", info.getAsJsonArray("authors").get(0).getAsString());
		if (info.has("pageCount")) r.put("pagines", String.valueOf(info.get("pageCount").getAsInt()));
		if (info.has("publishedDate")) {
			Matcher yr = Pattern.compile("^(\\d{4})").matcher(info.get("publishedDate").getAsString());
			if (yr.find()) r.put("any", yr.group(1));
		}
		if (info.has("imageLinks")) {
			String thumb = jsonStr(info.getAsJsonObject("imageLinks"), "thumbnail");
			put(r, "thumbnailUrl", thumb);
		}
	}

	/** Cerca per títol. Retorna el primer resultat amb les claus: title,
	 *  autor, any, isbn. En cas d'error de xarxa/HTTP afegeix la clau
	 *  "error". */
	public static Map<String, String> lookupByTitle(String title) {
		return lookupBySearch("title=" + encode(title));
	}

	/** Cerca pel nom de l'autor. Mateixa forma de retorn que
	 *  lookupByTitle. En cas d'error de xarxa/HTTP afegeix la clau
	 *  "error". */
	public static Map<String, String> lookupByAutor(String autor) {
		return lookupBySearch("author=" + encode(autor));
	}

	private static Map<String, String> lookupBySearch(String param) {
		Map<String, String> r = new HashMap<>();
		try {
			String json = fetchWithRetry(base() + "/search.json?" + param
				+ "&limit=1&fields=title,author_name,first_publish_year,isbn");
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			if (!root.has("docs") || root.getAsJsonArray("docs").isEmpty()) return r;
			JsonObject doc = root.getAsJsonArray("docs").get(0).getAsJsonObject();
			put(r, "title", jsonStr(doc, "title"));
			if (doc.has("author_name") && doc.getAsJsonArray("author_name").size() > 0)
				r.put("autor", doc.getAsJsonArray("author_name").get(0).getAsString());
			if (doc.has("isbn") && doc.getAsJsonArray("isbn").size() > 0)
				r.put("isbn", doc.getAsJsonArray("isbn").get(0).getAsString());
			if (doc.has("first_publish_year"))
				r.put("any", String.valueOf(doc.get("first_publish_year").getAsInt()));
		} catch (Exception e) {
			r.put("error", e.getMessage());
		}
		return r;
	}

	// Ordre de caiguda per a imatges de coberta:
	//   1. OpenLibrary covers.openlibrary.org/b/isbn/{isbn}-L.jpg  (Gran, 400px)
	//   2. Miniatura de Google Books (només si OL retorna GIF o 404)
	// Les URLs base es poden sobreescriure via testBaseUrl per a mocks de test.
	private static final String COVER_BASE = "https://covers.openlibrary.org";
	private static final String COVER_SIZE  = "-L"; // Gran; -M (Mitjana) o -S (Petita) també disponibles però no es proven

	public static byte[] fetchCoverByISBN(String isbn) {
		try { rateLimit(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
		String coverBase = testBaseUrl != null ? testBaseUrl : COVER_BASE;
		String url = coverBase + "/b/isbn/" + isbn + COVER_SIZE + ".jpg";
		byte[] result = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			conn.setRequestProperty("User-Agent", "Biblioteca/1.0");
			if (conn.getResponseCode() == 200) {
				String ct = conn.getContentType();
				boolean esGif = ct != null && ct.startsWith("image/gif");
				if (!esGif) {
					try (java.io.InputStream is = conn.getInputStream()) {
						result = is.readAllBytes();
					}
				}
			}
		} catch (Exception e) {
			LOG.log(Level.FINE, "fetchCoverByISBN: ha fallat l'URL primària per a " + isbn, e);
		} finally {
			if (conn != null) {
				try { if (conn.getErrorStream() != null) conn.getErrorStream().close(); } catch (Exception ignored) {}
				conn.disconnect();
			}
		}
		if (result != null || testBaseUrl != null) return result;
		try {
			String encoded = java.net.URLEncoder.encode("isbn:" + isbn, StandardCharsets.UTF_8);
			String meta = fetch("https://www.googleapis.com/books/v1/volumes?q=" + encoded + "&maxResults=1");
			// Analitza la resposta de Google Books de la mateixa manera
			// que mergeGoogleBooks (l'anàlisi anterior amb regex fallava
			// amb les cometes escapades dins la URL de la miniatura —
			// algunes entrades de Google Books les incrusten — i amb
			// miniatures `data:image/jpeg;base64,…` la regex capturava
			// tot el prefix com a URL). Reutilitza el mateix recorregut
			// de JsonObject.
			JsonObject gbRoot;
			try { gbRoot = JsonParser.parseString(meta).getAsJsonObject(); }
			catch (RuntimeException analitzarEx) {
				LOG.log(Level.FINE, "fetchCoverByISBN fallback de Google: JSON mal format per a " + isbn, analitzarEx);
				return null;
			}
			if (!gbRoot.has("items")) return null;
			JsonArray items = gbRoot.getAsJsonArray("items");
			if (items.isEmpty()) return null;
			JsonObject info = items.get(0).getAsJsonObject();
			if (!info.has("volumeInfo")) return null;
			JsonObject volumeInfo = info.getAsJsonObject("volumeInfo");
			if (!volumeInfo.has("imageLinks")) return null;
			JsonObject imageLinks = volumeInfo.getAsJsonObject("imageLinks");
			if (!imageLinks.has("thumbnail")) return null;
			String thumbUrl = imageLinks.get("thumbnail").getAsString().replace("http://", "https://");
			HttpURLConnection c2 = null;
			try {
				c2 = (HttpURLConnection) URI.create(thumbUrl).toURL().openConnection();
				c2.setConnectTimeout(CONNECT_TIMEOUT_MS); c2.setReadTimeout(READ_TIMEOUT_MS);
				c2.setRequestProperty("User-Agent", "Biblioteca/1.0");
				if (c2.getResponseCode() != 200) return null;
				try (java.io.InputStream is = c2.getInputStream()) { result = is.readAllBytes(); }
			} finally {
				if (c2 != null) {
					try { if (c2.getErrorStream() != null) c2.getErrorStream().close(); } catch (Exception ignored) {}
					c2.disconnect();
				}
			}
		} catch (Exception e) {
			LOG.log(Level.FINE, "Ha fallat el fallback de Google a fetchCoverByISBN per a " + isbn, e);
		}
		return result;
	}

	private static String fetchWithRetry(String url) throws java.io.IOException {
		try { rateLimit(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new java.io.IOException("Interromput durant la limitació de freqüència", ie); }
		int retries = testMaxRetries >= 0 ? testMaxRetries : MAX_RETRIES;
		long baseMs = testRetryBaseMs >= 0 ? testRetryBaseMs : RETRY_BASE_MS;
		if (retries <= 0) return fetch(url);
		java.io.IOException last = null;
		for (int i = 0; i < retries; i++) {
			try {
				return fetch(url);
			} catch (java.io.IOException e) {
				last = e;
				if (i < retries - 1) {
					try { Thread.sleep(baseMs * (1L << i)); }
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new java.io.IOException("Interromput durant el retrocés entre reintents", ie);
					}
				}
			}
		}
		throw last != null ? last : new java.io.IOException("No s'han fet reintents per a " + url);
	}

	private static String fetch(String url) throws java.io.IOException {
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
		} catch (IllegalArgumentException e) {
			throw new java.io.IOException("Malformed URL: " + url, e);
		}
		try {
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			conn.setRequestProperty("User-Agent", "Biblioteca/1.0");
			int code = conn.getResponseCode();
			if (code != 200) {
				try { if (conn.getErrorStream() != null) conn.getErrorStream().close(); } catch (Exception ignored) {}
				throw new java.io.IOException("HTTP " + code);
			}
			try (java.io.InputStream is = conn.getInputStream()) {
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		} finally {
			conn.disconnect();
		}
	}

	private static String jsonStr(JsonObject obj, String key) {
		return JsonHelpers.jsonStr(obj, key);
	}

	private static String encode(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static void put(Map<String, String> map, String key, String val) {
		if (val != null && !val.isBlank()) map.put(key, val.trim());
	}
}
