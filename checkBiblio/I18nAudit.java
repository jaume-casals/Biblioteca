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
                    String[] row = s.split(",", 4);
                    if (row.length >= 4) out.put(row[0].trim(), new String[]{row[1], row[2], row[3]});
                }
            }
        } catch (IOException e) {
            out.clear();
        }
        return out;
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
        Pattern p = Pattern.compile(
            "\\b(llista|llibre|afegir|eliminar|guardar|tancar|biblioteca|prestatge|etiqueta)\\b",
            Pattern.CASE_INSENSITIVE);
        try (Stream<Path> walk = Files.walk(Path.of("src"))) {
            walk.filter(f -> f.toString().endsWith(".java"))
                .filter(f -> !f.toString().contains("I18n.java"))
                .forEach(f -> {
                    try {
                        String text = Files.readString(f, StandardCharsets.UTF_8);
                        if (text.contains("I18n.t(")) return;
                        Matcher m = p.matcher(text);
                        while (m.find()) {
                            warnCount[0]++;
                            log.println("WARN: possible hardcoded Catalan in " + f
                                + " (" + m.group() + " @ " + m.start() + ")");
                        }
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }

    private static void auditJavaKeyUsage(Map<String, String[]> keys, PrintWriter log, int[] failCount, int[] warnCount) {
        Pattern used = Pattern.compile("I18n\\.t\\(\\s*\"([^\"]+)\"");
        Set<String> seen = new HashSet<>();
        try (Stream<Path> walk = Files.walk(Path.of("src"))) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(f, StandardCharsets.UTF_8);
                Matcher m = used.matcher(text);
                while (m.find()) seen.add(m.group(1));
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
                    String key = s.split(",", 2)[0].trim();
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
