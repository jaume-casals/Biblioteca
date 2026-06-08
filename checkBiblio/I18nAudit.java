package checkBiblio;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/** Static i18n checks for UIAudit (3-language CSV, hardcoded Catalan, unused keys). */
final class I18nAudit {

    private I18nAudit() {}

    static void run(PrintWriter log, int[] failCount, int[] warnCount) {
        Path stringsDir = Path.of("strings");
        Map<String, String[]> keys = loadCsvKeys(stringsDir);
        auditThreeLanguages(keys, log, failCount);
        auditHardcodedCatalan(log, warnCount);
        auditJavaKeyUsage(keys, log, failCount, warnCount);
        auditDuplicateKeys(stringsDir, log, warnCount);
    }

    private static Map<String, String[]> loadCsvKeys(Path dir) {
        Map<String, String[]> out = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".csv")).toList()) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#") || s.startsWith("key,")) continue;
                    String[] row = splitCsvLine(s);
                    if (row.length >= 4) {
                        String key = stripQuotes(row[0]);
                        if (!key.isEmpty()) out.put(key, new String[]{stripQuotes(row[1]), stripQuotes(row[2]), stripQuotes(row[3])});
                    }
                }
            }
        } catch (IOException e) {
            out.clear();
        }
        return out;
    }

    /** Parses a single CSV line honouring double-quoted fields. Returns the fields in order. */
    private static String[] splitCsvLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else if (c == '"') {
                    inQuote = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuote = true;
            } else if (c == ',') {
                out.add(cur.toString()); cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static void auditThreeLanguages(Map<String, String[]> keys, PrintWriter log, int[] failCount) {
        for (var e : keys.entrySet()) {
            String[] v = e.getValue();
            for (int i = 0; i < 3; i++) {
                if (v[i] == null || v[i].isBlank()) {
                    failCount[0]++;
                    log.println("FAIL: i18n missing lang " + (i == 0 ? "ca" : i == 1 ? "es" : "en")
                        + " for key " + e.getKey());
                }
            }
        }
    }

    private static void auditHardcodedCatalan(PrintWriter log, int[] warnCount) {
        Pattern wordP = Pattern.compile(
            "\\b(llista|llibre|afegir|eliminar|guardar|tancar|biblioteca|prestatge|etiqueta)\\b",
            Pattern.CASE_INSENSITIVE);
        try (Stream<Path> walk = Files.walk(Path.of("src"))) {
            walk.filter(f -> f.toString().endsWith(".java"))
                .filter(f -> !f.toString().contains("I18n.java"))
                .forEach(f -> {
                    try {
                        String text = Files.readString(f, StandardCharsets.UTF_8);
                        if (text.contains("I18n.t(")) return;
                        Matcher m = wordP.matcher(text);
                        while (m.find()) {
                            int start = m.start();
                            int end = m.end();
                            if (!isInsideStringLiteral(text, start, end)) continue;
                            String word = m.group(1);
                            if (text.contains("I18n.t(\"" + word + "\"")
                                || text.contains("t(\"" + word + "\"")) continue;
                            warnCount[0]++;
                            log.println("WARN: possible hardcoded Catalan in " + f
                                + " (" + word + " in " + literalContext(text, start, end) + " @ " + start + ")");
                        }
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }

    private static boolean isInsideStringLiteral(String text, int wordStart, int wordEnd) {
        int openQuote = text.lastIndexOf('"', wordStart);
        if (openQuote < 0) return false;
        int closeQuote = text.indexOf('"', wordEnd);
        return closeQuote > openQuote && (closeQuote - openQuote) < 200;
    }

    private static String literalContext(String text, int wordStart, int wordEnd) {
        int openQuote = text.lastIndexOf('"', wordStart);
        int closeQuote = text.indexOf('"', wordEnd);
        if (openQuote < 0 || closeQuote < 0) return "";
        String lit = text.substring(openQuote, closeQuote + 1);
        return truncate(lit, 60);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static void auditJavaKeyUsage(Map<String, String[]> keys, PrintWriter log, int[] failCount, int[] warnCount) {
        Pattern used = Pattern.compile("\\b(?:I18n\\.t|t)\\(\\s*\"([^\"]+)\"");
        Set<String> seen = new HashSet<>();
        try (Stream<Path> walk = Files.walk(Path.of("src"))) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(f, StandardCharsets.UTF_8);
                Matcher m = used.matcher(text);
                while (m.find()) {
                    String k = m.group(1);
                    if (k.endsWith("_")) continue;
                    seen.add(k);
                }
            }
        } catch (IOException ignored) {}
        for (String k : keys.keySet()) {
            if (!seen.contains(k)) {
                warnCount[0]++;
                log.println("WARN: i18n key never used in src: " + k);
            }
        }
        for (String k : seen) {
            if (!keys.containsKey(k)) {
                failCount[0]++;
                log.println("FAIL: I18n.t key missing from CSV: " + k);
            }
        }
    }

    private static void auditDuplicateKeys(Path dir, PrintWriter log, int[] warnCount) {
        Map<String, String> firstFile = new HashMap<>();
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".csv")).toList()) {
                Set<String> seenInFile = new HashSet<>();
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#") || s.startsWith("key,")) continue;
                    String key = stripQuotes(splitCsvLine(s)[0]);
                    if (!seenInFile.add(key)) {
                        warnCount[0]++;
                        log.println("WARN: duplicate i18n key " + key + " in " + p.getFileName());
                    }
                    String prev = firstFile.putIfAbsent(key, p.getFileName().toString());
                    if (prev != null && !prev.equals(p.getFileName().toString())) {
                        warnCount[0]++;
                        log.println("WARN: duplicate i18n key " + key + " in " + prev + " and " + p.getFileName());
                    }
                }
            }
        } catch (IOException ignored) {}
    }
}
