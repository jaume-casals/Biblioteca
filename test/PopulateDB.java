package test;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.LlibreValidator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches books from OpenLibrary and inserts them into the local database.
 * Run: java -cp lib/h2-2.3.232.jar:bin test.PopulateDB [max_books]
 * Default max: 2000
 */
public class PopulateDB {

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT    = 15_000;
    private static final int LIMIT_PER_QUERY = 100;

    // Diverse search terms — each fetches up to 100 books
    private static final String[] QUERIES = {
        "subject:fiction",        "subject:science",        "subject:history",
        "subject:philosophy",     "subject:biography",      "subject:mystery",
        "subject:romance",        "subject:horror",         "subject:fantasy",
        "subject:science+fiction","subject:thriller",       "subject:politics",
        "subject:economics",      "subject:psychology",     "subject:art",
        "subject:poetry",         "subject:drama",          "subject:adventure",
        "subject:children",       "subject:religion",       "subject:nature",
        "subject:music",          "subject:sports",         "subject:cooking",
        "subject:travel",         "subject:health",         "subject:technology",
        "subject:mathematics",    "subject:astronomy",      "subject:medicine",
        "author:tolkien",         "author:asimov",          "author:king",
        "author:hemingway",       "author:dickens",         "author:shakespeare",
        "author:orwell",          "author:rowling",         "author:kafka",
        "author:dostoevsky",      "author:tolstoy",         "author:austen",
        "author:twain",           "author:homer",           "author:cervantes",
        "author:borges",          "author:marquez",         "author:camus",
        "author:sartre",          "author:nietzsche",       "author:darwin",
        "author:freud",           "author:einstein",        "author:feynman",
        "author:hawking",         "author:sagan",           "author:chomsky",
        "author:vonnegut",        "author:steinbeck",       "author:faulkner",
        "author:fitzgerald",      "author:woolf",           "author:joyce",
        "author:nabokov",         "author:dumas",           "author:hugo",
        "author:flaubert",        "author:zola",            "author:balzac",
        "author:chekhov",         "author:turgenev",        "author:pushkin",
        "author:ibsen",           "author:strindberg",      "author:brecht",
        "author:beckett",         "author:pinter",          "author:stoppard",
        "language:eng&subject:classic novel",
        "language:eng&subject:world war",
        "language:eng&subject:american literature",
        "language:eng&subject:british literature",
        "language:eng&subject:short stories",
        "language:spa&subject:literatura",
        "language:cat&subject:literatura",
        "language:fre&subject:litterature",
        "language:ger&subject:literatur",
        "language:ita&subject:letteratura",
    };

    public static void main(String[] args) throws Exception {
        int maxBooks = args.length > 0 ? Integer.parseInt(args[0]) : 2000;

        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:" + System.getProperty("user.home") + "/.biblioteca/biblioteca" +
            ";MODE=MySQL;NON_KEYWORDS=VALUE");

        System.out.println("Connecting to database...");
        ControladorDomini cd = ControladorDomini.getInstance();
        int before = cd.getSize();
        System.out.println("Books currently in DB: " + before);
        System.out.println("Target: +" + maxBooks + " new books\n");

        Set<Long> existingISBNs = new HashSet<>();
        for (Llibre l : cd.getAllLlibres()) existingISBNs.add(l.getISBN());

        int inserted = 0;
        int skipped  = 0;
        int errors   = 0;

        outer:
        for (String q : QUERIES) {
            if (inserted >= maxBooks) break;

            System.out.printf("  Querying: %s%n", q);
            List<BookRecord> books = searchOpenLibrary(q, LIMIT_PER_QUERY);
            System.out.printf("    → %d results%n", books.size());

            for (BookRecord b : books) {
                if (inserted >= maxBooks) break outer;
                if (b.isbn == null || existingISBNs.contains(b.isbn)) { skipped++; continue; }
                try {
                    Llibre l = LlibreValidator.checkLlibre(
                        b.isbn, b.title, b.author, b.year, b.subject,
                        null, null, false, "");
                    cd.addLlibre(l);
                    existingISBNs.add(b.isbn);
                    inserted++;
                    if (inserted % 50 == 0)
                        System.out.printf("    [%d inserted so far]%n", inserted);
                } catch (Exception e) {
                    errors++;
                }
            }

            // Polite delay between queries
            Thread.sleep(500);
        }

        System.out.printf("%n══════════════════════════════════════%n");
        System.out.printf("  Inserted : %d%n", inserted);
        System.out.printf("  Skipped  : %d (duplicate/no ISBN)%n", skipped);
        System.out.printf("  Errors   : %d%n", errors);
        System.out.printf("  Total DB : %d%n", cd.getSize());
        System.out.printf("══════════════════════════════════════%n");
    }

    // ── OpenLibrary search ───────────────────────────────────────────────────

    private static List<BookRecord> searchOpenLibrary(String query, int limit) {
        List<BookRecord> results = new ArrayList<>();
        try {
            String encoded = query.replace(" ", "+");
            String url = "https://openlibrary.org/search.json?q=" + encoded
                + "&limit=" + limit
                + "&fields=title,author_name,first_publish_year,isbn,subject";
            String json = fetch(url);
            if (json == null) return results;

            // Extract docs array
            int docsStart = json.indexOf("\"docs\"");
            if (docsStart < 0) return results;
            int arrStart = json.indexOf('[', docsStart);
            if (arrStart < 0) return results;

            // Split into individual doc objects
            List<String> docs = splitJsonArray(json, arrStart);
            for (String doc : docs) {
                BookRecord b = parseDoc(doc);
                if (b != null) results.add(b);
            }
        } catch (Exception e) {
            System.out.println("    [fetch error: " + e.getMessage() + "]");
        }
        return results;
    }

    private static List<String> splitJsonArray(String json, int start) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        int itemStart = -1;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) itemStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && itemStart >= 0) {
                    items.add(json.substring(itemStart, i + 1));
                    itemStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return items;
    }

    private static BookRecord parseDoc(String doc) {
        String title  = extractString(doc, "title");
        String author = extractArrayFirst(doc, "author_name");
        String isbn13 = pickBestISBN(doc);
        String subj   = extractArrayFirst(doc, "subject");
        Integer year  = null;

        Matcher ym = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(doc);
        if (ym.find()) {
            try { year = Integer.parseInt(ym.group(1)); } catch (NumberFormatException ignored) {}
        }

        if (title == null || title.isBlank() || isbn13 == null) return null;
        return new BookRecord(isbn13 == null ? null : parseLong(isbn13), title, author, year, subj);
    }

    private static String pickBestISBN(String doc) {
        Matcher m = Pattern.compile("\"isbn\"\\s*:\\s*\\[([^\\]]+)\\]").matcher(doc);
        if (!m.find()) return null;
        String arr = m.group(1);
        Matcher im = Pattern.compile("\"(\\d+)\"").matcher(arr);
        String best13 = null, best10 = null;
        while (im.find()) {
            String candidate = im.group(1);
            if (candidate.length() == 13 && best13 == null) best13 = candidate;
            if (candidate.length() == 10 && best10 == null) best10 = candidate;
        }
        return best13 != null ? best13 : best10;
    }

    private static Long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("User-Agent", "Biblioteca/1.0 (jordicasals29@gmail.com)");
        conn.setRequestProperty("Accept", "application/json");
        if (conn.getResponseCode() != 200) return null;
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
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private static String extractArrayFirst(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // ── Data class ───────────────────────────────────────────────────────────

    static class BookRecord {
        Long isbn; String title, author, subject; Integer year;
        BookRecord(Long isbn, String title, String author, Integer year, String subject) {
            this.isbn = isbn; this.title = title; this.author = author;
            this.year = year; this.subject = subject;
        }
    }
}
