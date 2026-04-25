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

	/** Override base URL in tests to simulate network errors (e.g. "http://localhost:1"). */
	public static String testBaseUrl = null;

	private static String base() {
		return testBaseUrl != null ? testBaseUrl : "https://openlibrary.org";
	}

	/** Lookup by ISBN. Returns map with keys: title, autor, any, descripcio. On network/HTTP error adds "error" key. */
	public static Map<String, String> lookupByISBN(String isbn) {
		Map<String, String> r = new HashMap<>();
		try {
			String json = fetch(base() + "/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data");
			put(r, "title",     extractString(json, "title"));
			put(r, "autor",     extractString(json, "name"));
			put(r, "descripcio", extractString(json, "description"));
			String date = extractString(json, "publish_date");
			if (date != null) {
				Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(date);
				if (m.find()) r.put("any", m.group(1));
			}
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
		}
		return r;
	}

	/**
	 * Search by title (or author). Returns first result with keys:
	 * title, autor, any, isbn. On network/HTTP error adds "error" key.
	 */
	public static Map<String, String> lookupByTitle(String title) {
		Map<String, String> r = new HashMap<>();
		try {
			String encoded = java.net.URLEncoder.encode(title, StandardCharsets.UTF_8);
			String json = fetch(base() + "/search.json?title=" + encoded
				+ "&limit=1&fields=title,author_name,first_publish_year,isbn");
			put(r, "title", extractString(json, "title"));
			put(r, "autor", extractArrayFirst(json, "author_name"));
			put(r, "isbn",  extractArrayFirst(json, "isbn"));
			Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
			if (m.find()) r.put("any", m.group(1));
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
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
			String json = fetch(base() + "/search.json?author=" + encoded
				+ "&limit=1&fields=title,author_name,first_publish_year,isbn");
			put(r, "title", extractString(json, "title"));
			put(r, "autor", extractArrayFirst(json, "author_name"));
			put(r, "isbn",  extractArrayFirst(json, "isbn"));
			Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
			if (m.find()) r.put("any", m.group(1));
		} catch (java.io.IOException e) {
			r.put("error", e.getMessage());
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
			if (conn.getResponseCode() != 200) return null;
			try (java.io.InputStream is = conn.getInputStream()) {
				return is.readAllBytes();
			}
		} catch (Exception e) {
			return null;
		}
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

	private static void put(Map<String, String> map, String key, String val) {
		if (val != null && !val.isBlank()) map.put(key, val.trim());
	}
}
