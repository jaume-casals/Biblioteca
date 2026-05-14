package api;

import interficie.BibliotecaWriter;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.LlibreValidator;
import herramienta.OpenLibraryClient;

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
        List<Map<String, Object>> llibres = new ArrayList<>();
        for (Llibre l : cd.getAllLlibres()) {
            Map<String, Object> m = JsonMapper.llibreToMap(l);
            m.put("llistes", cd.getLlistesForLlibre(l.getISBN()).stream()
                .map(ll -> Map.of("id", ll.getId(), "valoracio", ll.getValoracioLlibre(),
                    "llegit", Boolean.TRUE.equals(ll.getLlegitLlibre())))
                .collect(java.util.stream.Collectors.toList()));
            m.put("tags", cd.getTagsForLlibre(l.getISBN()).stream()
                .map(t -> Map.of("id", t.getId()))
                .collect(java.util.stream.Collectors.toList()));
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
                } catch (Exception e) { err++; }
            }
        }
        ctx.json(Map.of("ok", ok, "skipped", skipped, "errors", err));
    }

    private void exportGoodreadsCSV(HttpCtx ctx) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,")
          .append("Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,")
          .append("Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,")
          .append("Read Count,Recommended For,Recommended By,Owned Copies,Original Purchase Date,")
          .append("Condition,Condition Description,BCID\n");
        int rowId = 1;
        for (Llibre l : cd.getAllLlibres()) {
            List<Llista> llistes = cd.getLlistesForLlibre(l.getISBN());
            String shelf = Boolean.TRUE.equals(l.getLlegit()) ? "read" : (!llistes.isEmpty() ? llistes.get(0).getNom() : "to-read");
            String bookshelves = llistes.stream().map(Llista::getNom)
                .collect(java.util.stream.Collectors.joining(", "));
            sb.append(rowId++).append(',')
              .append(csvQ(l.getNom())).append(',')
              .append(csvQ(l.getAutor().toString())).append(',')
              .append(csvQ(l.getAutor().toString())).append(',')
              .append(',') // Additional Authors
              .append(csvQ("=\"" + l.getISBN() + "\"")).append(',')
              .append(csvQ("=\"" + l.getISBN() + "\"")).append(',')
              .append(l.getValoracio() > 0 ? (int) Math.round(l.getValoracio() / 2.0) : 0).append(',')
              .append(',') // Average Rating
              .append(csvQ(l.getEditorial() != null ? l.getEditorial() : "")).append(',')
              .append(csvQ(l.getFormat() != null ? l.getFormat() : "")).append(',')
              .append(l.getPagines() > 0 ? l.getPagines() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(l.getAny() > 0 ? l.getAny() : "").append(',')
              .append(csvQ(l.getDataLectura() != null ? l.getDataLectura() : "")).append(',')
              .append(csvQ(l.getDataCompra() != null ? l.getDataCompra() : "")).append(',')
              .append(csvQ(bookshelves)).append(',')
              .append(csvQ(shelf)).append(',')
              .append(csvQ(l.getNotes() != null ? l.getNotes() : "")).append(',')
              .append(",,,,,,,,,\n"); // remaining empty columns
        }
        ctx.responseHeader("Content-Disposition", "attachment; filename=\"goodreads_export.csv\"");
        ctx.responseHeader("Content-Type", "text/csv; charset=UTF-8");
        ctx.result(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csvQ(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void importCsv(HttpCtx ctx) throws Exception {
        String body = ctx.body();
        if (body == null || body.isBlank()) throw new Exception("Empty body");
        String[] lines = body.split("\r?\n", -1);
        if (lines.length < 2) throw new Exception("CSV has no data rows");
        String[] headers = parseCsvLine(lines[0]);
        Map<String, Integer> hMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) hMap.put(headers[i].trim(), i);
        boolean isLibraryThing = hMap.containsKey("BCID");
        boolean isGoodreads = !isLibraryThing &&
            (hMap.containsKey("Book Id") || hMap.containsKey("Exclusive Shelf"));
        int ok = 0, skipped = 0, err = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            try {
                String[] c = parseCsvLine(lines[i]);
                if (isLibraryThing) {
                    if (importLibraryThingRow(c, hMap)) ok++; else skipped++;
                } else if (isGoodreads) {
                    if (importGoodreadsRow(c, hMap)) ok++; else skipped++;
                } else {
                    long isbn = Long.parseLong(c[0].trim());
                    try { cd.getLlibre(isbn); skipped++; continue; } catch (Exception ignored) {}
                    Llibre l = LlibreValidator.checkLlibre(isbn, c[1], c[2],
                        Integer.parseInt(c[3].trim()),
                        c.length > 4 ? c[4] : "",
                        c.length > 5 ? parseDoubleOr(c[5], 0.0) : 0.0,
                        c.length > 6 ? parseDoubleOr(c[6], 0.0) : 0.0,
                        c.length > 7 && Boolean.parseBoolean(c[7].trim()),
                        c.length > 8 ? c[8] : "");
                    cd.addLlibre(l);
                    ok++;
                }
            } catch (Exception e) { err++; }
        }
        ctx.json(Map.of("ok", ok, "skipped", skipped, "errors", err));
    }

    private boolean importLibraryThingRow(String[] c, Map<String, Integer> hMap) throws Exception {
        String isbnRaw = colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = colVal(hMap, c, "ISBN");
        isbnRaw = isbnRaw.replaceAll("[^0-9]", "");
        if (isbnRaw.isEmpty()) return false;
        long isbn;
        try { isbn = Long.parseLong(isbnRaw); } catch (NumberFormatException e) { return false; }
        try { cd.getLlibre(isbn); return false; } catch (Exception ignored) {}
        String nom    = colVal(hMap, c, "Title");
        String autor  = colVal(hMap, c, "Authors");
        // LibraryThing stores "Last, First" — convert to "First Last" if single author
        if (autor.contains(",") && !autor.contains(";")) {
            String[] parts = autor.split(",", 2);
            autor = parts[1].trim() + " " + parts[0].trim();
        }
        int any = 0;
        String yearStr = colVal(hMap, c, "Original Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = parseDoubleOr(colVal(hMap, c, "Rating"), 0.0) * 2.0; // LT uses 0.5–5 stars → 0–10
        String desc    = colVal(hMap, c, "Summary");
        String notes   = colVal(hMap, c, "Comments");
        if (notes.isEmpty()) notes = colVal(hMap, c, "Review");
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, desc, valoracio, 0.0, false, "");
        if (!notes.isEmpty()) l.setNotes(notes);
        cd.addLlibre(l);
        String collections = colVal(hMap, c, "Collections");
        if (!collections.isEmpty()) {
            for (String s : collections.split(",")) {
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                Llista llista = cd.getAllLlistes().stream()
                    .filter(ll -> ll.getNom().equals(nomLlista)).findFirst().orElse(null);
                if (llista == null) llista = cd.addLlista(nomLlista);
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, false);
            }
        }
        String tags = colVal(hMap, c, "Tags");
        if (!tags.isEmpty()) {
            for (String t : tags.split(",")) {
                String nomTag = t.trim();
                if (nomTag.isEmpty()) continue;
                Tag tag = cd.getAllTags().stream()
                    .filter(tg -> tg.getNom().equals(nomTag)).findFirst().orElse(null);
                if (tag == null) tag = cd.addTag(nomTag);
                cd.addLlibreToTag(isbn, tag.getId());
            }
        }
        return true;
    }

    private boolean importGoodreadsRow(String[] c, Map<String, Integer> hMap) throws Exception {
        String isbnRaw = colVal(hMap, c, "ISBN13");
        if (isbnRaw.isEmpty()) isbnRaw = colVal(hMap, c, "ISBN");
        isbnRaw = isbnRaw.replaceAll("[^0-9]", "");
        if (isbnRaw.isEmpty()) throw new Exception("ISBN buit");
        long isbn = Long.parseLong(isbnRaw);
        try { cd.getLlibre(isbn); return false; } catch (Exception ignored) {}
        String nom       = colVal(hMap, c, "Title");
        String autor     = colVal(hMap, c, "Author");
        String editorial = colVal(hMap, c, "Publisher");
        String pagesStr  = colVal(hMap, c, "Number of Pages");
        int pagines = pagesStr.isEmpty() ? 0 : (int) parseDoubleOr(pagesStr, 0.0);
        int any = 0;
        String yearStr = colVal(hMap, c, "Year Published");
        if (yearStr.isEmpty()) yearStr = colVal(hMap, c, "Original Publication Year");
        if (!yearStr.isEmpty()) { try { any = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {} }
        double valoracio = parseDoubleOr(colVal(hMap, c, "My Rating"), 0.0);
        String shelf    = colVal(hMap, c, "Exclusive Shelf");
        boolean llegit  = "read".equalsIgnoreCase(shelf);
        String notes    = colVal(hMap, c, "My Review");
        String dataLect = colVal(hMap, c, "Date Read");
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, "", valoracio, 0.0, llegit, "");
        l.setEditorial(editorial);
        l.setPagines(pagines);
        l.setNotes(notes);
        if (!dataLect.isEmpty()) l.setDataLectura(dataLect);
        cd.addLlibre(l);
        String bookshelves = colVal(hMap, c, "Bookshelves");
        if (!bookshelves.isEmpty()) {
            for (String s : bookshelves.split(",")) {
                String nomLlista = s.trim();
                if (nomLlista.isEmpty()) continue;
                Llista llista = cd.getAllLlistes().stream()
                    .filter(ll -> ll.getNom().equals(nomLlista)).findFirst().orElse(null);
                if (llista == null) llista = cd.addLlista(nomLlista);
                cd.addLlibreToLlista(isbn, llista.getId(), valoracio, llegit);
            }
        }
        return true;
    }

    private void fetchCovers(HttpCtx ctx) throws Exception {
        List<Llibre> missing = cd.getAllLlibres().stream()
            .filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
            .collect(java.util.stream.Collectors.toList());
        int total = missing.size();
        ctx.json(Map.of("queued", total));
        Thread worker = new Thread(() -> {
            for (Llibre l : missing) {
                try {
                    byte[] blob = OpenLibraryClient.fetchCoverByISBN(String.valueOf(l.getISBN()));
                    if (blob != null && blob.length > 0) cd.setLlibreBlob(l.getISBN(), blob);
                } catch (Exception ignored) {}
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private static String colVal(Map<String, Integer> hMap, String[] c, String col) {
        Integer idx = hMap.get(col);
        if (idx == null || idx >= c.length) return "";
        return c[idx].trim();
    }

    private static double parseDoubleOr(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else { inQuote = !inQuote; }
            } else if (ch == ',' && !inQuote) {
                fields.add(sb.toString()); sb.setLength(0);
            } else { sb.append(ch); }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }
}
