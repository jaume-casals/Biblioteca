package api;

import interficie.BibliotecaWriter;
import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.BookImporter.ImportResult;
import herramienta.JsonImporter;
import herramienta.OpenLibraryClient;
import herramienta.csv.CsvUtils;
import herramienta.csv.GoodreadsCsvStrategy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ImportExportRouter {

    private final BibliotecaWriter cd;

    public ImportExportRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/export/json",         ctx -> exportJson(ctx));
        app.get("/api/export/csv/goodreads",ctx -> exportGoodreadsCSV(ctx));
        app.post("/api/import/json",        ctx -> importJson(ctx));
        app.post("/api/import/csv",         ctx -> importCsv(ctx));
        app.post("/api/covers/fetch",       ctx -> fetchCovers(ctx));
    }

    private void exportJson(HttpCtx ctx) throws Exception {
        // Build isbn→llistes and isbn→tags maps with bulk queries to avoid N+1
        Map<Long, List<Map<String, Object>>> llistaMap = new HashMap<>();
        Map<Long, List<Map<String, Object>>> tagMap = new HashMap<>();
        if (cd instanceof ControladorDomini dom) {
            for (persistencia.LlibreLlistaRow row : dom.getAllLlibreLlistaRows()) {
                llistaMap.computeIfAbsent(row.isbn(), k -> new ArrayList<>())
                    .add(Map.of("id", row.llistaId(), "valoracio", row.valoracio(), "llegit", row.llegit()));
            }
            for (persistencia.LlibreTagRow row : dom.getAllLlibreTagRows()) {
                tagMap.computeIfAbsent(row.isbn(), k -> new ArrayList<>())
                    .add(Map.of("id", row.tagId()));
            }
        }
        List<Map<String, Object>> llibres = new ArrayList<>();
        for (Llibre l : cd.getAllLlibres()) {
            Map<String, Object> m = JsonMapper.llibreToMap(l);
            m.put("llistes", llistaMap.getOrDefault(l.getISBN(), List.of()));
            m.put("tags", tagMap.getOrDefault(l.getISBN(), List.of()));
            llibres.add(m);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);
        root.put("llibres", llibres);
        root.put("llistes", cd.getAllLlistes().stream().map(JsonMapper::llistaToMap)
            .collect(java.util.stream.Collectors.toList()));
        root.put("tags", cd.getAllTags().stream().map(JsonMapper::tagToMap)
            .collect(java.util.stream.Collectors.toList()));
        ctx.responseHeader("Content-Disposition", "attachment; filename=\"biblioteca.json\"");
        ctx.json(root);
    }

    private void importJson(HttpCtx ctx) throws Exception {
        String body = ctx.body();
        if (body == null || body.isBlank()) throw new Exception("Empty body");
        ImportResult r = JsonImporter.run(JsonParser.parseString(body).getAsJsonObject(), cd);
        ctx.json(Map.of("ok", r.imported(), "skipped", r.skipped(), "errors", r.errors(), "errorDetails", r.errorDetails()));
    }

    private void exportGoodreadsCSV(HttpCtx ctx) throws Exception {
        String csv = herramienta.export.GoodreadsExportService.exportToCsv(cd);
        StringBuilder sb = new StringBuilder(csv);
        ctx.responseHeader("Content-Disposition", "attachment; filename=\"goodreads_export.csv\"");
        ctx.responseHeader("Content-Type", "text/csv; charset=UTF-8");
        ctx.result(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void importCsv(HttpCtx ctx) throws Exception {
        String body = ctx.body();
        if (body == null || body.isBlank()) throw new Exception("Empty body");
        String[] lines = body.split("\r?\n", -1);
        if (lines.length < 2) throw new Exception("CSV has no data rows");
        String headerLine = lines[0];
        String[] headers = CsvUtils.parseLine(headerLine);
        Map<String, Integer> hMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) hMap.put(headers[i].trim(), i);
        List<herramienta.csv.CsvImportStrategy> strategies = List.of(
            new herramienta.csv.LibraryThingCsvStrategy(),
            new GoodreadsCsvStrategy(),
            new herramienta.csv.NativeCsvStrategy());
        herramienta.csv.CsvImportStrategy strategy = strategies.stream()
            .filter(s -> s.canHandle(headerLine)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No CSV strategy matched header: " + headerLine));
        int ok = 0, skipped = 0, err = 0;
        List<String> errDetails = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            try {
                if (strategy.parseLine(CsvUtils.parseLine(lines[i]), hMap, cd)) ok++;
                else skipped++;
            } catch (Exception e) { err++; errDetails.add(e.getMessage()); }
        }
        ctx.json(Map.of("ok", ok, "skipped", skipped, "errors", err, "errorDetails", errDetails));
    }

    private void fetchCovers(HttpCtx ctx) throws Exception {
        List<Llibre> missing = cd.getAllLlibres().stream()
            .filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
            .collect(java.util.stream.Collectors.toList());
        int total = missing.size();
        ctx.json(Map.of("queued", total));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(4,
            r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        for (Llibre l : missing) {
            pool.submit(() -> {
                try {
                    byte[] blob = OpenLibraryClient.fetchCoverByISBN(String.valueOf(l.getISBN()));
                    if (blob != null && blob.length > 0) cd.setLlibreBlob(l.getISBN(), blob);
                } catch (Exception ignored) {}
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES);
    }

}
