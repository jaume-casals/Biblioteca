package api;

import interficie.BibliotecaWriter;
import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
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
            for (domini.LlibreLlistaRow row : dom.getAllLlibreLlistaRows()) {
                llistaMap.computeIfAbsent(row.isbn(), k -> new ArrayList<>())
                    .add(Map.of("id", row.llistaId(), "valoracio", row.valoracio(), "llegit", row.llegit()));
            }
            for (domini.LlibreTagRow row : dom.getAllLlibreTagRows()) {
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
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        int ok = 0, skipped = 0, err = 0;
        List<String> errDetails = new ArrayList<>();

        Map<Integer, Integer> tagIdMap = new HashMap<>();
        if (root.has("tags")) {
            for (JsonElement te : root.getAsJsonArray("tags")) {
                JsonObject to = te.getAsJsonObject();
                int oldId = to.get("id").getAsInt();
                String nom = to.get("nom").getAsString();
                Tag existing = cd.getAllTags().stream().filter(t -> t.getNom().equals(nom)).findFirst().orElse(null);
                tagIdMap.put(oldId, existing != null ? existing.getId() : cd.addTag(nom).getId());
            }
        }
        Map<Integer, Integer> llistaIdMap = new HashMap<>();
        if (root.has("llistes")) {
            for (JsonElement le : root.getAsJsonArray("llistes")) {
                JsonObject lo = le.getAsJsonObject();
                int oldId = lo.get("id").getAsInt();
                String nom = lo.get("nom").getAsString();
                Llista existing = cd.getAllLlistes().stream().filter(l -> l.getNom().equals(nom)).findFirst().orElse(null);
                llistaIdMap.put(oldId, existing != null ? existing.getId() : cd.addLlista(nom).getId());
            }
        }
        if (root.has("llibres")) {
            for (JsonElement be : root.getAsJsonArray("llibres")) {
                try {
                    JsonObject bo = be.getAsJsonObject();
                    long isbn = bo.get("isbn").getAsLong();
                    try { cd.getLlibre(isbn); skipped++; continue; } catch (Exception ignored) {}
                    Llibre l = JsonMapper.jsonToLlibre(bo);
                    cd.addLlibre(l);
                    if (bo.has("tags") && bo.get("tags").isJsonArray()) {
                        for (JsonElement te : bo.getAsJsonArray("tags")) {
                            int oldTagId = te.getAsJsonObject().get("id").getAsInt();
                            Integer newId = tagIdMap.get(oldTagId);
                            if (newId != null) cd.addLlibreToTag(isbn, newId);
                        }
                    }
                    if (bo.has("llistes") && bo.get("llistes").isJsonArray()) {
                        for (JsonElement me : bo.getAsJsonArray("llistes")) {
                            JsonObject mo = me.getAsJsonObject();
                            Integer newLlistaId = llistaIdMap.get(mo.get("id").getAsInt());
                            if (newLlistaId == null) continue;
                            double val = mo.has("valoracio") ? mo.get("valoracio").getAsDouble() : 0.0;
                            boolean llegit = mo.has("llegit") && mo.get("llegit").getAsBoolean();
                            cd.addLlibreToLlista(isbn, newLlistaId, val, llegit);
                        }
                    }
                    ok++;
                } catch (Exception e) { err++; errDetails.add(e.getMessage()); }
            }
        }
        ctx.json(Map.of("ok", ok, "skipped", skipped, "errors", err, "errorDetails", errDetails));
    }

    private void exportGoodreadsCSV(HttpCtx ctx) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,")
          .append("Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,")
          .append("Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,")
          .append("Read Count,Recommended For,Recommended By,Owned Copies,Original Purchase Date,")
          .append("Condition,Condition Description,BCID\n");
        // Build isbn→llistes map in bulk to avoid N+1 queries
        Map<Integer, Llista> llistaById = new HashMap<>();
        for (Llista ll : cd.getAllLlistes()) llistaById.put(ll.getId(), ll);
        Map<Long, List<Llista>> llibLlistes = new HashMap<>();
        for (domini.LlibreLlistaRow row : cd.getAllLlibreLlistaRows()) {
            Llista ll = llistaById.get(row.llistaId());
            if (ll != null) llibLlistes.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(ll);
        }
        int rowId = 1;
        for (Llibre l : cd.getAllLlibres()) {
            List<Llista> llistes = llibLlistes.getOrDefault(l.getISBN(), Collections.emptyList());
            String shelf = Boolean.TRUE.equals(l.getLlegit()) ? "read" : (!llistes.isEmpty() ? llistes.get(0).getNom() : "to-read");
            String bookshelves = llistes.stream().map(Llista::getNom)
                .collect(java.util.stream.Collectors.joining(", "));
            sb.append(rowId++).append(',')
              .append(CsvUtils.csvQ(l.getNom())).append(',')
              .append(CsvUtils.csvQ(l.getAutor().toString())).append(',')
              .append(CsvUtils.csvQ(l.getAutor().toString())).append(',')
              .append(',') // Additional Authors
              .append(CsvUtils.csvQ("=\"" + l.getISBN() + "\"")).append(',')
              .append(CsvUtils.csvQ("=\"" + l.getISBN() + "\"")).append(',')
              .append(l.getValoracio() > 0 ? (int) Math.round(l.getValoracio() / 2.0) : 0).append(',')
              .append(',') // Average Rating
              .append(CsvUtils.csvQ(l.getEditorial() != null ? l.getEditorial() : "")).append(',')
              .append(CsvUtils.csvQ(l.getFormat() != null ? l.getFormat() : "")).append(',')
              .append(l.getPagines() > 0 ? l.getPagines() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(CsvUtils.csvQ(l.getDataLectura() != null ? l.getDataLectura() : "")).append(',')
              .append(CsvUtils.csvQ(l.getDataCompra() != null ? l.getDataCompra() : "")).append(',')
              .append(CsvUtils.csvQ(bookshelves)).append(',')
              .append(CsvUtils.csvQ(shelf)).append(',')
              .append(CsvUtils.csvQ(l.getNotes() != null ? l.getNotes() : "")).append(',')
              .append(",,,,,,,,,\n"); // remaining empty columns
        }
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
            .filter(s -> s.canHandle(headerLine)).findFirst().get();
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
    }

}
