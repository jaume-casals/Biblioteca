package herramienta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenLibraryClient {

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(OpenLibraryClient.class.getName());

	private OpenLibraryClient() {}

	private static final long MIN_REQUEST_INTERVAL_MS = 300;
	private static long lastRequestMs = 0;

	/** Connect timeout (ms) per OL HTTP call — keeps the EDT responsive when OL hangs. */
	private static final int CONNECT_TIMEOUT_MS = 6000;
	/** Read timeout (ms) per OL HTTP call — slow OL response = IOException, not indefinite hang. */
	private static final int READ_TIMEOUT_MS = 10000;

	private static synchronized void rateLimit() throws InterruptedException {
		long now = System.currentTimeMillis();
		long wait = MIN_REQUEST_INTERVAL_MS - (now - lastRequestMs);
		if (wait > 0) Thread.sleep(wait);
		lastRequestMs = System.currentTimeMillis();
	}

	// @VisibleForTesting — test-only override of base URL / retry policy.
	// Public so the test tree (which lives in the default package, not herramienta)
	// can read the previous value to restore it after a test. Not thread-safe;
	// tests must not run OpenLibraryClient in parallel while these are set.
	public static String testBaseUrl = null;
	public static int testMaxRetries = -1;
	public static long testRetryBaseMs = -1;

	private static final int   MAX_RETRIES    = 3;
	private static final long  RETRY_BASE_MS  = 500;

	private static String base() {
		return testBaseUrl != null ? testBaseUrl : "https://openlibrary.org";
	}

	/** Lookup by ISBN. Returns map with keys: title, autor, any, descripcio, pagines, editorial, idioma. On network/HTTP error adds "error" key. */
	public static Map<String, String> lookupByISBN(String isbn) {
		Map<String, String> r = new HashMap<>();
		try {
			String json = fetchWithRetry(base() + "/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data");
			JsonObject root;
			try {
				root = JsonParser.parseString(json).getAsJsonObject();
			} catch (RuntimeException e) {
				r.put("error", "Malformed JSON response: " + e.getMessage());
				return r;
			}
			JsonObject book = null;
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				if (e.getValue().isJsonObject()) { book = e.getValue().getAsJsonObject(); break; }
			}
			if (book != null) {
				put(r, "title",      jsonStr(book, "title"));
				put(r, "descripcio", jsonStrNested(book, "description", "value"));
				if (r.get("descripcio") == null) put(r, "descripcio", jsonStr(book, "description"));
				String date = jsonStr(book, "publish_date");
				if (date != null) {
					Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(date);
					if (m.find()) r.put("any", m.group(1));
				}
				if (book.has("number_of_pages") && !book.get("number_of_pages").isJsonNull())
					r.put("pagines", String.valueOf(book.get("number_of_pages").getAsInt()));
				put(r, "editorial", jsonArrayFirstField(book, "publishers", "name"));
				put(r, "autor",     jsonArrayFirstField(book, "authors",    "name"));
				if (book.has("languages")) {
					JsonArray langs = book.getAsJsonArray("languages");
					if (langs.size() > 0) {
						String key = jsonStr(langs.get(0).getAsJsonObject(), "key");
						if (key != null) r.put("idioma", key.replaceAll(".*/", ""));
					}
				}
			}
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
		}
		if (!r.containsKey("title") && !r.containsKey("error")) {
			try { mergeGoogleBooks(isbn, r); } catch (Exception ignored) {}
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

	/** Search by title. Returns first result with keys: title, autor, any, isbn. On network/HTTP error adds "error" key. */
	public static Map<String, String> lookupByTitle(String title) {
		return lookupBySearch("title=" + encode(title));
	}

	/** Search by author name. Same return shape as lookupByTitle. On network/HTTP error adds "error" key. */
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

	// Fallback order for cover images:
	//   1. OpenLibrary covers.openlibrary.org/b/isbn/{isbn}-L.jpg  (Large, 400px)
	//   2. Google Books thumbnail (only if OL returns GIF or 404)
	// Base URLs are overridable via testBaseUrl for test mocking.
	private static final String COVER_BASE = "https://covers.openlibrary.org";
	private static final String COVER_SIZE  = "-L"; // Large; -M (Medium) or -S (Small) also available but not tried

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
				boolean isGif = ct != null && ct.startsWith("image/gif");
				if (!isGif) {
					try (java.io.InputStream is = conn.getInputStream()) {
						result = is.readAllBytes();
					}
				}
			}
		} catch (Exception e) {
			LOG.log(Level.FINE, "fetchCoverByISBN primary URL failed for " + isbn, e);
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
			Matcher thumb = Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"").matcher(meta);
			if (!thumb.find()) return null;
			String thumbUrl = thumb.group(1).replace("http://", "https://");
			HttpURLConnection c2 = (HttpURLConnection) URI.create(thumbUrl).toURL().openConnection();
			c2.setConnectTimeout(CONNECT_TIMEOUT_MS); c2.setReadTimeout(READ_TIMEOUT_MS);
			c2.setRequestProperty("User-Agent", "Biblioteca/1.0");
			if (c2.getResponseCode() != 200) { c2.disconnect(); return null; }
			try (java.io.InputStream is = c2.getInputStream()) { result = is.readAllBytes(); }
			c2.disconnect();
		} catch (Exception e) {
			LOG.log(Level.FINE, "fetchCoverByISBN google fallback failed for " + isbn, e);
		}
		return result;
	}

	private static String fetchWithRetry(String url) throws java.io.IOException {
		try { rateLimit(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new java.io.IOException("Interrupted while rate-limiting", ie); }
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
						throw new java.io.IOException("Interrupted during retry backoff", ie);
					}
				}
			}
		}
		throw last != null ? last : new java.io.IOException("No retries performed for " + url);
	}

	private static String fetch(String url) throws java.io.IOException {
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
		} catch (IllegalArgumentException e) {
			throw new java.io.IOException("Malformed URL: " + url, e);
		}
		conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
		conn.setReadTimeout(READ_TIMEOUT_MS);
		conn.setRequestProperty("User-Agent", "Biblioteca/1.0");
		int code = conn.getResponseCode();
		if (code != 200) {
			try { if (conn.getErrorStream() != null) conn.getErrorStream().close(); } catch (Exception ignored) {}
			conn.disconnect();
			throw new java.io.IOException("HTTP " + code);
		}
		try (java.io.InputStream is = conn.getInputStream()) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} finally { conn.disconnect(); }
	}

	private static String jsonStr(JsonObject obj, String key) {
		if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
		JsonElement e = obj.get(key);
		return e.isJsonPrimitive() ? e.getAsString() : null;
	}

	private static String jsonStrNested(JsonObject obj, String key, String subKey) {
		if (!obj.has(key) || !obj.get(key).isJsonObject()) return null;
		return jsonStr(obj.getAsJsonObject(key), subKey);
	}

	private static String jsonArrayFirstField(JsonObject obj, String arrayKey, String fieldKey) {
		if (!obj.has(arrayKey) || !obj.get(arrayKey).isJsonArray()) return null;
		JsonArray arr = obj.getAsJsonArray(arrayKey);
		if (arr.isEmpty()) return null;
		JsonElement first = arr.get(0);
		if (!first.isJsonObject()) return null;
		return jsonStr(first.getAsJsonObject(), fieldKey);
	}

	private static String encode(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static void put(Map<String, String> map, String key, String val) {
		if (val != null && !val.isBlank()) map.put(key, val.trim());
	}
}
