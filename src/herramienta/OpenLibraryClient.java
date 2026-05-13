package herramienta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenLibraryClient {

	private OpenLibraryClient() {}

	/** Override base URL in tests to simulate network errors (e.g. "http://localhost:1"). */
	public static String testBaseUrl = null;
	/** Override retry count in tests (default 3). Set to 1 to disable retries. */
	public static int testMaxRetries = -1;
	/** Override retry base delay ms in tests (default 500). Set to 0 for instant retries. */
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
			put(r, "title",      extractString(json, "title"));
			put(r, "autor",      extractString(json, "name"));
			put(r, "descripcio", extractString(json, "description"));
			String date = extractString(json, "publish_date");
			if (date != null) {
				Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(date);
				if (m.find()) r.put("any", m.group(1));
			}
			Matcher pages = Pattern.compile("\"number_of_pages\"\\s*:\\s*(\\d+)").matcher(json);
			if (pages.find()) r.put("pagines", pages.group(1));
			put(r, "editorial", extractArrayFirstObject(json, "publishers", "name"));
			Matcher lang = Pattern.compile("\"/languages/([a-z]+)\"").matcher(json);
			if (lang.find()) r.put("idioma", lang.group(1));
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
		}
		// Google Books fallback if OpenLibrary returned nothing useful
		if (!r.containsKey("title") && !r.containsKey("error")) {
			try { mergeGoogleBooks(isbn, r); } catch (Exception ignored) {}
		}
		return r;
	}

	private static void mergeGoogleBooks(String isbn, Map<String, String> r) throws java.io.IOException {
		String encoded = java.net.URLEncoder.encode("isbn:" + isbn, StandardCharsets.UTF_8);
		String json = fetch("https://www.googleapis.com/books/v1/volumes?q=" + encoded + "&maxResults=1");
		Matcher total = Pattern.compile("\"totalItems\"\\s*:\\s*(\\d+)").matcher(json);
		if (!total.find() || "0".equals(total.group(1))) return;
		put(r, "title",      extractString(json, "title"));
		// authors is an array in Google Books
		Matcher auth = Pattern.compile("\"authors\"\\s*:\\s*\\[\\s*\"([^\"]+)\"").matcher(json);
		if (auth.find()) r.put("autor", auth.group(1));
		put(r, "editorial", extractString(json, "publisher"));
		put(r, "descripcio", extractString(json, "description"));
		Matcher pg = Pattern.compile("\"pageCount\"\\s*:\\s*(\\d+)").matcher(json);
		if (pg.find()) r.put("pagines", pg.group(1));
		Matcher yr = Pattern.compile("\"publishedDate\"\\s*:\\s*\"(\\d{4})").matcher(json);
		if (yr.find()) r.put("any", yr.group(1));
		put(r, "idioma", extractString(json, "language"));
		// thumbnail URL stored so callers can download it
		Matcher thumb = Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
		if (thumb.find()) r.put("thumbnailUrl", thumb.group(1));
	}

	/**
	 * Search by title (or author). Returns first result with keys:
	 * title, autor, any, isbn. On network/HTTP error adds "error" key.
	 */
	public static Map<String, String> lookupByTitle(String title) {
		Map<String, String> r = new HashMap<>();
		try {
			String encoded = java.net.URLEncoder.encode(title, StandardCharsets.UTF_8);
			String json = fetchWithRetry(base() + "/search.json?title=" + encoded
				+ "&limit=1&fields=title,author_name,first_publish_year,isbn");
			put(r, "title", extractString(json, "title"));
			put(r, "autor", extractArrayFirst(json, "author_name"));
			put(r, "isbn",  extractArrayFirst(json, "isbn"));
			Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
			if (m.find()) r.put("any", m.group(1));
		} catch (Exception e) {
			r.put("error", e.getMessage());
		}
		return r;
	}

	/** Search by author name. Same return shape as lookupByTitle. On network/HTTP error adds "error" key. */
	public static Map<String, String> lookupByAutor(String autor) {
		Map<String, String> r = new HashMap<>();
		try {
			String encoded = java.net.URLEncoder.encode(autor, StandardCharsets.UTF_8);
			String json = fetchWithRetry(base() + "/search.json?author=" + encoded
				+ "&limit=1&fields=title,author_name,first_publish_year,isbn");
			put(r, "title", extractString(json, "title"));
			put(r, "autor", extractArrayFirst(json, "author_name"));
			put(r, "isbn",  extractArrayFirst(json, "isbn"));
			Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
			if (m.find()) r.put("any", m.group(1));
		} catch (Exception e) {
			r.put("error", e.getMessage());
		}
		return r;
	}

	/**
	 * Fetch cover image bytes for the given ISBN from OpenLibrary.
	 * Returns null if no cover found or on network error.
	 */
	public static byte[] fetchCoverByISBN(String isbn) {
		String coverBase = testBaseUrl != null ? testBaseUrl : "https://covers.openlibrary.org";
		String url = coverBase + "/b/isbn/" + isbn + "-L.jpg";
		try {
			HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("User-Agent", "Biblioteca/1.0");
			if (conn.getResponseCode() == 200) {
				try (java.io.InputStream is = conn.getInputStream()) {
					byte[] data = is.readAllBytes();
					// OpenLibrary returns a 1×1 GIF for missing covers — treat as not found
					if (data.length > 1000) return data;
				}
			}
		} catch (Exception ignored) {}
		// Google Books fallback
		if (testBaseUrl != null) return null; // skip remote fallback in tests
		try {
			String encoded = java.net.URLEncoder.encode("isbn:" + isbn, StandardCharsets.UTF_8);
			String meta = fetch("https://www.googleapis.com/books/v1/volumes?q=" + encoded + "&maxResults=1");
			Matcher thumb = Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"").matcher(meta);
			if (!thumb.find()) return null;
			String thumbUrl = thumb.group(1).replace("http://", "https://");
			HttpURLConnection c2 = (HttpURLConnection) URI.create(thumbUrl).toURL().openConnection();
			c2.setConnectTimeout(6000); c2.setReadTimeout(10000);
			c2.setRequestProperty("User-Agent", "Biblioteca/1.0");
			if (c2.getResponseCode() != 200) return null;
			try (java.io.InputStream is = c2.getInputStream()) { return is.readAllBytes(); }
		} catch (Exception ignored) {}
		return null;
	}

	private static String fetchWithRetry(String url) throws java.io.IOException {
		int retries = testMaxRetries >= 0 ? testMaxRetries : MAX_RETRIES;
		long baseMs = testRetryBaseMs >= 0 ? testRetryBaseMs : RETRY_BASE_MS;
		java.io.IOException last = null;
		for (int i = 0; i < retries; i++) {
			try {
				return fetch(url);
			} catch (java.io.IOException e) {
				last = e;
				if (i < retries - 1) {
					try { Thread.sleep(baseMs * (1L << i)); }
					catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
				}
			}
		}
		throw last;
	}

	private static String fetch(String url) throws java.io.IOException {
		HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
		conn.setConnectTimeout(6000);
		conn.setReadTimeout(6000);
		conn.setRequestProperty("User-Agent", "Biblioteca/1.0");
		int code = conn.getResponseCode();
		if (code != 200) throw new java.io.IOException("HTTP " + code);
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) sb.append(line);
		}
		return sb.toString();
	}

	private static String extractString(String json, String key) {
		Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
		if (m.find()) return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
		return null;
	}

	private static String extractArrayFirst(String json, String key) {
		Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
		if (m.find()) return m.group(1);
		return null;
	}

	private static String extractArrayFirstObject(String json, String arrayKey, String fieldKey) {
		Matcher arr = Pattern.compile("\"" + arrayKey + "\"\\s*:\\s*\\[\\s*\\{([^}]+)\\}").matcher(json);
		if (!arr.find()) return null;
		Matcher field = Pattern.compile("\"" + fieldKey + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(arr.group(1));
		return field.find() ? field.group(1) : null;
	}

	private static void put(Map<String, String> map, String key, String val) {
		if (val != null && !val.isBlank()) map.put(key, val.trim());
	}
}
