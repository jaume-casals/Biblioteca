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

    public static void exportCSV(File f, List<Llibre> view, BibliotecaWriter cd) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada,Llistes");
            for (Llibre l : view) {
                try {
                    ArrayList<Llista> llistes = cd.getLlistesForLlibre(l.getISBN());
                    StringBuilder llistesStr = new StringBuilder();
                    for (Llista ll : llistes) {
                        if (llistesStr.length() > 0) llistesStr.append(';');
                        llistesStr.append(esc(ll.getNom())).append('|')
                            .append(ll.getValoracioLlibre() != null ? ll.getValoracioLlibre() : 0.0).append('|')
                            .append(Boolean.TRUE.equals(ll.getLlegitLlibre()));
                    }
                    pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\",%.1f,%.2f,%b,\"%s\",\"%s\"%n",
                        l.getISBN(), esc(l.getNom()), esc(l.getAutor()), l.getAny(),
                        esc(l.getDescripcio()), l.getValoracio(), l.getPreu(), l.getLlegit(),
                        esc(l.getImatge()), llistesStr);
                } catch (Exception ignored) {}
            }
        }
    }

    public static void exportJSON(File f, BibliotecaWriter cd) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("{");
            pw.println("\"version\":1,");
            // books
            ArrayList<Llibre> tots = cd.getAllLlibres();
            pw.print("\"llibres\":[");
            for (int i = 0; i < tots.size(); i++) {
                Llibre l = tots.get(i);
                pw.print(jsonLlibre(l, cd));
                if (i < tots.size() - 1) pw.print(",");
            }
            pw.println("],");
            // shelves
            ArrayList<Llista> llistes = cd.getAllLlistes();
            pw.print("\"llistes\":[");
            for (int i = 0; i < llistes.size(); i++) {
                Llista ll = llistes.get(i);
                pw.print("{\"id\":" + ll.getId() + ",\"nom\":" + jsonStr(ll.getNom()) + "}");
                if (i < llistes.size() - 1) pw.print(",");
            }
            pw.println("],");
            // tags
            ArrayList<Tag> tags = cd.getAllTags();
            pw.print("\"tags\":[");
            for (int i = 0; i < tags.size(); i++) {
                Tag t = tags.get(i);
                pw.print("{\"id\":" + t.getId() + ",\"nom\":" + jsonStr(t.getNom()) + "}");
                if (i < tags.size() - 1) pw.print(",");
            }
            pw.println("]");
            pw.println("}");
        }
    }

    public static void exportHTML(File f, List<Llibre> view, BibliotecaWriter cd, boolean groupByShelf, boolean tableView) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html lang=\"ca\"><head><meta charset=\"UTF-8\">");
            pw.println("<title>La meva biblioteca</title><style>");
            pw.println("body{font-family:sans-serif;background:#1a1a2e;color:#e0e0e0;margin:0;padding:20px}");
            pw.println("h1{color:#a78bfa;text-align:center;margin-bottom:30px}");
            pw.println("h2{color:#c4b5fd;border-bottom:1px solid #4c1d95;padding-bottom:8px;margin:30px 0 16px}");
            pw.println(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:20px}");
            pw.println(".card{background:#16213e;border-radius:10px;padding:12px;text-align:center;transition:transform .2s}");
            pw.println(".card:hover{transform:translateY(-4px)}");
            pw.println(".card img{width:100%;height:200px;object-fit:cover;border-radius:6px;margin-bottom:8px}");
            pw.println(".card .no-cover{width:100%;height:200px;background:#0f3460;border-radius:6px;display:flex;align-items:center;justify-content:center;margin-bottom:8px;font-size:40px}");
            pw.println(".card .title{font-weight:bold;font-size:13px;margin:4px 0}");
            pw.println(".card .autor{color:#a0a0c0;font-size:12px}");
            pw.println(".card .stars{color:#f59e0b;font-size:12px;margin:4px 0}");
            pw.println(".card .badge{display:inline-block;padding:2px 6px;border-radius:10px;font-size:10px;margin-top:4px}");
            pw.println(".read{background:#065f46;color:#6ee7b7}.unread{background:#3b0764;color:#c4b5fd}");
            pw.println("table{border-collapse:collapse;width:100%}th,td{border:1px solid #4c1d95;padding:8px 12px;text-align:left}");
            pw.println("th{background:#0f3460;color:#a78bfa}tr:nth-child(even){background:#16213e}");
            pw.println("</style></head><body>");
            pw.println("<h1>📚 La meva biblioteca</h1>");

            if (groupByShelf) {
                List<Llista> llistes = cd.getAllLlistes();
                java.util.Set<Long> printed = new java.util.HashSet<>();
                for (Llista llista : llistes) {
                    List<Llibre> shelfBooks = view.stream()
                        .filter(l -> { try { return cd.getLlistesForLlibre(l.getISBN()).stream().anyMatch(ll -> ll.getId() == llista.getId()); } catch (Exception e2) { return false; } })
                        .collect(Collectors.toList());
                    if (shelfBooks.isEmpty()) continue;
                    pw.println("<h2>" + htmlEsc(llista.getNom()) + " (" + shelfBooks.size() + ")</h2>");
                    if (tableView) printHtmlTable(pw, shelfBooks);
                    else printHtmlGrid(pw, shelfBooks, cd);
                    shelfBooks.forEach(l -> printed.add(l.getISBN()));
                }
                List<Llibre> unshelfed = view.stream()
                    .filter(l -> !printed.contains(l.getISBN())).collect(Collectors.toList());
                if (!unshelfed.isEmpty()) {
                    pw.println("<h2>" + I18n.t("lbl_no_shelf") + "</h2>");
                    if (tableView) printHtmlTable(pw, unshelfed);
                    else printHtmlGrid(pw, unshelfed, cd);
                }
            } else {
                if (tableView) printHtmlTable(pw, view);
                else printHtmlGrid(pw, view, cd);
            }

            pw.println("</body></html>");
        }
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

    private static void printHtmlGrid(PrintWriter pw, List<Llibre> books, BibliotecaWriter cd) {
        pw.println("<div class=\"grid\">");
        for (Llibre l : books) {
            pw.println("<div class=\"card\">");
            byte[] blob = null;
            try { blob = cd.getLlibreBlob(l.getISBN()); } catch (Exception ignored) {}
            if (blob == null && l.getImatge() != null && !l.getImatge().isEmpty()) {
                try { blob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(l.getImatge())); } catch (Exception ignored) {}
            }
            if (blob != null) {
                String mime = (blob.length > 4 && blob[0] == (byte)0x89) ? "image/png" : "image/jpeg";
                String b64 = java.util.Base64.getEncoder().encodeToString(blob);
                pw.println("<img src=\"data:" + mime + ";base64," + b64 + "\" alt=\"portada\">");
            } else {
                pw.println("<div class=\"no-cover\">📖</div>");
            }
            pw.println("<div class=\"title\">" + htmlEsc(l.getNom()) + "</div>");
            pw.println("<div class=\"autor\">" + htmlEsc(l.getAutor()) + " (" + l.getAny() + ")</div>");
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
        pw.println("<th>ISBN</th><th>" + htmlEsc(I18n.t("field_title")) + "</th><th>" + htmlEsc(I18n.t("field_author")) + "</th>");
        pw.println("<th>" + htmlEsc(I18n.t("field_year")) + "</th><th>" + htmlEsc(I18n.t("field_publisher")) + "</th>");
        pw.println("<th>" + htmlEsc(I18n.t("field_rating")) + "</th><th>" + htmlEsc(I18n.t("field_read")) + "</th>");
        pw.println("</tr></thead><tbody>");
        for (Llibre l : books) {
            pw.println("<tr>");
            pw.println("<td>" + l.getISBN() + "</td>");
            pw.println("<td>" + htmlEsc(l.getNom()) + "</td>");
            pw.println("<td>" + htmlEsc(l.getAutor()) + "</td>");
            pw.println("<td>" + (l.getAny() > 0 ? l.getAny() : "") + "</td>");
            pw.println("<td>" + htmlEsc(l.getEditorial() != null ? l.getEditorial() : "") + "</td>");
            pw.println("<td>" + (l.getValoracio() != null && l.getValoracio() > 0 ? l.getValoracio() : "") + "</td>");
            pw.println("<td>" + (Boolean.TRUE.equals(l.getLlegit()) ? "✓" : "") + "</td>");
            pw.println("</tr>");
        }
        pw.println("</tbody></table>");
    }

    private static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String jsonLlibre(Llibre l, BibliotecaWriter cd) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"isbn\":").append(l.getISBN()).append(",");
        sb.append("\"nom\":").append(jsonStr(l.getNom())).append(",");
        sb.append("\"autor\":").append(jsonStr(l.getAutor())).append(",");
        sb.append("\"any\":").append(l.getAny()).append(",");
        sb.append("\"descripcio\":").append(jsonStr(l.getDescripcio())).append(",");
        sb.append("\"valoracio\":").append(l.getValoracio()).append(",");
        sb.append("\"preu\":").append(l.getPreu()).append(",");
        sb.append("\"llegit\":").append(l.getLlegit()).append(",");
        sb.append("\"desitjat\":").append(l.getDesitjat()).append(",");
        sb.append("\"imatge\":").append(jsonStr(l.getImatge())).append(",");
        sb.append("\"notes\":").append(jsonStr(l.getNotes())).append(",");
        sb.append("\"pagines\":").append(l.getPagines()).append(",");
        sb.append("\"paginesLlegides\":").append(l.getPaginesLlegides()).append(",");
        sb.append("\"editorial\":").append(jsonStr(l.getEditorial())).append(",");
        sb.append("\"serie\":").append(jsonStr(l.getSerie())).append(",");
        sb.append("\"volum\":").append(l.getVolum()).append(",");
        sb.append("\"dataCompra\":").append(jsonStr(l.getDataCompra())).append(",");
        sb.append("\"dataLectura\":").append(jsonStr(l.getDataLectura())).append(",");
        sb.append("\"idioma\":").append(jsonStr(l.getIdioma())).append(",");
        sb.append("\"format\":").append(jsonStr(l.getFormat())).append(",");
        sb.append("\"paisOrigen\":").append(jsonStr(l.getPaisOrigen())).append(",");
        // shelf memberships
        ArrayList<Llista> llistes = cd.getLlistesForLlibre(l.getISBN());
        sb.append("\"llistes\":[");
        for (int i = 0; i < llistes.size(); i++) {
            Llista ll = llistes.get(i);
            sb.append("{\"id\":").append(ll.getId())
                .append(",\"valoracio\":").append(ll.getValoracioLlibre() != null ? ll.getValoracioLlibre() : 0.0)
                .append(",\"llegit\":").append(Boolean.TRUE.equals(ll.getLlegitLlibre())).append("}");
            if (i < llistes.size() - 1) sb.append(",");
        }
        sb.append("],");
        // tags
        ArrayList<Tag> tags = cd.getTagsForLlibre(l.getISBN());
        sb.append("\"tags\":[");
        for (int i = 0; i < tags.size(); i++) {
            sb.append(tags.get(i).getId());
            if (i < tags.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
