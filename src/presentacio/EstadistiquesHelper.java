package presentacio;

import domini.Llibre;
import herramienta.I18n;
import interficie.BibliotecaWriter;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class EstadistiquesHelper {

    public static javax.swing.JPanel buildReadingChart(ArrayList<Llibre> books) {
        java.util.Map<Integer, Long> perYear = books.stream()
            .filter(l -> Boolean.TRUE.equals(l.getLlegit()))
            .filter(l -> {
                if (l.getDataLectura() != null && !l.getDataLectura().isEmpty()) return true;
                return l.getAny() != null && l.getAny() > 1900;
            })
            .collect(Collectors.groupingBy(l -> {
                int yr = l.getDataLectura() != null ? herramienta.DateUtils.parseYear(l.getDataLectura()) : 0;
                return yr > 0 ? yr : (l.getAny() != null ? l.getAny() : 0);
            }, Collectors.counting()));
        java.awt.Font chartFont9 = herramienta.UITheme.FONT_BASE.deriveFont(9f);
        return new javax.swing.JPanel() {
            { setPreferredSize(new java.awt.Dimension(560, 180)); setBackground(herramienta.UITheme.BG_PANEL); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_books_year"))); }
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (perYear.isEmpty()) { g.drawString(I18n.t("stats_no_data"), 20, 60); return; }
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                java.util.List<Integer> years = perYear.keySet().stream().filter(y -> y > 1900).sorted().collect(Collectors.toList());
                if (years.isEmpty()) { g2.drawString(I18n.t("stats_no_data"), 20, 60); return; }
                long maxVal = perYear.values().stream().mapToLong(v -> v).max().orElse(1);
                int pad = 40, barW = Math.max(18, (getWidth() - pad * 2) / years.size() - 4);
                int chartH = getHeight() - pad * 2;
                g2.setColor(herramienta.UITheme.ACCENT);
                for (int i = 0; i < years.size(); i++) {
                    int yr = years.get(i);
                    long cnt = perYear.getOrDefault(yr, 0L);
                    int bH = (int) (chartH * cnt / maxVal);
                    int x = pad + i * (barW + 4);
                    int y = pad + chartH - bH;
                    g2.fillRect(x, y, barW, bH);
                    g2.setColor(herramienta.UITheme.TEXT_DARK);
                    g2.setFont(chartFont9);
                    g2.drawString(String.valueOf(yr), x, getHeight() - 5);
                    g2.drawString(String.valueOf(cnt), x + 2, y - 2);
                    g2.setColor(herramienta.UITheme.ACCENT);
                }
            }
        };
    }

    public static javax.swing.JPanel buildPublisherChart(ArrayList<Llibre> books) {
        java.util.Map<String, Long> byPublisher = books.stream()
            .filter(l -> l.getEditorial() != null && !l.getEditorial().isEmpty())
            .collect(Collectors.groupingBy(l -> l.getEditorial(), Collectors.counting()));
        java.util.List<java.util.Map.Entry<String, Long>> top = byPublisher.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10).collect(Collectors.toList());
        java.awt.Font chartFont10 = herramienta.UITheme.FONT_BASE.deriveFont(10f);
        return new javax.swing.JPanel() {
            { setPreferredSize(new java.awt.Dimension(560, 200)); setBackground(herramienta.UITheme.BG_PANEL); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_publishers"))); }
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (top.isEmpty()) { g.drawString(I18n.t("stats_no_data"), 20, 60); return; }
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                long maxVal = top.get(0).getValue();
                int pad = 8, lblW = 130, chartH = 22, gap = 4;
                g2.setFont(chartFont10);
                for (int i = 0; i < top.size(); i++) {
                    int y = pad + i * (chartH + gap);
                    String lbl = top.get(i).getKey();
                    long cnt = top.get(i).getValue();
                    int barW = (int) ((getWidth() - lblW - pad * 3 - 40) * cnt / maxVal);
                    g2.setColor(herramienta.UITheme.TEXT_MID);
                    String lblTrunc = lbl.length() > 18 ? lbl.substring(0, 17) + "…" : lbl;
                    g2.drawString(lblTrunc, pad, y + chartH - 6);
                    g2.setColor(herramienta.UITheme.ACCENT);
                    g2.fillRect(lblW + pad, y + 2, barW, chartH - 4);
                    g2.setColor(herramienta.UITheme.TEXT_DARK);
                    g2.drawString(String.valueOf(cnt), lblW + pad + barW + 4, y + chartH - 6);
                }
            }
        };
    }

    public static javax.swing.JPanel buildTagCloud(ArrayList<Llibre> books, BibliotecaWriter cd) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 6));
        panel.setBackground(herramienta.UITheme.BG_PANEL);
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_tab_tags")));

        java.util.Set<Long> bookIsbns = books.stream().map(Llibre::getISBN).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Integer, String> tagIdToName = new java.util.HashMap<>();
        for (domini.Tag t : cd.getAllTags()) tagIdToName.put(t.getId(), t.getNom());
        java.util.Map<String, Long> tagCount = new java.util.HashMap<>();
        for (domini.LlibreTagRow row : cd.getAllLlibreTagRows()) {
            if (!bookIsbns.contains(row.isbn())) continue;
            String nom = tagIdToName.get(row.tagId());
            if (nom != null) tagCount.merge(nom, 1L, Long::sum);
        }
        if (tagCount.isEmpty()) {
            javax.swing.JLabel lbl = new javax.swing.JLabel(I18n.t("stats_no_tags"));
            herramienta.UITheme.styleLabel(lbl);
            panel.add(lbl);
            return panel;
        }
        long maxCount = tagCount.values().stream().mapToLong(v -> v).max().orElse(1);
        tagCount.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                float size = 11f + 14f * e.getValue() / maxCount;
                javax.swing.JLabel lbl = new javax.swing.JLabel(e.getKey() + " (" + e.getValue() + ")");
                lbl.setFont(herramienta.UITheme.FONT_BASE.deriveFont(size));
                lbl.setForeground(herramienta.UITheme.ACCENT);
                lbl.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                lbl.setToolTipText(e.getValue() + " " + I18n.t("stats_books_with_tag"));
                panel.add(lbl);
            });
        return panel;
    }

    public static javax.swing.JPanel buildReadingPacePanel(ArrayList<Llibre> books) {
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBackground(herramienta.UITheme.BG_PANEL);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

        int currentYear = java.time.LocalDate.now().getYear();
        int dayOfYear = java.time.LocalDate.now().getDayOfYear();

        long finishedThisYear = books.stream()
            .filter(l -> Boolean.TRUE.equals(l.getLlegit()))
            .filter(l -> l.getDataLectura() != null && herramienta.DateUtils.parseYear(l.getDataLectura()) == currentYear)
            .count();
        double booksPerDay = dayOfYear > 0 ? (double) finishedThisYear / dayOfYear : 0;
        double booksPerMonth = booksPerDay * 30.44;
        double projectedYear = booksPerDay * 365;

        long totalPages = books.stream().mapToLong(Llibre::getPaginesLlegides).sum();
        int goal = herramienta.Config.getReadingGoal();

        java.util.function.Consumer<String> addLine = text -> {
            javax.swing.JLabel lbl = new javax.swing.JLabel(text);
            herramienta.UITheme.styleLabel(lbl);
            lbl.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            panel.add(lbl);
            panel.add(javax.swing.Box.createVerticalStrut(6));
        };
        addLine.accept(I18n.t("stats_pace_year", currentYear, finishedThisYear));
        addLine.accept(I18n.t("stats_pace_per_month", String.format("%.1f", booksPerMonth)));
        addLine.accept(I18n.t("stats_pace_projected", String.format("%.0f", projectedYear)));
        if (goal > 0) {
            double remaining = goal - finishedThisYear;
            int daysLeft = 365 - dayOfYear;
            addLine.accept(I18n.t("stats_pace_goal_remaining", goal, (int) remaining, daysLeft));
        }
        addLine.accept(I18n.t("stats_pace_total_pages", totalPages));
        return panel;
    }

    public static String buildStatsSummary(ArrayList<Llibre> llibres, String scope) {
        int total = llibres.size();
        long llegits = llibres.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
        double avgVal = llibres.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
        double avgPreu = llibres.stream().mapToDouble(l -> l.getPreu() != null ? l.getPreu() : 0).average().orElse(0);
        String topAnys = llibres.stream()
            .filter(l -> l.getAny() != null && l.getAny() > 0)
            .collect(Collectors.groupingBy(Llibre::getAny, Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> "  " + e.getKey() + ": " + e.getValue() + " " + (e.getValue() > 1 ? I18n.t("stats_book_plural") : I18n.t("stats_book_singular")))
            .collect(Collectors.joining("\n"));
        return scope + "\n" +
            I18n.t("stats_total") + " " + total + "  ·  " +
            I18n.t("stats_llegits_colon") + " " + llegits + " (" + String.format("%.1f", 100.0 * llegits / total) + "%)  ·  " +
            I18n.t("stats_no_llegits_colon") + " " + (total - llegits) + "\n" +
            I18n.t("stats_avg_rating_colon") + " " + String.format("%.2f", avgVal) + " / 10  ·  " +
            I18n.t("stats_avg_price_colon") + " " + String.format("%.2f", avgPreu) + " " + herramienta.Config.getCurrencySymbol() + "\n" +
            I18n.t("stats_top_years") + "\n" + (topAnys.isEmpty() ? "  " + I18n.t("stats_no_years") : topAnys);
    }
}
