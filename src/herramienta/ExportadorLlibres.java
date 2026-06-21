package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.i18n.Escapers;
import herramienta.i18n.I18n;
import herramienta.ui.DialegError;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import persistencia.contract.LectorBiblioteca;
import persistencia.contract.LectorPrestatgeria;

import persistencia.row.LlibreLlistaRow;
import persistencia.row.LlibreTagRow;
public class ExportadorLlibres {

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    public static void exportarCSV(File f, List<Llibre> view, LectorPrestatgeria cd) throws Exception {
        java.util.Map<Integer, Llista> llistaById = new java.util.HashMap<>();
        for (Llista ll : cd.obtenirAllLlistes()) llistaById.put(ll.obtenirId(), ll);
        java.util.Map<Long, java.util.List<persistencia.row.LlibreLlistaRow>> llistaRows = new java.util.HashMap<>();
        for (persistencia.row.LlibreLlistaRow row : cd.obtenirAllLlibreLlistaRows())
            llistaRows.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(row);
        domini.AnalitzadorPrestatgeria.exportarToCsv(f, view, llistaById, llistaRows);
    }

    public static void exportarJSON(File f, LectorBiblioteca cd) throws Exception {
        // Càrrega en lot de les dades relacionals per evitar N+1
        java.util.Map<Long, List<persistencia.row.LlibreLlistaRow>> llistaRows = new java.util.HashMap<>();
        for (persistencia.row.LlibreLlistaRow r : cd.obtenirAllLlibreLlistaRows())
            llistaRows.computeIfAbsent(r.isbn(), k -> new ArrayList<>()).add(r);
        java.util.Map<Long, List<persistencia.row.LlibreTagRow>> tagRows = new java.util.HashMap<>();
        for (persistencia.row.LlibreTagRow r : cd.obtenirAllLlibreTagRows())
            tagRows.computeIfAbsent(r.isbn(), k -> new ArrayList<>()).add(r);

        // Construeix el document com un sol LinkedHashMap perquè GSON
        // gestioni les claus, comes i escapament (la implementació
        // anterior feia pw.println("{") a mà, etc. i només delegava la
        // serialització per llibre a GSON — una exportació de 5000
        // llibres pagava un 500x d'alentiment i enviava una superfície
        // d'escapament mantinguda a mà). L'ordre d'inserció de
        // LinkedHashMap coincideix amb el layout previ al disc: version,
        // llibres, llistes, tags.
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("version", 1);
        ArrayList<Llibre> tots = new ArrayList<>(cd.obtenirAllLlibres());
        java.util.List<java.util.Map<String, Object>> llibresJson = new java.util.ArrayList<>(tots.size());
        for (Llibre l : tots) {
            llibresJson.add(jsonLlibreMap(l,
                llistaRows.getOrDefault(l.obtenirISBN(), java.util.Collections.emptyList()),
                tagRows.getOrDefault(l.obtenirISBN(), java.util.Collections.emptyList())));
        }
        root.put("llibres", llibresJson);
        ArrayList<Llista> llistes = new ArrayList<>(cd.obtenirAllLlistes());
        java.util.List<java.util.Map<String, Object>> llistesJson = new java.util.ArrayList<>(llistes.size());
        for (Llista ll : llistes) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", ll.obtenirId());
            m.put("nom", ll.obtenirNom());
            llistesJson.add(m);
        }
        root.put("llistes", llistesJson);
        ArrayList<Tag> tags = new ArrayList<>(cd.obtenirAllTags());
        java.util.List<java.util.Map<String, Object>> tagsJson = new java.util.ArrayList<>(tags.size());
        for (Tag t : tags) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", t.obtenirId());
            m.put("nom", t.obtenirNom());
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

    public static void exportarHTML(File f, List<Llibre> view, LectorPrestatgeria cd,
            boolean groupByShelf, boolean tableView) throws Exception {
        exportarHTML(f, view, cd, groupByShelf, tableView, null);
    }

    /**
     * Exportació HTML ampliada amb un indicador explícit d'inclusió de coberta
     * i bytes de coberta precarregats. La sobrecàrrega més senzilla de sobre
     * omet les cobertes (renderitza un marcador 📖 per a cada llibre) i és
     * el valor per defecte segur per a biblioteques amb més de
     * {@link #LARGE_LIBRARY_COVER_THRESHOLD} llibres — codificar en base64 una
     * coberta de 50 KB per llibre produeix ~50 MB d'HTML per a 1 000 llibres,
     * que la majoria de navegadors gestionen malament.
     *
     * <p>Quan {@code coverBlobs} no és null ha de contenir una entrada per
     * llibre a {@code view} (mateix ordre). Una entrada {@code null} significa
     * "no hi ha coberta disponible, renderitza el marcador". Els consumidors
     * que opten per incloure cobertes han d'omplir el mapa des d'un fil en
     * segon pla (les operacions subjacents {@code ServeiCoberta.obtenirCachedBytes}
     * i {@code cd.obtenirLlibreBlob} fan I/O de disc + BBDD).
     */
    public static void exportarHTML(File f, List<Llibre> view, LectorPrestatgeria cd,
            boolean groupByShelf, boolean tableView,
            java.util.Map<Long, byte[]> coverBlobs) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            escriureHtmlHeader(pw);
            escriureHtmlBody(pw, view, cd, groupByShelf, tableView, coverBlobs);
            pw.println("</body></html>");
        }
    }

    /** Per sobre d'aquest nombre de llibres, el consumidor hauria de precarregar
     *  les cobertes fora de l'EDT i passar-les, o ometre les cobertes del tot
     *  (la sobrecàrrega per defecte de 3 args ho fa). */
    static final int LARGE_LIBRARY_COVER_THRESHOLD = 100;

    private static void escriureHtmlHeader(PrintWriter pw) {
        pw.println("<!DOCTYPE html><html lang=\"ca\"><head><meta charset=\"UTF-8\">");
        pw.println("<title>" + Escapers.html(I18n.t("dlg_export_html_title")) + "</title><style>");
        pw.println(HTML_CSS);
        pw.println("</style></head><body>");
        pw.println("<h1>" + Escapers.html(I18n.t("export_html_heading")) + "</h1>");
    }

    private static void escriureHtmlBody(PrintWriter pw, List<Llibre> view, LectorPrestatgeria cd,
            boolean groupByShelf, boolean tableView, java.util.Map<Long, byte[]> coverBlobs) {
        if (groupByShelf) {
            escriureHtmlGroupedByShelf(pw, view, cd, tableView, coverBlobs);
        } else {
            if (tableView) printHtmlTable(pw, view);
            else printHtmlGrid(pw, view, coverBlobs);
        }
    }

    private static void escriureHtmlGroupedByShelf(PrintWriter pw, List<Llibre> view, LectorPrestatgeria cd,
            boolean tableView, java.util.Map<Long, byte[]> coverBlobs) {
        List<Llista> llistes = cd.obtenirAllLlistes();
        java.util.Map<Long, java.util.Set<Integer>> isbnToLlistes = new java.util.HashMap<>();
        for (persistencia.row.LlibreLlistaRow row : cd.obtenirAllLlibreLlistaRows())
            isbnToLlistes.computeIfAbsent(row.isbn(), k -> new java.util.HashSet<>()).add(row.llistaId());
        java.util.Set<Long> printed = new java.util.HashSet<>();
        for (Llista llista : llistes) {
            List<Llibre> shelfBooks = view.stream()
                .filter(l -> { java.util.Set<Integer> s = isbnToLlistes.get(l.obtenirISBN()); return s != null && s.contains(llista.obtenirId()); })
                .collect(Collectors.toList());
            if (shelfBooks.isEmpty()) continue;
            escriureHtmlShelfHeader(pw, llista.obtenirNom(), shelfBooks.size());
            if (tableView) printHtmlTable(pw, shelfBooks);
            else printHtmlGrid(pw, shelfBooks, coverBlobs);
            shelfBooks.forEach(l -> printed.add(l.obtenirISBN()));
        }
        List<Llibre> unshelfed = view.stream()
            .filter(l -> !printed.contains(l.obtenirISBN())).collect(Collectors.toList());
        if (!unshelfed.isEmpty()) {
            escriureHtmlShelfHeader(pw, I18n.t("lbl_no_shelf"), 0);
            if (tableView) printHtmlTable(pw, unshelfed);
            else printHtmlGrid(pw, unshelfed, coverBlobs);
        }
    }

    private static void escriureHtmlShelfHeader(PrintWriter pw, String name, int count) {
        if (count > 0) pw.println("<h2>" + Escapers.html(name) + " (" + count + ")</h2>");
        else pw.println("<h2>" + Escapers.html(name) + "</h2>");
    }

    public static void exportarPDF(List<Llibre> view) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Llibre l : view) {
            double val = l.obtenirValoracio() != null ? l.obtenirValoracio() : 0.0;
            String stars = val > 0 ? "★".repeat((int) Math.round(val)) : "-";
            rows.add(new String[]{
                l.obtenirNom(), l.obtenirAutor(), String.valueOf(l.obtenirAny()),
                stars, Boolean.TRUE.equals(l.obtenirLlegit()) ? "✓" : "○"
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
            java.awt.FontMetrics fm = g.getFontMetrics();
            java.text.BreakIterator grapheme = java.text.BreakIterator.getCharacterInstance();
            for (int r = start; r < end; r++) {
                x = margin;
                for (int c = 0; c < headers.length; c++) {
                    String val = rows.get(r)[c];
                    int maxW = colWidths[c] - 4;
                    grapheme.setText(val);
                    while (val.length() > 1 && fm.stringWidth(val) > maxW) {
                        int prev = grapheme.preceding(val.length());
                        if (prev <= 0) break;
                        val = val.substring(0, prev) + "…";
                    }
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
            catch (java.awt.print.PrinterException e) { new DialegError(e).mostrarErrorMessage(); }
        }
    }

    private static void printHtmlGrid(PrintWriter pw, List<Llibre> books,
            java.util.Map<Long, byte[]> coverBlobs) {
        pw.println("<div class=\"grid\">");
        for (Llibre l : books) {
            pw.println("<div class=\"card\">");
            byte[] blob = coverBlobs != null ? coverBlobs.get(l.obtenirISBN()) : null;
            if (blob != null) {
                String mime = (blob.length > 4 && blob[0] == (byte)0x89) ? "image/png" : "image/jpeg";
                String b64 = java.util.Base64.getEncoder().encodeToString(blob);
                pw.println("<img src=\"data:" + mime + ";base64," + b64 + "\" alt=\"portada\">");
            } else {
                pw.println("<div class=\"no-cover\">📖</div>");
            }
            Integer any = l.obtenirAny();
            pw.println("<div class=\"title\">" + Escapers.html(l.obtenirNom()) + "</div>");
            pw.println("<div class=\"autor\">" + Escapers.html(l.obtenirAutor())
                + (any != null ? " (" + any + ")" : "") + "</div>");
            if (l.obtenirValoracio() != null && l.obtenirValoracio() > 0) {
                int stars = (int) Math.round(l.obtenirValoracio());
                pw.println("<div class=\"stars\">" + "★".repeat(stars) + "☆".repeat(Math.max(0, 5-stars)) + "</div>");
            }
            boolean llegit = Boolean.TRUE.equals(l.obtenirLlegit());
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
            pw.println("<td>" + l.obtenirISBN() + "</td>");
            pw.println("<td>" + Escapers.html(l.obtenirNom()) + "</td>");
            pw.println("<td>" + Escapers.html(l.obtenirAutor()) + "</td>");
            Integer any = l.obtenirAny();
            pw.println("<td>" + (any != null && any > 0 ? any : "") + "</td>");
            pw.println("<td>" + Escapers.html(l.obtenirEditorial() != null ? l.obtenirEditorial() : "") + "</td>");
            Double val = l.obtenirValoracio();
            // Locale.ROOT: el text de la cel·la es renderitza com a
            // HTML, els navegants esperen '.' com a separador decimal
            // independentment de la configuració regional de la pàgina,
            // i un `4,5` formatat segons la regionalització es
            // renderitzaria com una coma literal dins d'un <td>.
            pw.println("<td>" + (val != null && val > 0 ? String.format(java.util.Locale.ROOT, "%.1f", val) : "") + "</td>");
            pw.println("<td>" + (Boolean.TRUE.equals(l.obtenirLlegit()) ? "✓" : "") + "</td>");
            pw.println("</tr>");
        }
        pw.println("</tbody></table>");
    }

    private static java.util.Map<String, Object> jsonLlibreMap(Llibre l,
            List<persistencia.row.LlibreLlistaRow> llistaRows, List<persistencia.row.LlibreTagRow> tagRows) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("isbn", l.obtenirISBN());
        m.put("nom", l.obtenirNom());
        m.put("autor", l.obtenirAutor());
        m.put("any", l.obtenirAny());
        m.put("descripcio", l.obtenirDescripcio());
        m.put("valoracio", l.obtenirValoracio());
        m.put("preu", l.obtenirPreu());
        m.put("llegit", l.obtenirLlegit());
        m.put("desitjat", l.esDesitjat());
        m.put("imatge", l.obtenirImatge());
        m.put("notes", l.obtenirNotes());
        m.put("pagines", l.obtenirPagines());
        m.put("paginesLlegides", l.obtenirPaginesLlegides());
        m.put("editorial", l.obtenirEditorial());
        m.put("serie", l.obtenirSerie());
        m.put("volum", l.obtenirVolum());
        m.put("dataCompra", l.obtenirDataCompra());
        m.put("dataLectura", l.obtenirDataLectura());
        m.put("idioma", l.obtenirIdioma());
        m.put("format", l.obtenirFormat());
        m.put("paisOrigen", l.obtenirPaisOrigen());
        java.util.List<java.util.Map<String, Object>> llistes = new java.util.ArrayList<>();
        for (persistencia.row.LlibreLlistaRow row : llistaRows) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", row.llistaId());
            entry.put("valoracio", row.valoracio());
            entry.put("llegit", row.llegit());
            llistes.add(entry);
        }
        m.put("llistes", llistes);
        java.util.List<Integer> tagIds = new java.util.ArrayList<>();
        for (persistencia.row.LlibreTagRow row : tagRows) tagIds.add(row.tagId());
        m.put("tags", tagIds);
        return m;
    }
}
