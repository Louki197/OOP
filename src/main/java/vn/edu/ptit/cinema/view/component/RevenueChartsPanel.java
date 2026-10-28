package vn.edu.ptit.cinema.view.component;

import vn.edu.ptit.cinema.model.booking.Invoice;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;

/** Two charts sharing the report's filtered invoices. No external chart runtime. */
public final class RevenueChartsPanel extends JPanel {
    private final Bars revenue = new Bars(false);
    private final Bars tickets = new Bars(true);

    public RevenueChartsPanel() {
        super(new GridLayout(1, 2, 16, 0));
        setOpaque(false);
        add(card("Doanh thu theo ngày", "Đơn vị: triệu đồng · Sau giảm giá", revenue));
        add(card("Số vé theo phim", "5 phim bán nhiều vé nhất", tickets));
        setPreferredSize(new Dimension(800, 255));
        setMinimumSize(new Dimension(400, 220));
    }

    private JPanel card(String title, String subtitle, Bars chart) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 231, 236)), new EmptyBorder(12, 14, 8, 14)));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        JLabel name = new JLabel(title);
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel hint = new JLabel(subtitle);
        hint.setForeground(new Color(103, 113, 131));
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        heading.add(name);
        heading.add(hint);
        panel.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(chart, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getHorizontalScrollBar().setUnitIncrement(64);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void setInvoices(List<Invoice> invoices, LocalDate firstDay, LocalDate lastDay) {
        TreeMap<LocalDate, Double> daily = new TreeMap<>();
        Map<String, Double> counts = new TreeMap<>();
        for (Invoice invoice : invoices) {
            if (invoice.getCreatedAt() != null) {
                daily.merge(invoice.getCreatedAt().toLocalDate(), invoice.getFinalAmount(), Double::sum);
            }
            String title = invoice.getMovieTitle() == null ? "Không rõ phim" : invoice.getMovieTitle();
            counts.merge(title, invoice.getTickets() == null ? 0.0 : (double) invoice.getTickets().size(), Double::sum);
        }
        if (firstDay == null && !daily.isEmpty()) firstDay = daily.keySet().iterator().next();
        if (lastDay == null && !daily.isEmpty()) lastDay = daily.lastKey();
        // Fill empty days in short periods, without allocating years of empty chart bars.
        if (firstDay != null && lastDay != null && java.time.temporal.ChronoUnit.DAYS.between(firstDay, lastDay) <= 366) {
            for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) daily.putIfAbsent(day, 0.0);
        }
        Map<String, Double> days = new LinkedHashMap<>();
        daily.forEach((day, value) -> days.put(day.format(DateTimeFormatter.ofPattern("dd/MM/yy")), value));
        Map<String, Double> top = new LinkedHashMap<>();
        counts.entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5).forEach(e -> top.put(e.getKey(), e.getValue()));
        revenue.setValues(days);
        tickets.setValues(top);
    }

    private static final class Bars extends JPanel {
        private final boolean horizontal;
        private Map<String, Double> values = new LinkedHashMap<>();
        private final Map<Rectangle, String> hits = new LinkedHashMap<>();
        Bars(boolean horizontal) {
            this.horizontal = horizontal;
            setBackground(Color.WHITE);
            setToolTipText("");
        }
        void setValues(Map<String, Double> values) {
            this.values = values;
            hits.clear();
            setPreferredSize(new Dimension(horizontal ? 350 : Math.max(350, values.size() * 60 + 60), 170));
            revalidate();
            repaint();
        }
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            for (Map.Entry<Rectangle, String> hit : hits.entrySet()) {
                if (hit.getKey().contains(event.getPoint())) return hit.getValue();
            }
            return null;
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                hits.clear();
                if (values.isEmpty()) {
                    g.setColor(new Color(103, 113, 131));
                    g.drawString("Chưa có dữ liệu trong khoảng thời gian này", 12, 70);
                    return;
                }
                double max = Math.max(1, values.values().stream().mapToDouble(Double::doubleValue).max().orElse(1));
                int w = getWidth(), h = getHeight(), i = 0;
                if (horizontal) {
                    int step = Math.max(28, Math.min(38, (h - 8) / values.size()));
                    for (Map.Entry<String, Double> entry : values.entrySet()) {
                        int y = i++ * step + 4;
                        String label = entry.getKey();
                        while (g.getFontMetrics().stringWidth(label) > w - 70 && label.length() > 1) label = label.substring(0, label.length() - 1);
                        if (!label.equals(entry.getKey())) label += "…";
                        g.setColor(new Color(36, 44, 58));
                        g.drawString(label, 0, y + 11);
                        String count = String.format("%.0f vé", entry.getValue());
                        g.drawString(count, w - g.getFontMetrics().stringWidth(count) - 3, y + 11);
                        g.setColor(new Color(244, 245, 247));
                        g.fillRoundRect(0, y + 18, w - 4, 9, 4, 4);
                        g.setColor(new Color(220, 38, 38));
                        g.fillRoundRect(0, y + 18, (int) ((w - 4) * entry.getValue() / max), 9, 4, 4);
                        hits.put(new Rectangle(0, y, w, step), entry.getKey() + ": " + count);
                    }
                } else {
                    int left = 42, bottom = h - 27, plotHeight = Math.max(30, h - 54);
                    double ceiling = Math.ceil(max / 1_000_000.0) * 1_000_000;
                    if (ceiling == 0) ceiling = 1_000_000;
                    for (int tick = 0; tick <= 4; tick++) {
                        int y = bottom - tick * plotHeight / 4;
                        g.setColor(new Color(232, 235, 240));
                        g.drawLine(left, y, w - 8, y);
                        g.setColor(new Color(103, 113, 131));
                        g.drawString(String.format("%.1f", ceiling * tick / 4 / 1_000_000), 2, y + 4);
                    }
                    int slot = Math.max(1, (w - left - 8) / values.size());
                    for (Map.Entry<String, Double> entry : values.entrySet()) {
                        int x = left + i++ * slot;
                        int height = (int) (plotHeight * entry.getValue() / ceiling);
                        int barWidth = Math.max(4, Math.min(40, slot - 16));
                        int barX = x + (slot - barWidth) / 2;
                        g.setColor(new Color(220, 38, 38));
                        g.fillRoundRect(barX, bottom - height, barWidth, height, 5, 5);
                        g.setColor(new Color(36, 44, 58));
                        String amount = String.format("%.2f", entry.getValue() / 1_000_000);
                        g.drawString(amount, x + (slot - g.getFontMetrics().stringWidth(amount)) / 2, bottom - height - 6);
                        g.setColor(new Color(103, 113, 131));
                        g.drawString(entry.getKey(), x + (slot - g.getFontMetrics().stringWidth(entry.getKey())) / 2, bottom + 18);
                        hits.put(new Rectangle(x, 0, slot, h), entry.getKey() + String.format(": %,.0f VNĐ", entry.getValue()));
                    }
                }
            } finally { g.dispose(); }
        }
    }
}
