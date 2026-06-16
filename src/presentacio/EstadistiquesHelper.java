package presentacio;



import presentacio.UIComponents;
import domini.Llibre;
import herramienta.I18n;
import interficie.BibliotecaWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EstadistiquesHelper {

    public static class BookStats {
        public final int total;
        public final long llegits;
        public final double avgValoracio;
        public final double avgPreu;
        public final java.util.Map<Integer, Long> booksByReadYear;

        public BookStats(int total, long llegits, double avgValoracio, double avgPreu, java.util.Map<Integer, Long> booksByReadYear) {
            this.total = total;
            this.llegits = llegits;
            this.avgValoracio = avgValoracio;
            this.avgPreu = avgPreu;
            this.booksByReadYear = booksByReadYear;
        }
    }

    private static int readYearFor(Llibre l) {
        java.util.Optional<Integer> yr = l.getDataLectura() != null
            ? herramienta.DateUtils.parseYear(l.getDataLectura())
            : java.util.Optional.empty();
        return yr.filter(y -> y > 0).orElse(l.getAny() != null && l.getAny() > 1900 ? l.getAny() : 0);
    }

    public static java.util.Map<Integer, Long> booksByReadYear(List<Llibre> books) {
        java.util.Map<Integer, Long> byYear = new java.util.HashMap<>();
        for (Llibre l : books) {
            if (!Boolean.TRUE.equals(l.getLlegit())) continue;
            if ((l.getDataLectura() == null || l.getDataLectura().isEmpty()) && (l.getAny() == null || l.getAny() <= 1900)) continue;
            int yr = readYearFor(l);
            if (yr > 0) byYear.merge(yr, 1L, Long::sum);
        }
        return byYear;
    }

    public static BookStats computeStats(List<Llibre> books) {
        int total = books.size();
        long llegits = books.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
        double avgVal = books.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
        double avgPreu = books.stream().mapToDouble(l -> l.getPreu() != null ? l.getPreu() : 0).average().orElse(0);
        return new BookStats(total, llegits, avgVal, avgPreu, booksByReadYear(books));
    }

    static javax.swing.JPanel buildReadingChart(java.util.Map<Integer, Long> perYear) {
        java.awt.Font chartFont9 = herramienta.UITheme.fontBase().deriveFont(9f);
        return new javax.swing.JPanel() {
            { setPreferredSize(new java.awt.Dimension(560, 180)); setBackground(herramienta.UITheme.palette().bgPanel()); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_books_year"))); }
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
                g2.setColor(herramienta.UITheme.palette().accent());
                for (int i = 0; i < years.size(); i++) {
                    int yr = years.get(i);
                    long cnt = perYear.getOrDefault(yr, 0L);
                    int bH = (int) (chartH * cnt / maxVal);
                    int x = pad + i * (barW + 4);
                    int y = pad + chartH - bH;
                    g2.fillRect(x, y, barW, bH);
                    g2.setColor(herramienta.UITheme.palette().textDark());
                    g2.setFont(chartFont9);
                    g2.drawString(String.valueOf(yr), x, getHeight() - 5);
                    g2.drawString(String.valueOf(cnt), x + 2, y - 2);
                    g2.setColor(herramienta.UITheme.palette().accent());
                }
            }
        };
    }

    public static javax.swing.JPanel buildPublisherChart(List<Llibre> books) {
        java.util.Map<String, Long> byPublisher = books.stream()
            .filter(l -> l.getEditorial() != null && !l.getEditorial().isEmpty())
            .collect(Collectors.groupingBy(Llibre::getEditorial, Collectors.counting()));
        java.util.List<java.util.Map.Entry<String, Long>> top = byPublisher.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10).collect(Collectors.toList());
        java.awt.Font chartFont10 = herramienta.UITheme.fontBase().deriveFont(10f);
        return new javax.swing.JPanel() {
            { setPreferredSize(new java.awt.Dimension(560, 200)); setBackground(herramienta.UITheme.palette().bgPanel()); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_publishers"))); }
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
                    g2.setColor(herramienta.UITheme.palette().textMid());
                    String lblTrunc = lbl.length() > 18 ? lbl.substring(0, 17) + "…" : lbl;
                    g2.drawString(lblTrunc, pad, y + chartH - 6);
                    g2.setColor(herramienta.UITheme.palette().accent());
                    g2.fillRect(lblW + pad, y + 2, barW, chartH - 4);
                    g2.setColor(herramienta.UITheme.palette().textDark());
                    g2.drawString(String.valueOf(cnt), lblW + pad + barW + 4, y + chartH - 6);
                }
            }
        };
    }

    public static javax.swing.JPanel buildTagCloud(List<Llibre> books, BibliotecaWriter cd) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 6));
        panel.setBackground(herramienta.UITheme.palette().bgPanel());
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_tab_tags")));

        java.util.Set<Long> bookIsbns = books.stream().map(Llibre::getISBN).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Integer, String> tagIdToName = new java.util.HashMap<>();
        for (domini.Tag t : cd.getAllTags()) tagIdToName.put(t.getId(), t.getNom());
        java.util.Map<String, Long> tagCount = new java.util.HashMap<>();
        for (persistencia.LlibreTagRow row : cd.getAllLlibreTagRows()) {
            if (!bookIsbns.contains(row.isbn())) continue;
            String nom = tagIdToName.get(row.tagId());
            if (nom != null) tagCount.merge(nom, 1L, Long::sum);
        }
        if (tagCount.isEmpty()) {
            javax.swing.JLabel lbl = new javax.swing.JLabel(I18n.t("stats_no_tags"));
            presentacio.UIComponents.styleLabel(lbl);
            panel.add(lbl);
            return panel;
        }
        long maxCount = tagCount.values().stream().mapToLong(v -> v).max().orElse(1);
        tagCount.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                float size = 11f + 14f * e.getValue() / maxCount;
                javax.swing.JLabel lbl = new javax.swing.JLabel(e.getKey() + " (" + e.getValue() + ")");
                lbl.setFont(herramienta.UITheme.fontBase().deriveFont(size));
                lbl.setForeground(herramienta.UITheme.palette().accent());
                lbl.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                lbl.setToolTipText(e.getValue() + " " + I18n.t("stats_books_with_tag"));
                panel.add(lbl);
            });
        return panel;
    }

    static javax.swing.JPanel buildReadingPacePanel(List<Llibre> books, java.util.Map<Integer, Long> byYear) {
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBackground(herramienta.UITheme.palette().bgPanel());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

        int currentYear = java.time.LocalDate.now().getYear();
        int dayOfYear = java.time.LocalDate.now().getDayOfYear();

        long finishedThisYear = byYear.getOrDefault(currentYear, 0L);
        double booksPerDay = dayOfYear > 0 ? (double) finishedThisYear / dayOfYear : 0;
        double booksPerMonth = booksPerDay * 30.44;
        double projectedYear = booksPerDay * 365;

        long totalPages = books.stream().mapToLong(Llibre::getPaginesLlegides).sum();
        int goal = herramienta.Config.getReadingGoal();

        java.util.function.Consumer<String> addLine = text -> {
            javax.swing.JLabel lbl = new javax.swing.JLabel(text);
            presentacio.UIComponents.styleLabel(lbl);
            lbl.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            panel.add(lbl);
            panel.add(javax.swing.Box.createVerticalStrut(6));
        };
        addLine.accept(I18n.t("stats_pace_year", currentYear, finishedThisYear));
        addLine.accept(I18n.t("stats_pace_per_month", String.format("%.1f", booksPerMonth)));
        addLine.accept(I18n.t("stats_pace_projected", String.format("%.0f", projectedYear)));
        if (goal > 0) {
            int remaining = Math.max(0, (int) (goal - finishedThisYear));
            int daysLeft = 365 - dayOfYear;
            addLine.accept(I18n.t("stats_pace_goal_remaining", goal, remaining, daysLeft));
        }
        addLine.accept(I18n.t("stats_pace_total_pages", totalPages));
        return panel;
    }

    public static String buildStatsSummary(BookStats stats, String scope) {
        return buildStatsSummary(stats, scope,
            stats.total > 0 ? String.format("%.1f", 100.0 * stats.llegits / stats.total) + "%" : "0%");
    }

    private static String buildStatsSummary(BookStats stats, String scope, String llegitsPercent) {
        String topAnys = stats.booksByReadYear.entrySet().stream()
            .filter(e -> e.getKey() > 0)
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> "  " + e.getKey() + ": " + e.getValue() + " " + (e.getValue() > 1 ? I18n.t("stats_book_plural") : I18n.t("stats_book_singular")))
            .collect(Collectors.joining("\n"));
        return scope + "\n" +
            I18n.t("stats_total") + " " + stats.total + "  ·  " +
            I18n.t("stats_llegits_colon") + " " + stats.llegits + " (" + llegitsPercent + ")  ·  " +
            I18n.t("stats_no_llegits_colon") + " " + (stats.total - stats.llegits) + "\n" +
            I18n.t("stats_avg_rating_colon") + " " + String.format("%.2f", stats.avgValoracio) + " / 10  ·  " +
            I18n.t("stats_avg_price_colon") + " " + String.format("%.2f", stats.avgPreu) + " " + herramienta.Config.getCurrencySymbol() + "\n" +
            I18n.t("stats_top_years") + "\n" + (topAnys.isEmpty() ? "  " + I18n.t("stats_no_years") : topAnys);
    }

    public static String buildStatsSummary(List<Llibre> llibres, String scope) {
        return buildStatsSummary(computeStats(llibres), scope);
    }

    /**
     * Builds the "General" stats tab: goal progress + text summary + per-shelf table.
     * All view construction lives here so the controller stays slim.
     */
    public static javax.swing.JPanel buildGeneralTab(List<Llibre> global, BookStats globalStats, BibliotecaWriter cd) {
        String summary = buildStatsSummary(globalStats, I18n.t("lbl_all_library"));

        javax.swing.JTextArea txtSummary = new javax.swing.JTextArea(summary);
        txtSummary.setEditable(false);
        txtSummary.setFont(herramienta.UITheme.fontBase());
        txtSummary.setBackground(herramienta.UITheme.palette().bgPanel());
        txtSummary.setForeground(herramienta.UITheme.palette().textDark());
        txtSummary.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));

        javax.swing.JScrollPane shelfScroll = buildShelfTable(global, cd);

        javax.swing.JPanel goalPanel = buildReadingGoalPanel(global, globalStats);
        javax.swing.JPanel statsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        statsPanel.setBackground(herramienta.UITheme.palette().bgPanel());
        statsPanel.add(txtSummary, java.awt.BorderLayout.NORTH);
        if (shelfScroll != null) statsPanel.add(shelfScroll, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel tab = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        tab.setBackground(herramienta.UITheme.palette().bgPanel());
        tab.add(goalPanel, java.awt.BorderLayout.NORTH);
        tab.add(statsPanel, java.awt.BorderLayout.CENTER);
        return tab;
    }

    private static javax.swing.JScrollPane buildShelfTable(List<Llibre> global, BibliotecaWriter cd) {
        java.util.Map<Long, Llibre> byIsbn = new java.util.HashMap<>();
        for (Llibre l : global) byIsbn.put(l.getISBN(), l);
        java.util.Map<Integer, java.util.List<Llibre>> shelfBooks = new java.util.HashMap<>();
        for (persistencia.LlibreLlistaRow row : cd.getAllLlibreLlistaRows()) {
            Llibre l = byIsbn.get(row.isbn());
            if (l != null) shelfBooks.computeIfAbsent(row.llistaId(), k -> new java.util.ArrayList<>()).add(l);
        }
        javax.swing.table.DefaultTableModel shelfModel = new javax.swing.table.DefaultTableModel(
            new String[]{I18n.t("col_stats_llista"), I18n.t("col_stats_llibres"), I18n.t("col_stats_llegits"),
                I18n.t("col_stats_pct"), I18n.t("col_stats_val")}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (domini.Llista ll : cd.getAllLlistes()) {
            java.util.List<Llibre> shelf = shelfBooks.getOrDefault(ll.getId(), java.util.List.of());
            if (shelf.isEmpty()) { shelfModel.addRow(new Object[]{ll.getNom(), 0, 0, "0.0%", "—"}); continue; }
            long llegits = shelf.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
            double avgVal = shelf.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
            shelfModel.addRow(new Object[]{
                ll.getNom(), shelf.size(), llegits,
                String.format("%.1f%%", 100.0 * llegits / shelf.size()),
                String.format("%.2f", avgVal)
            });
        }
        if (shelfModel.getRowCount() == 0) return null;
        javax.swing.JTable shelfTable = new javax.swing.JTable(shelfModel);
        shelfTable.setFont(herramienta.UITheme.fontBase());
        shelfTable.setBackground(herramienta.UITheme.palette().bgPanel());
        shelfTable.setForeground(herramienta.UITheme.palette().textDark());
        shelfTable.setRowHeight(26);
        shelfTable.setEnabled(false);
        shelfTable.getTableHeader().setFont(herramienta.UITheme.fontBold());
        javax.swing.JScrollPane shelfScroll = new javax.swing.JScrollPane(shelfTable);
        shelfScroll.setPreferredSize(new java.awt.Dimension(480, Math.min(200, shelfModel.getRowCount() * 27 + 30)));
        shelfScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("lbl_per_list")));
        return shelfScroll;
    }

    private static javax.swing.JPanel buildReadingGoalPanel(List<Llibre> global, BookStats globalStats) {
        int totalLlegits = (int) global.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
        int savedGoal = herramienta.Config.getReadingGoal();
        javax.swing.JPanel goalPanel = new javax.swing.JPanel(new java.awt.BorderLayout(6, 4));
        goalPanel.setBackground(herramienta.UITheme.palette().bgPanel());
        goalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(herramienta.UITheme.palette().borderClr()),
            I18n.t("lbl_reading_goal_section"), javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP, herramienta.UITheme.fontBold(), herramienta.UITheme.palette().textMid()));
        javax.swing.JProgressBar goalBar = new javax.swing.JProgressBar(0, Math.max(savedGoal, 1));
        goalBar.setValue(Math.min(totalLlegits, Math.max(savedGoal, 1)));
        goalBar.setStringPainted(true);
        goalBar.setFont(herramienta.UITheme.fontBase());
        javax.swing.JSpinner goalSpinner = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(Math.max(savedGoal, 1), 1, 9999, 1));
        goalSpinner.setFont(herramienta.UITheme.fontBase());
        goalSpinner.setPreferredSize(new java.awt.Dimension(70, 28));
        goalSpinner.addChangeListener(ev -> {
            int goal = (int) goalSpinner.getValue();
            herramienta.UiConfig.setReadingGoal(goal);
            goalBar.setMaximum(goal);
            goalBar.setValue(Math.min(totalLlegits, goal));
            goalBar.setString(totalLlegits + " / " + goal);
        });
        goalBar.setMaximum(Math.max(savedGoal, 1));
        goalBar.setString(totalLlegits + " / " + Math.max(savedGoal, 1));
        javax.swing.JLabel lblGoal = new javax.swing.JLabel(I18n.t("lbl_goal"));
        UIComponents.styleLabel(lblGoal);
        javax.swing.JPanel goalControls = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        goalControls.setBackground(herramienta.UITheme.palette().bgPanel());
        goalControls.add(lblGoal);
        goalControls.add(goalSpinner);
        goalControls.add(new javax.swing.JLabel(I18n.t("lbl_read_count", totalLlegits)));
        goalPanel.add(goalControls, java.awt.BorderLayout.NORTH);
        goalPanel.add(goalBar, java.awt.BorderLayout.CENTER);
        return goalPanel;
    }

    public static void showDialog(java.awt.Window parent, javax.swing.JComponent[] tabs, String[] tabTitles) {
        javax.swing.JDialog dlg = new javax.swing.JDialog(parent,
            I18n.t("dlg_stats_title"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

        javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
        tabbedPane.setBackground(herramienta.UITheme.palette().bgPanel());
        for (int i = 0; i < tabs.length; i++) {
            String title = i < tabTitles.length ? tabTitles[i] : ("Tab " + i);
            tabbedPane.addTab(title, tabs[i]);
        }

        dlg.getContentPane().setBackground(herramienta.UITheme.palette().bgPanel());
        dlg.add(tabbedPane);
        javax.swing.JButton btnClose = new javax.swing.JButton(I18n.t("btn_close"));
        presentacio.UIComponents.styleSecondaryButton(btnClose);
        btnClose.addActionListener(e -> dlg.dispose());
        dlg.getRootPane().setDefaultButton(btnClose);
        dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(),
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.JPanel btnPanel = new javax.swing.JPanel();
        btnPanel.setBackground(herramienta.UITheme.palette().bgPanel());
        btnPanel.add(btnClose);
        dlg.add(btnPanel, java.awt.BorderLayout.SOUTH);
        dlg.setSize(600, 500);
        dlg.setMinimumSize(new java.awt.Dimension(500, 400));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
}
