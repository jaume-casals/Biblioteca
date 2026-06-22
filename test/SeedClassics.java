import domini.ControladorDomini;
import domini.Llibre;
import herramienta.text.ValidadorLlibre;

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
 * Buida la biblioteca i la reomple amb llibres clàssics (≤2000) de tres
 * categories: detectius, novel·la, i assassinats. Cerca a OpenLibrary,
 * filtra per any, valida i insereix via ControladorDomini.
 *
 * Executa: java -cp lib/h2-2.3.232.jar:bin test.SeedClassics [cap]
 * Defecte: 500 llibres.
 */
public class SeedClassics {

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT    = 15_000;
    private static final int LIMIT_PER_QUERY = 100;
    private static final int DEFAULT_CAP     = 500;
    private static final int MAX_YEAR        = 2000;

    private static final String[] DETECTIVE_QUERIES = {
        "author:arthur conan doyle",
        "author:agatha christie",
        "author:dashiell hammett",
        "author:raymond chandler",
        "author:georges simenon",
        "author:edgar allan poe",
        "author:wilkie collins",
        "author:g k chesterton",
        "subject:detective fiction",
        "subject:mystery classic",
    };

    private static final String[] NOVEL_QUERIES = {
        "author:leo tolstoy",
        "author:fyodor dostoevsky",
        "author:victor hugo",
        "author:charles dickens",
        "author:jane austen",
        "author:emily bronte",
        "author:charlotte bronte",
        "author:stendhal",
        "author:gustave flaubert",
        "author:honore de balzac",
        "author:herman melville",
        "author:mark twain",
        "author:thomas hardy",
        "author:joseph conrad",
        "author:henry james",
        "author:virginia woolf",
        "author:james joyce",
        "author:william faulkner",
        "author:ernest hemingway",
        "author:f scott fitzgerald",
        "author:john steinbeck",
        "author:gabriel garcia marquez",
        "author:jorge luis borges",
        "author:albert camus",
        "author:italo calvino",
        "subject:russian classic novel",
        "subject:19th century novel",
        "subject:gothic novel",
    };

    private static final String[] ASSASSINATION_QUERIES = {
        "author:frederick forsyth",
        "subject:assassin fiction",
        "subject:assassination novel",
    };

    private static final String[] ALL_QUERIES;
    static {
        ALL_QUERIES = new String[DETECTIVE_QUERIES.length + NOVEL_QUERIES.length + ASSASSINATION_QUERIES.length];
        int i = 0;
        for (String q : DETECTIVE_QUERIES)      ALL_QUERIES[i++] = q;
        for (String q : NOVEL_QUERIES)          ALL_QUERIES[i++] = q;
        for (String q : ASSASSINATION_QUERIES)  ALL_QUERIES[i++] = q;
    }

    public static void main(String[] args) throws Exception {
        int cap = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_CAP;

        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:" + System.getProperty("user.home") + "/.biblioteca/biblioteca" +
            ";MODE=MySQL;NON_KEYWORDS=VALUE");

        System.out.println("Connectant a la base de dades...");
        ControladorDomini cd = ControladorDomini.getInstance();
        int before = cd.getSize();
        System.out.println("Llibres actuals a la BD: " + before);

        if (before > 0) {
            System.out.println("Buidant la BD (cd.netejarAll())...");
            cd.netejarAll();
            System.out.println("Després de buidar: " + cd.getSize() + " llibres");
        }
        System.out.println();
        System.out.println("Objectiu: fins a " + cap + " llibres clàssics (≤" + MAX_YEAR + ")");
        System.out.println("Categories: " + DETECTIVE_QUERIES.length + " detectiu + "
            + NOVEL_QUERIES.length + " novel·la + "
            + ASSASSINATION_QUERIES.length + " assassinats = "
            + ALL_QUERIES.length + " consultes a OpenLibrary");
        System.out.println();

        Set<Long> seenISBNs = new HashSet<>();
        int inserted = 0, skipped = 0, errors = 0, yearFiltered = 0;

        outer:
        for (String q : ALL_QUERIES) {
            if (inserted >= cap) break;
            System.out.printf("  q: %s%n", q);
            List<RegistreLlibre> books = cercarOpenLibrary(q, LIMIT_PER_QUERY);
            System.out.printf("    → %d resultats bruts%n", books.size());

            for (RegistreLlibre b : books) {
                if (inserted >= cap) break outer;
                if (b.year == null || b.year > MAX_YEAR) { yearFiltered++; continue; }
                if (b.isbn == null || seenISBNs.contains(b.isbn)) { skipped++; continue; }
                try {
                    Llibre l = ValidadorLlibre.comprovarLlibre(
                        b.isbn, b.title, b.author, b.year, b.subject,
                        null, null, false, "");
                    cd.afegirLlibre(l);
                    seenISBNs.add(b.isbn);
                    inserted++;
                    if (inserted % 25 == 0)
                        System.out.printf("    [%d inserits]%n", inserted);
                } catch (Exception e) {
                    errors++;
                }
            }
            Thread.sleep(500);
        }

        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.printf ("  Inserits        : %d%n", inserted);
        System.out.printf ("  Filtrat per any : %d (year>=" + (MAX_YEAR+1) + " o null)%n", yearFiltered);
        System.out.printf ("  Omesos          : %d (duplicat/sense ISBN)%n", skipped);
        System.out.printf ("  Errors          : %d%n", errors);
        System.out.printf ("  Total BD        : %d%n", cd.getSize());
        System.out.println("══════════════════════════════════════");
    }

    private static List<RegistreLlibre> cercarOpenLibrary(String query, int limit) {
        List<RegistreLlibre> results = new ArrayList<>();
        try {
            String encoded = query.replace(" ", "+");
            String url = "https://openlibrary.org/search.json?q=" + encoded
                + "&limit=" + limit
                + "&fields=title,author_name,first_publish_year,isbn,subject";
            String json = fetch(url);
            if (json == null) return results;

            int docsStart = json.indexOf("\"docs\"");
            if (docsStart < 0) return results;
            int arrStart = json.indexOf('[', docsStart);
            if (arrStart < 0) return results;

            List<String> docs = splitJsonArray(json, arrStart);
            for (String doc : docs) {
                RegistreLlibre b = analitzarDoc(doc);
                if (b != null) results.add(b);
            }
        } catch (Exception e) {
            System.out.println("    [error de fetch: " + e.getMessage() + "]");
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

    private static RegistreLlibre analitzarDoc(String doc) {
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
        return new RegistreLlibre(parseLong(isbn13), title, author, year, subj);
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

    static class RegistreLlibre {
        Long isbn; String title, author, subject; Integer year;
        RegistreLlibre(Long isbn, String title, String author, Integer year, String subject) {
            this.isbn = isbn; this.title = title; this.author = author;
            this.year = year; this.subject = subject;
        }
    }
}