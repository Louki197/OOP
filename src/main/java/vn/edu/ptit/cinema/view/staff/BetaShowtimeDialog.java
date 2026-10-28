package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.cinema.Room;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.util.PosterManager;

import vn.edu.ptit.cinema.service.BookingRules;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * [BETA CINEMAS SHOWTIME POPUP]
 * Cửa sổ popup chọn suất chiếu chuẩn phong cách Beta Cinemas & CGV:
 * 1. Thanh Tab Ngày nằm ngang: Ngày to bản + Thứ/Tháng (27/10 - T3, 28/10 - T4...)
 * 2. Phân loại định dạng: 2D PHỤ ĐỀ / 2D LỒNG TIẾNG
 * 3. Hộp Suất Chiếu (Showtime Pills): Giờ bắt đầu to đậm + Số lượng ghế trống thực tế
 * 4. Bấm vào bất kỳ ô giờ nào sẽ vào thẳng sơ đồ ghế của suất chiếu đó.
 */
@SuppressWarnings("serial")
public class BetaShowtimeDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final Movie movie;
    private final Consumer<ShowTime> onShowTimeSelected;
    private final DataStore dataStore = DataStore.getInstance();

    private LocalDate selectedDate;
    private JPanel dateTabsContainer;
    private JPanel showtimePillsContainer;
    private final List<LocalDate> availableDates = new ArrayList<>();
    private final Map<LocalDate, JButton> dateButtonMap = new HashMap<>();

    public BetaShowtimeDialog(Frame parent, Movie movie, Consumer<ShowTime> onShowTimeSelected) {
        super(parent, "CHỌN SUẤT CHIẾU - " + (movie != null ? movie.getTitle().toUpperCase() : ""), true);
        this.movie = movie;
        this.onShowTimeSelected = onShowTimeSelected;

        initData();
        initUI();
    }

    private void initData() {
        if (movie == null) return;
        Set<LocalDate> dateSet = new TreeSet<>();
        for (ShowTime st : dataStore.getShowTimeMap().values()) {
            if (st.getMovieId().equals(movie.getId()) && BookingRules.canSell(st, java.time.LocalDateTime.now())) {
                dateSet.add(st.getStartTime().toLocalDate());
            }
        }
        availableDates.addAll(dateSet);
        if (!availableDates.isEmpty()) {
            selectedDate = availableDates.get(0);
        }
    }

    private void initUI() {
        setSize(780, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(248, 250, 252));

        // 1. HEADER: THÔNG TIN PHIM + POSTER THU NHỎ
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. BODY: TAB NGÀY + LƯỚI SUẤT CHIẾU
        JPanel bodyPanel = new JPanel(new BorderLayout(0, 12));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(new EmptyBorder(12, 20, 16, 20));

        // A. Thanh chọn ngày (Date Tabs)
        dateTabsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dateTabsContainer.setOpaque(false);
        renderDateTabs();

        JScrollPane scrollDateTabs = new JScrollPane(dateTabsContainer);
        scrollDateTabs.setBorder(null);
        scrollDateTabs.setOpaque(false);
        scrollDateTabs.getViewport().setOpaque(false);
        scrollDateTabs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollDateTabs.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        JPanel dateSection = new JPanel(new BorderLayout(0, 6));
        dateSection.setOpaque(false);
        JLabel lblDateTitle = new JLabel("CHỌN NGÀY CHIẾU");
        lblDateTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDateTitle.setForeground(new Color(100, 116, 139));
        dateSection.add(lblDateTitle, BorderLayout.NORTH);
        dateSection.add(scrollDateTabs, BorderLayout.CENTER);

        bodyPanel.add(dateSection, BorderLayout.NORTH);

        // B. Lưới thẻ suất chiếu (Showtime Pills)
        JPanel showtimeSection = new JPanel(new BorderLayout(0, 8));
        showtimeSection.setOpaque(false);

        JLabel lblFormatTitle = new JLabel("2D PHỤ ĐỀ  |  DIGITAL CINEMA");
        lblFormatTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFormatTitle.setForeground(new Color(30, 41, 59));
        showtimeSection.add(lblFormatTitle, BorderLayout.NORTH);

        showtimePillsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        showtimePillsContainer.setBackground(Color.WHITE);
        showtimePillsContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));
        showtimePillsContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");

        JScrollPane scrollPills = new JScrollPane(showtimePillsContainer);
        scrollPills.setBorder(null);
        scrollPills.setOpaque(false);
        scrollPills.getViewport().setOpaque(false);
        scrollPills.getVerticalScrollBar().setUnitIncrement(14);
        showtimeSection.add(scrollPills, BorderLayout.CENTER);

        bodyPanel.add(showtimeSection, BorderLayout.CENTER);
        add(bodyPanel, BorderLayout.CENTER);

        // 3. FOOTER: NÚT ĐÓNG
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnClose = new JButton("Đóng lại");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 6,18,6,18;");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);

        // Render danh sách giờ chiếu của ngày đầu tiên
        renderShowtimePills();
    }

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(14, 20, 14, 20)
        ));

        // Poster thu nhỏ 95x135px
        int pw = 95;
        int ph = 135;
        JPanel posterBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                PosterManager.drawPosterOrPlaceholder(g2, movie, getWidth(), getHeight());
                g2.dispose();
            }
        };
        posterBox.setPreferredSize(new Dimension(pw, ph));
        posterBox.setMinimumSize(new Dimension(pw, ph));
        posterBox.setMaximumSize(new Dimension(pw, ph));
        posterBox.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        // Info box bên phải poster
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        // Dòng 1: Tên phim + Badge tuổi
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel(movie != null ? movie.getTitle().toUpperCase() : "PHIM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(15, 23, 42));

        String rating = (movie != null && movie.getAgeRating() != null) ? movie.getAgeRating() : "P";
        JLabel lblBadge = new JLabel(" " + rating + " ", JLabel.CENTER);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setOpaque(true);
        lblBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        setAgeBadgeStyle(lblBadge, rating);

        titleRow.add(lblTitle);
        titleRow.add(lblBadge);

        JLabel lblGenre = new JLabel("• Thể loại: " + (movie != null ? movie.getGenre() : ""));
        lblGenre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblGenre.setForeground(new Color(71, 85, 105));
        lblGenre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDur = new JLabel("• Thời lượng: " + (movie != null ? movie.getDurationMinutes() : 0) + " phút");
        lblDur.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDur.setForeground(new Color(71, 85, 105));
        lblDur.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDir = new JLabel("• Đạo diễn: " + (movie != null ? movie.getDirector() : "N/A"));
        lblDir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDir.setForeground(new Color(71, 85, 105));
        lblDir.setAlignmentX(Component.LEFT_ALIGNMENT);

        long totalShows = dataStore.getShowTimeMap().values().stream()
                .filter(st -> st.getMovieId().equals(movie.getId()))
                .count();
        JLabel lblShows = new JLabel("• Tổng số suất chiếu: " + totalShows + " suất");
        lblShows.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblShows.setForeground(new Color(37, 99, 235));
        lblShows.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(titleRow);
        info.add(Box.createVerticalStrut(6));
        info.add(lblGenre);
        info.add(Box.createVerticalStrut(3));
        info.add(lblDur);
        info.add(Box.createVerticalStrut(3));
        info.add(lblDir);
        info.add(Box.createVerticalStrut(4));
        info.add(lblShows);

        p.add(posterBox, BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        return p;
    }

    /**
     * DỰNG CÁC TAB NGÀY NẰM NGANG KIỂU BETA CINEMAS
     */
    private void renderDateTabs() {
        dateTabsContainer.removeAll();
        dateButtonMap.clear();

        if (availableDates.isEmpty()) {
            JLabel lblNoDate = new JLabel("Phim chưa có lịch chiếu sắp tới.");
            lblNoDate.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lblNoDate.setForeground(Color.GRAY);
            dateTabsContainer.add(lblNoDate);
            return;
        }

        for (LocalDate d : availableDates) {
            String dayNum = String.valueOf(d.getDayOfMonth());
            String monthDay = String.format("/%02d - %s", d.getMonthValue(), getVietnameseDayOfWeek(d.getDayOfWeek()));

            JButton btnTab = new JButton();
            btnTab.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 6));
            btnTab.setPreferredSize(new Dimension(108, 56));
            btnTab.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

            JLabel lblDay = new JLabel(dayNum);
            lblDay.setFont(new Font("Segoe UI", Font.BOLD, 22));

            JLabel lblSub = new JLabel(monthDay);
            lblSub.setFont(new Font("Segoe UI", Font.BOLD, 12));

            btnTab.add(lblDay);
            btnTab.add(lblSub);

            boolean isSelected = d.isEqual(selectedDate);
            applyDateTabStyle(btnTab, lblDay, lblSub, isSelected);

            btnTab.addActionListener(e -> {
                this.selectedDate = d;
                for (Map.Entry<LocalDate, JButton> entry : dateButtonMap.entrySet()) {
                    boolean sel = entry.getKey().isEqual(selectedDate);
                    JButton b = entry.getValue();
                    JLabel l1 = (JLabel) b.getComponent(0);
                    JLabel l2 = (JLabel) b.getComponent(1);
                    applyDateTabStyle(b, l1, l2, sel);
                }
                renderShowtimePills();
            });

            dateButtonMap.put(d, btnTab);
            dateTabsContainer.add(btnTab);
        }

        dateTabsContainer.revalidate();
        dateTabsContainer.repaint();
    }

    private void applyDateTabStyle(JButton btn, JLabel lblDay, JLabel lblSub, boolean isSelected) {
        if (isSelected) {
            btn.setBackground(new Color(2, 132, 199)); // Xanh dương rực rỡ như Beta
            btn.setForeground(Color.WHITE);
            lblDay.setForeground(Color.WHITE);
            lblSub.setForeground(new Color(224, 242, 254));
            btn.setBorder(BorderFactory.createLineBorder(new Color(3, 105, 161), 2));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(30, 41, 59));
            lblDay.setForeground(new Color(15, 23, 42));
            lblSub.setForeground(new Color(100, 116, 139));
            btn.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        }
    }

    /**
     * DỰNG CÁC THẺ SUẤT CHIẾU HÌNH HỘP (SHOWTIME PILLS) CHUẨN BETA CINEMAS
     */
    private void renderShowtimePills() {
        showtimePillsContainer.removeAll();

        if (selectedDate == null) {
            showEmptyPillsNotice("Vui lòng chọn ngày chiếu.");
            return;
        }

        List<ShowTime> list = new ArrayList<>();
        for (ShowTime st : dataStore.getShowTimeMap().values()) {
            if (st.getMovieId().equals(movie.getId())
                    && BookingRules.canSell(st, java.time.LocalDateTime.now())
                    && st.getStartTime().toLocalDate().isEqual(selectedDate)) {
                list.add(st);
            }
        }

        if (list.isEmpty()) {
            showEmptyPillsNotice("Không có suất chiếu nào cho ngày này.");
            return;
        }

        list.sort(Comparator.comparing(ShowTime::getStartTime));
        DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("HH:mm");

        for (ShowTime st : list) {
            // Đếm số ghế trống thực tế
            long availableSeats = 0;
            if (st.getSeats() != null) {
                availableSeats = st.getSeats().values().stream()
                        .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                        .count();
            }

            Room room = dataStore.getRoomMap().get(st.getRoomId());
            String roomName = (room != null) ? room.getName() : st.getRoomId();

            JPanel pill = new JPanel();
            pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
            pill.setPreferredSize(new Dimension(145, 68));
            pill.setBackground(new Color(241, 245, 249)); // Màu xám nhạt tinh tế kiểu Beta
            pill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            pill.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel lblTime = new JLabel(st.getStartTime().format(timeDtf), JLabel.CENTER);
            lblTime.setFont(new Font("Segoe UI", Font.BOLD, 17));
            lblTime.setForeground(new Color(15, 23, 42));
            lblTime.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblSeats = new JLabel(availableSeats + " ghế trống", JLabel.CENTER);
            lblSeats.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (availableSeats <= 10) {
                lblSeats.setForeground(new Color(225, 29, 72)); // Báo đỏ sắp hết vé
            } else {
                lblSeats.setForeground(new Color(37, 99, 235)); // Màu xanh dương
            }
            lblSeats.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblRoom = new JLabel(roomName, JLabel.CENTER);
            lblRoom.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblRoom.setForeground(new Color(100, 116, 139));
            lblRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

            pill.add(lblTime);
            pill.add(Box.createVerticalStrut(2));
            pill.add(lblSeats);
            pill.add(Box.createVerticalStrut(1));
            pill.add(lblRoom);

            // Hover effect
            pill.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    pill.setBackground(new Color(224, 242, 254)); // Highlight xanh nhạt
                    pill.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(2, 132, 199), 2),
                            new EmptyBorder(7, 9, 7, 9)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    pill.setBackground(new Color(241, 245, 249));
                    pill.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                            new EmptyBorder(8, 10, 8, 10)
                    ));
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!BookingRules.canSell(st, java.time.LocalDateTime.now())) {
                        JOptionPane.showMessageDialog(BetaShowtimeDialog.this, "Suất chiếu đã bắt đầu. Vui lòng chọn suất khác.");
                        renderShowtimePills();
                        return;
                    }
                    // Chọn suất chiếu này -> Đóng popup và kích hoạt callback sang Sơ đồ ghế
                    dispose();
                    if (onShowTimeSelected != null) {
                        onShowTimeSelected.accept(st);
                    }
                }
            });

            showtimePillsContainer.add(pill);
        }

        showtimePillsContainer.revalidate();
        showtimePillsContainer.repaint();
    }

    private void showEmptyPillsNotice(String msg) {
        JLabel lblEmpty = new JLabel(msg);
        lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblEmpty.setForeground(Color.GRAY);
        showtimePillsContainer.add(lblEmpty);
        showtimePillsContainer.revalidate();
        showtimePillsContainer.repaint();
    }

    private String getVietnameseDayOfWeek(java.time.DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private void setAgeBadgeStyle(JLabel lbl, String rating) {
        if (rating == null) rating = "P";
        switch (rating.toUpperCase()) {
            case "C18", "T18" -> {
                lbl.setBackground(new Color(244, 63, 94));
                lbl.setForeground(Color.WHITE);
            }
            case "C16" -> {
                lbl.setBackground(new Color(249, 115, 22));
                lbl.setForeground(Color.WHITE);
            }
            case "C13" -> {
                lbl.setBackground(new Color(234, 179, 8));
                lbl.setForeground(Color.BLACK);
            }
            default -> {
                lbl.setBackground(new Color(34, 197, 94));
                lbl.setForeground(Color.WHITE);
            }
        }
    }
}
