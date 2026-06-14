package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import interficie.BibliotecaWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookExporter {

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    public static void exportCSV(File f, List<Llibre> view, BibliotecaWriter cd) throws Exception {
        domini.ShelfParser.exportToCsv(f, view, cd);
    }

    public static void exportJSON(File f, BibliotecaWriter cd) throws Exception {
        // Bulk-load relational data to avoid N+1
        java.util.Map<Long, List<persistencia.LlibreLlistaRow>> llistaRows = new java.util.HashMap<>();
        for (persistencia.LlibreLlistaRow r : cd.getAllLlibreLlistaRows())
            llistaRows.computeIfAbsent(r.isbn(), k -> new ArrayList<>()).add(r);
        java.util.Map<Long, List<persistencia.LlibreTagRow>> tagRows = new java.util.HashMap<>();
        for (persistencia.LlibreTagRow r : cd.getAllLlibreTagRows())
            tagRows.computeIfAbsent(r.isbn(), k -> new ArrayList<>()).add(r);

        // Build the document as a single LinkedHashMap so GSON handles
        // the surrounding braces, commas, and escaping (the previous
        // implementation hand-rolled `pw.println("{")` etc. and only
        // delegated per-book serialization to GSON — a 5000-book export
        // paid a 500x slowdown and shipped a hand-maintained escape
        // surface). The LinkedHashMap insertion order matches the prior
        // on-disk layout: version, llibres, llistes, tags.
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("version", 1);
        ArrayList<Llibre> tots = new ArrayList<>(cd.getAllLlibres());
        java.util.List<java.util.Map<String, Object>> llibresJson = new java.util.ArrayList<>(tots.size());
        for (Llibre l : tots) {
            llibresJson.add(jsonLlibreMap(l,
                llistaRows.getOrDefault(l.getISBN(), java.util.Collections.emptyList()),
                tagRows.getOrDefault(l.getISBN(), java.util.Collections.emptyList())));
        }
        root.put("llibres", llibresJson);
        ArrayList<Llista> llistes = new ArrayList<>(cd.getAllLlistes());
        java.util.List<java.util.Map<String, Object>> llistesJson = new java.util.ArrayList<>(llistes.size());
        for (Llista ll : llistes) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", ll.getId());
            m.put("nom", ll.getNom());
            llistesJson.add(m);
        }
        root.put("llistes", llistesJson);
        ArrayList<Tag> tags = new ArrayList<>(cd.getAllTags());
        java.util.List<java.util.Map<String, Object>> tagsJson = new java.util.ArrayList<>(tags.size());
        for (Tag t : tags) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("nom", t.getNom());
            tagsJson.add(m);
        }
        root.put("tags", tagsJson);

        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            GSON.toJson(root, pw);
        }
    }

    private static final String HTML_CSS =
        "body{font-family:sans-serif;background:#1a1a2e;color:#e0e0e0;margin:0;padding:20px}" +
        "h1{color:#a78bfa;text-align:center;margin-bottom:30px}" +
        "h2{color:#c4b5fd;border-bottom:1px solid #4c1d95;padding-bottom:8px;margin:30px 0 16px}" +
        ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:20px}" +
        ".card{background:#16213e;border-radius:10px;padding:12px;text-align:center;transition:transform .2s}" +
        ".card:hover{transform:translateY(-4px)}" +
        ".card img{width:100%;height:200px;object-fit:cover;border-radius:6px;margin-bottom:8px}" +
        ".card .no-cover{width:100%;height:200px;background:#0f3460;border-radius:6px;display:flex;align-items:center;justify-content:center;margin-bottom:8px;font-size:40px}" +
        ".card .title{font-weight:bold;font-size:13px;margin:4px 0}" +
        ".card .autor{color:#a0a0c0;font-size:12px}" +
        ".card .stars{color:#f59e0b;font-size:12px;margin:4px 0}" +
        ".card .badge{display:inline-block;padding:2px 6px;border-radius:10px;font-size:10px;margin-top:4px}" +
        ".read{background:#065f46;color:#6ee7b7}.unread{background:#3b0764;color:#c4b5fd}" +
        "table{border-collapse:collapse;width:100%}th,td{border:1px solid #4c1d95;padding:8px 12px;text-align:left}" +
        "th{background:#0f3460;color:#a78bfa}tr:nth-child(even){background:#16213e}";

    public static void exportHTML(File f, List<Llibre> view, BibliotecaWriter cd,
            boolean groupByShelf, boolean tableView) throws Exception {
        exportHTML(f, view, cd, groupByShelf, tableView, null);
    }

    /**
     * Extended HTML export with an explicit cover-inclusion flag and
     * pre-loaded cover bytes. The simpler overload above omits covers
     * (renders a 📖 placeholder for every book) and is the safe default
     * for libraries with more than {@link #LARGE_LIBRARY_COVER_THRESHOLD}
     * books — base64-encoding a 50 KB cover per book produces ~50 MB
     * of HTML for 1 000 books, which most browsers handle poorly.
     *
     * <p>When {@code coverBlobs} is non-null it must contain one entry
     * per book in {@code view} (same order). A {@code null} entry means
     * "no cover available, render the placeholder". Callers that opt in
     * to covers must populate the array from a background thread (the
     * underlying {@code CoverService.getCachedBytes} and
     * {@code cd.getLlibreBlob} do disk + DB I/O).
     */
    public static void exportHTML(File f, List<Llibre> view, BibliotecaWriter cd,
            boolean groupByShelf, boolean tableView,
            java.util.Map<Long, byte[]> coverBlobs) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            writeHtmlHeader(pw);
            writeHtmlBody(pw, view, cd, groupByShelf, tableView, coverBlobs);
            pw.println("</body></html>");
        }
    }

    /** Above this many books, the caller should pre-load covers off the
     *  EDT and pass them in, or skip covers entirely (the default
     *  3-arg overload does this). */
    static final int LARGE_LIBRARY_COVER_THRESHOLD = 100;

    private static void writeHtmlHeader(PrintWriter pw) {
        pw.println("<!DOCTYPE html><html lang=\"ca\"><head><meta charset=\"UTF-8\">");
        pw.println("<title>" + Escapers.html(I18n.t("dlg_export_html_title")) + "</title><style>");
        pw.println(HTML_CSS);
        pw.println("</style></head><body>");
        pw.println("<h1>" + Escapers.html(I18n.t("export_html_heading")) + "</h1>");
    }

    private static void writeHtmlBody(PrintWriter pw, List<Llibre> view, BibliotecaWriter cd,
            boolean groupByShelf, boolean tableView, java.util.Map<Long, byte[]> coverBlobs) {
        if (groupByShelf) {
            writeHtmlGroupedByShelf(pw, view, cd, tableView, coverBlobs);
        } else {
            if (tableView) printHtmlTable(pw, view);
            else printHtmlGrid(pw, view, coverBlobs);
        }
    }

    private static void writeHtmlGroupedByShelf(PrintWriter pw, List<Llibre> view, BibliotecaWriter cd,
            boolean tableView, java.util.Map<Long, byte[]> coverBlobs) {
        List<Llista> llistes = cd.getAllLlistes();
        java.util.Map<Long, java.util.Set<Integer>> isbnToLlistes = new java.util.HashMap<>();
        for (persistencia.LlibreLlistaRow row : cd.getAllLlibreLlistaRows())
            isbnToLlistes.computeIfAbsent(row.isbn(), k -> new java.util.HashSet<>()).add(row.llistaId());
        java.util.Set<Long> printed = new java.util.HashSet<>();
        for (Llista llista : llistes) {
            List<Llibre> shelfBooks = view.stream()
                .filter(l -> { java.util.Set<Integer> s = isbnToLlistes.get(l.getISBN()); return s != null && s.contains(llista.getId()); })
                .collect(Collectors.toList());
            if (shelfBooks.isEmpty()) continue;
            writeHtmlShelfHeader(pw, llista.getNom(), shelfBooks.size());
            if (tableView) printHtmlTable(pw, shelfBooks);
            else printHtmlGrid(pw, shelfBooks, coverBlobs);
            shelfBooks.forEach(l -> printed.add(l.getISBN()));
        }
        List<Llibre> unshelfed = view.stream()
            .filter(l -> !printed.contains(l.getISBN())).collect(Collectors.toList());
        if (!unshelfed.isEmpty()) {
            writeHtmlShelfHeader(pw, I18n.t("lbl_no_shelf"), 0);
            if (tableView) printHtmlTable(pw, unshelfed);
            else printHtmlGrid(pw, unshelfed, coverBlobs);
        }
    }

    private static void writeHtmlShelfHeader(PrintWriter pw, String name, int count) {
        if (count > 0) pw.println("<h2>" + Escapers.html(name) + " (" + count + ")</h2>");
        else pw.println("<h2>" + Escapers.html(name) + "</h2>");
    }

    public static void exportPDF(List<Llibre> view) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Llibre l : view) {
            double val = l.getValoracio() != null ? l.getValoracio() : 0.0;
            String stars = val > 0 ? "★".repeat((int) Math.round(val)) : "-";
            rows.add(new String[]{
                l.getNom(), l.getAutor(), String.valueOf(l.getAny()),
                stars, Boolean.TRUE.equals(l.getLlegit()) ? "✓" : "○"
            });
        }
        java.awt.print.PrinterJob pj = java.awt.print.PrinterJob.getPrinterJob();
        pj.setJobName("Biblioteca");
        String[] headers = {I18n.t("pdf_col_title"), I18n.t("pdf_col_author"), I18n.t("pdf_col_year"), I18n.t("pdf_col_rating"), I18n.t("pdf_col_read")};
        int[] colWidths = {250, 160, 45, 55, 45};
        pj.setPrintable((graphics, pageFormat, pageIndex) -> {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics;
            int lineH = 16, margin = (int) pageFormat.getImageableX();
            int topY  = (int) pageFormat.getImageableY();
            int pageW = (int) pageFormat.getImageableWidth();
            int perPage = (int)((pageFormat.getImageableHeight() - 40) / lineH);
            int totalPages = (rows.size() + perPage - 1) / perPage;
            if (pageIndex >= totalPages) return java.awt.print.Printable.NO_SUCH_PAGE;
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
            g.drawString(I18n.t("dlg_pdf_title", rows.size()), margin, topY + 14);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 9));
            int x = margin, y = topY + 30;
            for (int c = 0; c < headers.length; c++) {
                g.drawString(headers[c], x, y);
                x += colWidths[c];
            }
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
            g.drawLine(margin, y + 2, margin + pageW, y + 2);
            y += lineH;
            int start = pageIndex * perPage;
            int end   = Math.min(start + perPage, rows.size());
            for (int r = start; r < end; r++) {
                x = margin;
                for (int c = 0; c < headers.length; c++) {
                    String val = rows.get(r)[c];
                    int maxW = colWidths[c] - 4;
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    while (fm.stringWidth(val) > maxW && val.length() > 3)
                        val = val.substring(0, val.length() - 4) + "…";
                    g.drawString(val, x, y);
                    x += colWidths[c];
                }
                y += lineH;
            }
            g.drawString(I18n.t("dlg_pdf_page_footer", pageIndex + 1, totalPages), margin, topY + (int)pageFormat.getImageableHeight() - 4);
            return java.awt.print.Printable.PAGE_EXISTS;
        });
        if (pj.printDialog()) {
            try { pj.print(); }
            catch (java.awt.print.PrinterException e) { new DialogoError(e).showErrorMessage(); }
        }
    }

    private static void printHtmlGrid(PrintWriter pw, List<Llibre> books,
            java.util.Map<Long, byte[]> coverBlobs) {
        pw.println("<div class=\"grid\">");
        for (Llibre l : books) {
            pw.println("<div class=\"card\">");
            byte[] blob = coverBlobs != null ? coverBlobs.get(l.getISBN()) : null;
            if (blob != null) {
                String mime = (blob.length > 4 && blob[0] == (byte)0x89) ? "image/png" : "image/jpeg";
                String b64 = java.util.Base64.getEncoder().encodeToString(blob);
                pw.println("<img src=\"data:" + mime + ";base64," + b64 + "\" alt=\"portada\">");
            } else {
                pw.println("<div class=\"no-cover\">📖</div>");
            }
            Integer any = l.getAny();
            pw.println("<div class=\"title\">" + Escapers.html(l.getNom()) + "</div>");
            pw.println("<div class=\"autor\">" + Escapers.html(l.getAutor())
                + (any != null ? " (" + any + ")" : "") + "</div>");
            if (l.getValoracio() != null && l.getValoracio() > 0) {
                int stars = (int) Math.round(l.getValoracio());
                pw.println("<div class=\"stars\">" + "★".repeat(stars) + "☆".repeat(Math.max(0, 5-stars)) + "</div>");
            }
            boolean llegit = Boolean.TRUE.equals(l.getLlegit());
            pw.println("<span class=\"badge " + (llegit ? "read" : "unread") + "\">" +
                (llegit ? I18n.t("filter_read") : I18n.t("lbl_pending")) + "</span>");
            pw.println("</div>");
        }
        pw.println("</div>");
    }

    private static void printHtmlTable(PrintWriter pw, List<Llibre> books) {
        pw.println("<table><thead><tr>");
        pw.println("<th>ISBN</th><th>" + Escapers.html(I18n.t("field_title")) + "</th><th>" + Escapers.html(I18n.t("field_author")) + "</th>");
        pw.println("<th>" + Escapers.html(I18n.t("field_year")) + "</th><th>" + Escapers.html(I18n.t("field_publisher")) + "</th>");
        pw.println("<th>" + Escapers.html(I18n.t("field_rating")) + "</th><th>" + Escapers.html(I18n.t("field_read")) + "</th>");
        pw.println("</tr></thead><tbody>");
        for (Llibre l : books) {
            pw.println("<tr>");
            pw.println("<td>" + l.getISBN() + "</td>");
            pw.println("<td>" + Escapers.html(l.getNom()) + "</td>");
            pw.println("<td>" + Escapers.html(l.getAutor()) + "</td>");
            Integer any = l.getAny();
            pw.println("<td>" + (any != null && any > 0 ? any : "") + "</td>");
            pw.println("<td>" + Escapers.html(l.getEditorial() != null ? l.getEditorial() : "") + "</td>");
            Double val = l.getValoracio();
            // Locale.ROOT: the cell text is rendered as HTML, browsers
            // expect '.' as the decimal separator regardless of the
            // page's locale, and a locale-formatted `4,5` would render
            // as a literal comma in a <td>.
            pw.println("<td>" + (val != null && val > 0 ? String.format(java.util.Locale.ROOT, "%.1f", val) : "") + "</td>");
            pw.println("<td>" + (Boolean.TRUE.equals(l.getLlegit()) ? "✓" : "") + "</td>");
            pw.println("</tr>");
        }
        pw.println("</tbody></table>");
    }

    private static java.util.Map<String, Object> jsonLlibreMap(Llibre l,
            List<persistencia.LlibreLlistaRow> llistaRows, List<persistencia.LlibreTagRow> tagRows) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("isbn", l.getISBN());
        m.put("nom", l.getNom());
        m.put("autor", l.getAutor());
        m.put("any", l.getAny());
        m.put("descripcio", l.getDescripcio());
        m.put("valoracio", l.getValoracio());
        m.put("preu", l.getPreu());
        m.put("llegit", l.getLlegit());
        m.put("desitjat", l.isDesitjat());
        m.put("imatge", l.getImatge());
        m.put("notes", l.getNotes());
        m.put("pagines", l.getPagines());
        m.put("paginesLlegides", l.getPaginesLlegides());
        m.put("editorial", l.getEditorial());
        m.put("serie", l.getSerie());
        m.put("volum", l.getVolum());
        m.put("dataCompra", l.getDataCompra());
        m.put("dataLectura", l.getDataLectura());
        m.put("idioma", l.getIdioma());
        m.put("format", l.getFormat());
        m.put("paisOrigen", l.getPaisOrigen());
        java.util.List<java.util.Map<String, Object>> llistes = new java.util.ArrayList<>();
        for (persistencia.LlibreLlistaRow row : llistaRows) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", row.llistaId());
            entry.put("valoracio", row.valoracio());
            entry.put("llegit", row.llegit());
            llistes.add(entry);
        }
        m.put("llistes", llistes);
        java.util.List<Integer> tagIds = new java.util.ArrayList<>();
        for (persistencia.LlibreTagRow row : tagRows) tagIds.add(row.tagId());
        m.put("tags", tagIds);
        return m;
    }

    /** Wraps {@link Escapers#json(String)} in double quotes and renders
     *  {@code null} as the JSON literal {@code null}. */
    private static String jsonStr(String s) {
        if (s == null) return "null";
        return '"' + Escapers.json(s) + '"';
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
