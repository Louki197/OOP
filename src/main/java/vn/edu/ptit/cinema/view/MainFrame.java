package vn.edu.ptit.cinema.view;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.account.User;
import vn.edu.ptit.cinema.model.booking.Invoice;
import vn.edu.ptit.cinema.model.booking.OrderItem;
import vn.edu.ptit.cinema.model.cinema.Room;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.model.seat.SeatType;
import vn.edu.ptit.cinema.pattern.observer.SeatEventBus;
import vn.edu.ptit.cinema.pattern.strategy.PricingStrategies;
import vn.edu.ptit.cinema.pattern.strategy.PricingStrategy;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.service.SessionManager;
import vn.edu.ptit.cinema.view.auth.LoginFrame;
import vn.edu.ptit.cinema.view.component.CurvedScreenPanel;
import vn.edu.ptit.cinema.view.staff.FnBDialog;
import vn.edu.ptit.cinema.view.staff.PaymentDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [MAIN FRAME - TỐI ƯU TOÀN DIỆN CHUẨN CGV/LOTTE]
 * 1. CardLayout cho Tab Bán Vé: Sảnh Chọn Phim (Movie Lobby) <-> Sơ Đồ Ghế.
 * 2. Tách riêng phân hệ Quản lý Phim & Lên Lịch Suất Chiếu (chọn ngày, giờ, phòng, giá).
 * 3. Bảo lưu ghế giữ và đồng hồ đếm ngược liên tục khi chuyển tab.
 * 4. Nới rộng ComboBox giờ chiếu, loại bỏ hoàn toàn các ký tự Tofu [].
 * 5. 100% Pure Java Swing (Không nhúng HTML/CSS).
 */
public class MainFrame extends JFrame implements ActionListener {

    private final DataStore dataStore = DataStore.getInstance();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    private JTabbedPane tabbedPane;

    // --- TAB 1: CARDLAYOUT CHO BÁN VÉ ---
    private CardLayout bookingCardLayout;
    private JPanel bookingCardContainer;
    private static final String CARD_LOBBY = "CARD_LOBBY";
    private static final String CARD_SEATMAP = "CARD_SEATMAP";

    // Panel Sảnh Chọn Phim (Lobby)
    private JPanel movieGridPanel;

    // Panel Sơ Đồ Ghế của Phim đã chọn
    private Movie selectedMovieForBooking;
    private JLabel lblCurrentMovieTitle;
    private JLabel lblCurrentMovieBadge;
    private JComboBox<String> cbDate;
    private JComboBox<String> cbTime;
    private boolean isCascadingUpdating = false;

    private JPanel seatMatrixContainer;
    private final Map<String, JButton> seatButtons = new ConcurrentHashMap<>();
    private ShowTime currentShowTime;

    private final Set<String> selectedPendingSeats = Collections.synchronizedSet(new LinkedHashSet<>());
    private long lastSeatClickTimestamp = 0;

    // Sidebar Widgets
    private JPanel chipsPanel;
    private JLabel lblTicketTitle;
    private JLabel lblTicketValue;
    private JLabel lblCountdownTimer;
    private javax.swing.Timer swingCountdownTimer;
    private int remainingSeconds = 300;

    private JButton btnProceedToFnB;
    private JButton btnCancelAllHold;

    // --- TAB 2: QUẢN LÝ PHIM & LÊN LỊCH SUẤT CHIẾU ---
    private JTable tableMovies;
    private DefaultTableModel modelMovies;
    private JTextField txtSearchMovie;
    private JComboBox<String> cbGenreFilter;
    private JTextField txtMovieId, txtMovieTitle, txtMovieGenre, txtMovieDuration, txtMovieDirector;
    private JComboBox<String> cbAgeRating;
    private JLabel lblMovieCount;
    private JButton btnAddMovie, btnDeleteMovie;

    // Phân hệ Lên lịch chiếu
    private JComboBox<String> cbScheduleMovie;
    private JComboBox<String> cbScheduleDate;
    private JSpinner spScheduleHour;
    private JSpinner spScheduleMinute;
    private JComboBox<String> cbScheduleRoom;
    private JTextField txtSchedulePrice;
    private JButton btnCreateShowTime;

    // --- TAB 3: BÁO CÁO & DASHBOARD KPI CARDS ---
    private JLabel lblKpiRevenue, lblKpiTickets, lblKpiFnB, lblKpiTopMovie;
    private JTable tableInvoices;
    private DefaultTableModel modelInvoices;
    private JComboBox<String> cbFilterPeriod;
    private JButton btnSortByRevenue, btnSortByDate, btnExportReport;

    public MainFrame() {
        initUI();
        initSeatObserver();
    }

    private void initUI() {
        String roleStr = currentUser != null ? currentUser.getRole().getDisplayName() : "Khách";
        String nameStr = currentUser != null ? currentUser.getFullName() : "N/A";
        setTitle("HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM PTIT - [" + nameStr + " | " + roleStr + "]");
        setSize(1360, 860);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("BÁN VÉ & SƠ ĐỒ RẠP", createBookingCardContainer());
        tabbedPane.addTab("QUẢN LÝ PHIM & LỊCH CHIẾU", createMovieManagementTab());

        if (!SessionManager.getInstance().isAdmin()) {
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setToolTipTextAt(1, "Chức năng chỉ dành riêng cho Quản trị viên (Admin)!");
        }

        tabbedPane.addTab("THỐNG KÊ & DOANH THU", createReportTab());

        // LẮNG NGHE CHUYỂN TAB ĐỒNG BỘ DỮ LIỆU
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                refreshBookingTabState();
            } else if (selectedIndex == 2) {
                reloadReportDashboard();
            }
        });

        setContentPane(tabbedPane);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuSystem = new JMenu("Hệ thống");
        menuSystem.setMnemonic('H');

        JMenuItem miSave = new JMenuItem("Lưu dữ liệu ngay (Ctrl+S)");
        miSave.setAccelerator(KeyStroke.getKeyStroke("control S"));
        miSave.addActionListener(e -> {
            new Thread(() -> {
                dataStore.saveAllData();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Đã lưu toàn bộ dữ liệu xuống file JSON an toàn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE)
                );
            }).start();
        });

        JMenuItem miLogout = new JMenuItem("Đăng xuất");
        miLogout.addActionListener(e -> {
            SeatEventBus.clear();
            SessionManager.getInstance().logout();
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        JMenuItem miExit = new JMenuItem("Thoát");
        miExit.addActionListener(e -> System.exit(0));

        menuSystem.add(miSave);
        menuSystem.addSeparator();
        menuSystem.add(miLogout);
        menuSystem.add(miExit);

        JMenu menuHelp = new JMenu("Trợ giúp");
        JMenuItem miAbout = new JMenuItem("Giới thiệu Đồ án BTL");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "BÀI TẬP LỚN LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP) - PTIT\n"
                        + "Hệ Thống Quản Lý Rạp Chiếu Phim Toàn Diện (No-SQL + Swing FlatLaf)\n"
                        + "Đầy đủ 5 Design Patterns: MVC, DAO, Singleton, Factory, Strategy, Observer.",
                "Giới thiệu", JOptionPane.INFORMATION_MESSAGE));
        menuHelp.add(miAbout);

        menuBar.add(menuSystem);
        menuBar.add(menuHelp);
        setJMenuBar(menuBar);
    }

    // =========================================================================
    // 1. TAB 1: CARDLAYOUT (SẢNH CHỌN PHIM <-> PHÒNG CHIẾU SƠ ĐỒ GHẾ)
    // =========================================================================
    private JPanel createBookingCardContainer() {
        bookingCardLayout = new CardLayout();
        bookingCardContainer = new JPanel(bookingCardLayout);

        // Card 1: Sảnh chọn phim
        bookingCardContainer.add(createMovieLobbyPanel(), CARD_LOBBY);

        // Card 2: Sơ đồ ghế của phim được chọn
        bookingCardContainer.add(createSeatMapPanel(), CARD_SEATMAP);

        bookingCardLayout.show(bookingCardContainer, CARD_LOBBY);
        return bookingCardContainer;
    }

    /**
     * MÀN HÌNH 1: SẢNH CHỌN PHIM (MOVIE LOBBY)
     */
    private JPanel createMovieLobbyPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        panel.setBackground(new Color(248, 250, 252));

        // Tiêu đề Sảnh
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("DANH SÁCH PHIM ĐANG CHIẾU TẠI RẠP PTIT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Vui lòng chọn một bộ phim để xem lịch chiếu và chọn ghế ngồi");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(Color.GRAY);

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        // Lưới danh sách phim
        movieGridPanel = new JPanel(new GridLayout(0, 3, 18, 18));
        movieGridPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(movieGridPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        reloadMovieLobbyGrid();
        return panel;
    }

    private void reloadMovieLobbyGrid() {
        movieGridPanel.removeAll();

        for (Movie m : dataStore.getMovieMap().values()) {
            JPanel card = new JPanel(new BorderLayout(10, 10));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(15, 15, 15, 15)
            ));

            // Header Card: Tên Phim & Badge Độ tuổi
            JPanel topCard = new JPanel(new BorderLayout(8, 0));
            topCard.setOpaque(false);

            JLabel lblName = new JLabel(m.getTitle());
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblName.setForeground(new Color(15, 23, 42));

            JLabel lblBadge = new JLabel(m.getAgeRating(), JLabel.CENTER);
            lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblBadge.setOpaque(true);
            lblBadge.setPreferredSize(new Dimension(36, 22));
            setAgeBadgeStyle(lblBadge, m.getAgeRating());

            topCard.add(lblName, BorderLayout.CENTER);
            topCard.add(lblBadge, BorderLayout.EAST);
            card.add(topCard, BorderLayout.NORTH);

            // Body Card: Thông tin chi tiết
            JPanel infoPanel = new JPanel(new GridLayout(4, 1, 0, 4));
            infoPanel.setOpaque(false);

            JLabel lblGenre = new JLabel("Thể loại: " + m.getGenre());
            lblGenre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblGenre.setForeground(new Color(71, 85, 105));

            JLabel lblDur = new JLabel("Thời lượng: " + m.getDurationMinutes() + " phút");
            lblDur.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDur.setForeground(new Color(71, 85, 105));

            JLabel lblDir = new JLabel("Đạo diễn: " + m.getDirector());
            lblDir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDir.setForeground(new Color(71, 85, 105));

            // Đếm số suất chiếu hiện có của phim này
            long showCount = dataStore.getShowTimeMap().values().stream()
                    .filter(st -> st.getMovieId().equals(m.getId()))
                    .count();
            JLabel lblShows = new JLabel("Số suất chiếu: " + showCount + " suất");
            lblShows.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblShows.setForeground(new Color(37, 99, 235));

            infoPanel.add(lblGenre);
            infoPanel.add(lblDur);
            infoPanel.add(lblDir);
            infoPanel.add(lblShows);
            card.add(infoPanel, BorderLayout.CENTER);

            // Nút bấm Chọn Phim Này
            JButton btnSelect = new JButton("CHỌN PHIM NÀY");
            btnSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnSelect.setBackground(new Color(220, 38, 38));
            btnSelect.setForeground(Color.WHITE);
            btnSelect.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

            btnSelect.addActionListener(e -> selectMovieAndOpenSeatMap(m));

            card.add(btnSelect, BorderLayout.SOUTH);
            movieGridPanel.add(card);
        }

        movieGridPanel.revalidate();
        movieGridPanel.repaint();
    }

    private void setAgeBadgeStyle(JLabel lbl, String rating) {
        if (rating == null) rating = "P";
        switch (rating) {
            case "C18":
                lbl.setBackground(new Color(254, 226, 226));
                lbl.setForeground(new Color(220, 38, 38));
                lbl.setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 1));
                break;
            case "C16":
                lbl.setBackground(new Color(255, 237, 213));
                lbl.setForeground(new Color(234, 88, 12));
                lbl.setBorder(BorderFactory.createLineBorder(new Color(249, 115, 22), 1));
                break;
            case "C13":
                lbl.setBackground(new Color(254, 240, 138));
                lbl.setForeground(new Color(161, 98, 7));
                lbl.setBorder(BorderFactory.createLineBorder(new Color(234, 179, 8), 1));
                break;
            default:
                lbl.setBackground(new Color(220, 252, 231));
                lbl.setForeground(new Color(22, 163, 74));
                lbl.setBorder(BorderFactory.createLineBorder(new Color(34, 197, 94), 1));
                break;
        }
    }

    /**
     * CHUYỂN SANG MÀN HÌNH 2: SƠ ĐỒ GHẾ CỦA PHIM ĐƯỢC CHỌN
     */
    private void selectMovieAndOpenSeatMap(Movie m) {
        this.selectedMovieForBooking = m;
        lblCurrentMovieTitle.setText(m.getTitle());
        lblCurrentMovieBadge.setText(m.getAgeRating());
        setAgeBadgeStyle(lblCurrentMovieBadge, m.getAgeRating());

        reloadDateComboForSelectedMovie();
        bookingCardLayout.show(bookingCardContainer, CARD_SEATMAP);
    }

    /**
     * MÀN HÌNH 2: PHÒNG CHIẾU & SƠ ĐỒ GHẾ
     */
    private JPanel createSeatMapPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(12, 15, 12, 15));

        // --- 1.1 Thanh Điều Hướng Trên: Nút Quay Lại + Tên Phim + Ngày/Giờ + Legend ---
        JPanel topContainer = new JPanel(new BorderLayout(15, 0));
        topContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        topContainer.setBackground(Color.WHITE);

        // Bên trái: Nút Quay lại Sảnh + Tên Phim
        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        leftNav.setOpaque(false);

        JButton btnBackToLobby = new JButton("< Đổi Phim Khác");
        btnBackToLobby.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBackToLobby.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnBackToLobby.addActionListener(e -> {
            if (!selectedPendingSeats.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Bạn đang giữ " + selectedPendingSeats.size() + " ghế. Đổi sang phim khác sẽ hủy các ghế này.\nBạn có chắc chắn muốn quay lại sảnh không?",
                        "Xác nhận đổi phim",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            releaseSelectedPendingSeats();
            reloadMovieLobbyGrid();
            bookingCardLayout.show(bookingCardContainer, CARD_LOBBY);
        });
        leftNav.add(btnBackToLobby);

        lblCurrentMovieTitle = new JLabel("Tên Phim Đang Chọn");
        lblCurrentMovieTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCurrentMovieTitle.setForeground(new Color(15, 23, 42));
        leftNav.add(lblCurrentMovieTitle);

        lblCurrentMovieBadge = new JLabel("P", JLabel.CENTER);
        lblCurrentMovieBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCurrentMovieBadge.setOpaque(true);
        lblCurrentMovieBadge.setPreferredSize(new Dimension(36, 22));
        leftNav.add(lblCurrentMovieBadge);

        // ComboBox Ngày & Giờ (Kéo rộng ra 220px, không bị cắt chữ)
        JLabel lblDate = new JLabel("Ngày:");
        lblDate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbDate = new JComboBox<>();
        cbDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbDate.setPreferredSize(new Dimension(115, 32));

        JLabel lblTime = new JLabel("Giờ Chiếu:");
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbTime = new JComboBox<>();
        cbTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbTime.setPreferredSize(new Dimension(220, 32)); // Nới rộng 220px

        cbDate.addActionListener(e -> onDateSelectedForMovie());
        cbTime.addActionListener(e -> onTimeSelectedForMovie());

        leftNav.add(Box.createHorizontalStrut(10));
        leftNav.add(lblDate);
        leftNav.add(cbDate);
        leftNav.add(lblTime);
        leftNav.add(cbTime);

        topContainer.add(leftNav, BorderLayout.WEST);

        // Bên phải: Chú thích 2 tầng
        JPanel legendBox = new JPanel(new GridLayout(2, 1, 0, 4));
        legendBox.setOpaque(false);

        JPanel rowTier = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rowTier.setOpaque(false);
        rowTier.add(new JLabel("Hạng ghế:"));
        rowTier.add(createLegendBadge("Ghế Thường", new Color(34, 197, 94), null));
        rowTier.add(createLegendBadge("Ghế VIP (Viền vàng)", new Color(34, 197, 94), new Color(245, 158, 11)));
        rowTier.add(createLegendBadge("Sweetbox Đôi", new Color(236, 72, 153), new Color(219, 39, 119)));

        JPanel rowStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rowStatus.setOpaque(false);
        rowStatus.add(new JLabel("Trạng thái:"));
        rowStatus.add(createLegendBadge("Trống", new Color(34, 197, 94), null));
        rowStatus.add(createLegendBadge("Đang chọn", new Color(59, 130, 246), null));
        rowStatus.add(createLegendBadge("Đang giữ (5p)", new Color(234, 179, 8), null));
        rowStatus.add(createLegendBadge("Đã bán", new Color(220, 38, 38), null));

        legendBox.add(rowTier);
        legendBox.add(rowStatus);
        topContainer.add(legendBox, BorderLayout.EAST);

        panel.add(topContainer, BorderLayout.NORTH);

        // --- 1.2 Không gian rạp: Màn hình cong 3D + Trục tọa độ + Lối đi ---
        JPanel theaterCenterPanel = new JPanel(new BorderLayout(0, 10));
        theaterCenterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(12, 18, 12, 18)
        ));
        theaterCenterPanel.setBackground(Color.WHITE);

        theaterCenterPanel.add(new CurvedScreenPanel(), BorderLayout.NORTH);

        seatMatrixContainer = new JPanel(new GridBagLayout());
        seatMatrixContainer.setOpaque(false);

        JScrollPane scrollSeats = new JScrollPane(seatMatrixContainer);
        scrollSeats.setBorder(null);
        scrollSeats.setOpaque(false);
        scrollSeats.getViewport().setOpaque(false);
        theaterCenterPanel.add(scrollSeats, BorderLayout.CENTER);

        panel.add(theaterCenterPanel, BorderLayout.CENTER);

        // --- 1.3 Sidebar Tinh Gọn ---
        JPanel rightSidebar = new JPanel();
        rightSidebar.setLayout(new BoxLayout(rightSidebar, BoxLayout.Y_AXIS));
        rightSidebar.setPreferredSize(new Dimension(320, 0));
        rightSidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(18, 15, 18, 15)
        ));
        rightSidebar.setBackground(Color.WHITE);

        JPanel timerCard = new JPanel(new BorderLayout());
        timerCard.setBackground(new Color(254, 243, 199));
        timerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        timerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        lblCountdownTimer = new JLabel("Thời gian giữ ghế: 05:00", JLabel.CENTER);
        lblCountdownTimer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCountdownTimer.setForeground(new Color(180, 83, 9));
        timerCard.add(lblCountdownTimer, BorderLayout.CENTER);
        rightSidebar.add(timerCard);
        rightSidebar.add(Box.createVerticalStrut(15));

        JLabel lblHeading = new JLabel("GHẾ ĐANG CHỌN");
        lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHeading.setForeground(new Color(30, 41, 59));
        rightSidebar.add(lblHeading);
        rightSidebar.add(Box.createVerticalStrut(8));

        chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        chipsPanel.setBackground(Color.WHITE);

        JScrollPane scrollChips = new JScrollPane(chipsPanel);
        scrollChips.setPreferredSize(new Dimension(0, 115));
        scrollChips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        scrollChips.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(2, 2, 2, 2)
        ));
        scrollChips.getViewport().setBackground(Color.WHITE);
        scrollChips.getVerticalScrollBar().setUnitIncrement(8);
        rightSidebar.add(scrollChips);
        rightSidebar.add(Box.createVerticalStrut(15));

        JPanel ticketTotalBox = new JPanel(new GridLayout(2, 1, 0, 3));
        ticketTotalBox.setOpaque(false);
        ticketTotalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        lblTicketTitle = new JLabel("Tạm tính vé:");
        lblTicketTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTicketTitle.setForeground(new Color(51, 65, 85));

        lblTicketValue = new JLabel("0 VNĐ");
        lblTicketValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTicketValue.setForeground(new Color(220, 38, 38));

        ticketTotalBox.add(lblTicketTitle);
        ticketTotalBox.add(lblTicketValue);
        rightSidebar.add(ticketTotalBox);
        rightSidebar.add(Box.createVerticalStrut(20));

        JPanel stepBox = new JPanel(new GridLayout(3, 1, 0, 5));
        stepBox.setOpaque(false);
        stepBox.setBorder(BorderFactory.createTitledBorder("Quy trình 3 bước"));
        stepBox.add(new JLabel("Bước 1: Chọn ghế trên sơ đồ"));
        stepBox.add(new JLabel("Bước 2: Chọn thêm Bắp Nước"));
        stepBox.add(new JLabel("Bước 3: Áp Voucher & Thanh toán"));
        rightSidebar.add(stepBox);

        rightSidebar.add(Box.createVerticalGlue());

        // Nút TIẾP TỤC chuẩn ASCII (Không bao giờ lỗi font Tofu)
        btnProceedToFnB = new JButton("TIẾP TỤC >");
        btnProceedToFnB.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnProceedToFnB.setBackground(new Color(220, 38, 38));
        btnProceedToFnB.setForeground(Color.WHITE);
        btnProceedToFnB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnProceedToFnB.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        btnProceedToFnB.setEnabled(false);
        btnProceedToFnB.addActionListener(e -> openFnBDialog());

        btnCancelAllHold = new JButton("Hủy toàn bộ ghế đang chọn");
        btnCancelAllHold.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelAllHold.setForeground(new Color(100, 116, 139));
        btnCancelAllHold.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnCancelAllHold.putClientProperty(FlatClientProperties.STYLE, "arc: 10; borderWidth: 1;");
        btnCancelAllHold.addActionListener(e -> releaseSelectedPendingSeats());

        rightSidebar.add(btnProceedToFnB);
        rightSidebar.add(Box.createVerticalStrut(10));
        rightSidebar.add(btnCancelAllHold);

        panel.add(rightSidebar, BorderLayout.EAST);

        startCountdownTimer();
        return panel;
    }

    private JPanel createLegendBadge(String title, Color bgColor, Color borderColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(15, 15));
        box.setBackground(bgColor);
        if (borderColor != null) {
            box.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        } else {
            box.setBorder(BorderFactory.createLineBorder(new Color(100, 116, 139), 1));
        }
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.add(box);
        p.add(lbl);
        return p;
    }

    // =========================================================================
    // LOGIC CASCADING COMBOBOX CỦA RIÊNG PHIM ĐANG CHỌN (NGÀY -> GIỜ)
    // =========================================================================

    private void reloadDateComboForSelectedMovie() {
        if (selectedMovieForBooking == null) return;
        isCascadingUpdating = true;
        try {
            cbDate.removeAllItems();
            cbTime.removeAllItems();

            Set<LocalDate> dates = new TreeSet<>();
            for (ShowTime st : dataStore.getShowTimeMap().values()) {
                if (st.getMovieId().equals(selectedMovieForBooking.getId()) && st.getStartTime() != null) {
                    dates.add(st.getStartTime().toLocalDate());
                }
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (LocalDate d : dates) {
                cbDate.addItem(d.format(dtf));
            }

            if (cbDate.getItemCount() > 0) {
                cbDate.setSelectedIndex(0);
                populateTimesForSelectedDateInternal();
            } else {
                currentShowTime = null;
                showEmptySeatMatrix("Phim này hiện chưa có suất chiếu nào được lên lịch.");
            }
        } finally {
            isCascadingUpdating = false;
        }
    }

    private void onDateSelectedForMovie() {
        if (isCascadingUpdating) return;
        isCascadingUpdating = true;
        try {
            releaseSelectedPendingSeats();
            populateTimesForSelectedDateInternal();
        } finally {
            isCascadingUpdating = false;
        }
    }

    private void populateTimesForSelectedDateInternal() {
        String dateStr = (String) cbDate.getSelectedItem();
        cbTime.removeAllItems();

        if (dateStr == null || selectedMovieForBooking == null) {
            currentShowTime = null;
            showEmptySeatMatrix("Vui lòng chọn ngày chiếu.");
            return;
        }

        LocalDate selectedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        List<ShowTime> matchingShowTimes = new ArrayList<>();
        for (ShowTime st : dataStore.getShowTimeMap().values()) {
            if (st.getMovieId().equals(selectedMovieForBooking.getId())
                    && st.getStartTime() != null
                    && st.getStartTime().toLocalDate().isEqual(selectedDate)) {
                matchingShowTimes.add(st);
            }
        }

        matchingShowTimes.sort(Comparator.comparing(ShowTime::getStartTime));
        DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("HH:mm");

        for (ShowTime st : matchingShowTimes) {
            Room room = dataStore.getRoomMap().get(st.getRoomId());
            String roomName = (room != null) ? room.getName() : st.getRoomId();
            // Format ngắn gọn, dễ đọc: 18:00 (Phòng 1)
            cbTime.addItem(String.format("[%s] %s (%s)", st.getId(), st.getStartTime().format(timeDtf), roomName));
        }

        if (cbTime.getItemCount() > 0) {
            cbTime.setSelectedIndex(0);
            loadSelectedShowTime();
        } else {
            currentShowTime = null;
            showEmptySeatMatrix("Không có giờ chiếu nào trong ngày " + dateStr);
        }
    }

    private void onTimeSelectedForMovie() {
        if (isCascadingUpdating) return;
        loadSelectedShowTime();
    }

    private void loadSelectedShowTime() {
        String selectedTime = (String) cbTime.getSelectedItem();
        if (selectedTime == null || !selectedTime.contains("[") || !selectedTime.contains("]")) {
            currentShowTime = null;
            showEmptySeatMatrix("Vui lòng chọn suất chiếu.");
            return;
        }

        String showTimeId = selectedTime.substring(selectedTime.indexOf('[') + 1, selectedTime.indexOf(']'));
        currentShowTime = dataStore.getShowTimeMap().get(showTimeId);

        if (currentShowTime != null) {
            restoreUserHeldSeatsIfAny();
            renderSeatMatrixWithAxesAndAisles();
        } else {
            showEmptySeatMatrix("Không tìm thấy dữ liệu suất chiếu: " + showTimeId);
        }
    }

    /**
     * BẢO LƯU GHẾ GIỮ VÀ KHÔI PHỤC ĐỒNG HỒ ĐẾM NGƯỢC KHI CHUYỂN TAB
     */
    private void restoreUserHeldSeatsIfAny() {
        if (currentShowTime == null) return;
        String myUsername = (currentUser != null) ? currentUser.getUsername() : "staff";

        long now = System.currentTimeMillis();
        long earliestHoldTime = 0;

        for (Seat s : currentShowTime.getSeats().values()) {
            if (s.getStatus() == SeatStatus.PENDING && myUsername.equals(s.getHeldByUserId())) {
                selectedPendingSeats.add(s.getCode());
                if (earliestHoldTime == 0 || s.getHoldTimestamp() < earliestHoldTime) {
                    earliestHoldTime = s.getHoldTimestamp();
                }
            }
        }

        if (!selectedPendingSeats.isEmpty() && earliestHoldTime > 0) {
            long elapsedSeconds = (now - earliestHoldTime) / 1000;
            remainingSeconds = (int) Math.max(1, 300 - elapsedSeconds);
        } else {
            selectedPendingSeats.clear();
            remainingSeconds = 300;
        }

        refreshChipsAndTotals();
    }

    private void refreshBookingTabState() {
        reloadMovieLobbyGrid();
        if (currentShowTime != null) {
            restoreUserHeldSeatsIfAny();
            renderSeatMatrixWithAxesAndAisles();
        }
    }

    private void showEmptySeatMatrix(String message) {
        seatMatrixContainer.removeAll();
        seatButtons.clear();
        JLabel lblMsg = new JLabel(message, JLabel.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblMsg.setForeground(new Color(100, 116, 139));
        seatMatrixContainer.add(lblMsg);
        seatMatrixContainer.revalidate();
        seatMatrixContainer.repaint();
        refreshChipsAndTotals();
    }

    /**
     * DỰNG SƠ ĐỒ GHẾ VỚI TRỤC TỌA ĐỘ VÀ 2 LỐI ĐI THỰC TẾ
     */
    private void renderSeatMatrixWithAxesAndAisles() {
        seatMatrixContainer.removeAll();
        seatButtons.clear();

        if (currentShowTime == null || currentShowTime.getSeats() == null) {
            seatMatrixContainer.revalidate();
            seatMatrixContainer.repaint();
            return;
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.BOTH;

        int rows = 8;
        int cols = 10;

        // 1. Hàng số thứ tự cột trên đỉnh (1 ... 10)
        gbc.gridy = 0;
        gbc.gridx = 0;
        seatMatrixContainer.add(new JLabel(""), gbc);

        int gridX = 1;
        for (int c = 1; c <= cols; c++) {
            if (c == 3 || c == 9) {
                gbc.gridx = gridX++;
                seatMatrixContainer.add(Box.createHorizontalStrut(24), gbc);
            }
            gbc.gridx = gridX++;
            JLabel lblCol = new JLabel(String.valueOf(c), JLabel.CENTER);
            lblCol.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblCol.setForeground(new Color(100, 116, 139));
            lblCol.setPreferredSize(new Dimension(44, 22));
            seatMatrixContainer.add(lblCol, gbc);
        }
        gbc.gridx = gridX;
        seatMatrixContainer.add(new JLabel(""), gbc);

        // 2. Các hàng ghế (A -> H)
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            boolean isSweetboxRow = (r >= 6);
            boolean isVipRow = (r >= 2 && r < 6);

            gbc.gridy = r + 1;

            // Nhãn chữ cái bên trái (A-H)
            gbc.gridx = 0;
            JLabel lblRowLeft = new JLabel(String.valueOf(rowChar), JLabel.CENTER);
            lblRowLeft.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblRowLeft.setForeground(new Color(71, 85, 105));
            lblRowLeft.setPreferredSize(new Dimension(28, 42));
            seatMatrixContainer.add(lblRowLeft, gbc);

            gridX = 1;

            if (isSweetboxRow) {
                // SWEETBOX: ĐÔI 1-2, ĐÔI 3-4...
                int[][] pairs = {{1, 2}, {3, 4}, {5, 6}, {7, 8}, {9, 10}};
                for (int[] pair : pairs) {
                    int c1 = pair[0];
                    int c2 = pair[1];

                    if (c1 == 3 || c1 == 9) {
                        gbc.gridx = gridX++;
                        gbc.gridwidth = 1;
                        seatMatrixContainer.add(Box.createHorizontalStrut(24), gbc);
                    }

                    String code1 = String.format("%c%02d", rowChar, c1);
                    String code2 = String.format("%c%02d", rowChar, c2);
                    Seat s1 = currentShowTime.getSeats().get(code1);
                    Seat s2 = currentShowTime.getSeats().get(code2);

                    JButton btnSweetbox = new JButton(String.format("ĐÔI %d-%d", c1, c2));
                    btnSweetbox.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    btnSweetbox.setPreferredSize(new Dimension(94, 42));
                    btnSweetbox.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");

                    updateSweetboxAppearance(btnSweetbox, s1, s2);
                    btnSweetbox.setToolTipText(String.format("Ghế Đôi Sweetbox [%s - %s] - Giá: %,.0f VNĐ",
                            code1, code2, currentShowTime.getBasePrice() * SeatType.SWEETBOX.getPriceMultiplier()));

                    btnSweetbox.addActionListener(e -> onSweetboxClicked(code1, code2, btnSweetbox));

                    seatButtons.put(code1, btnSweetbox);
                    seatButtons.put(code2, btnSweetbox);

                    gbc.gridx = gridX;
                    gbc.gridwidth = 2;
                    seatMatrixContainer.add(btnSweetbox, gbc);
                    gridX += 2;
                }
                gbc.gridwidth = 1;

            } else {
                // GHẾ ĐƠN (Standard / VIP)
                for (int c = 1; c <= cols; c++) {
                    if (c == 3 || c == 9) {
                        gbc.gridx = gridX++;
                        seatMatrixContainer.add(Box.createHorizontalStrut(24), gbc);
                    }

                    String code = String.format("%c%02d", rowChar, c);
                    Seat seat = currentShowTime.getSeats().get(code);

                    JButton btnSeat = new JButton(String.valueOf(c));
                    btnSeat.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnSeat.setPreferredSize(new Dimension(44, 42));
                    btnSeat.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

                    updateSeatButtonAppearance(btnSeat, seat, isVipRow);

                    double price = currentShowTime.getBasePrice() * (isVipRow ? SeatType.VIP.getPriceMultiplier() : 1.0);
                    btnSeat.setToolTipText(String.format("Ghế %s [%s] - Giá: %,.0f VNĐ",
                            isVipRow ? "VIP" : "Thường", code, price));

                    btnSeat.addActionListener(e -> onSingleSeatClicked(code, btnSeat, isVipRow));

                    seatButtons.put(code, btnSeat);
                    gbc.gridx = gridX++;
                    seatMatrixContainer.add(btnSeat, gbc);
                }
            }

            // Nhãn chữ cái bên phải (A-H)
            gbc.gridx = gridX;
            JLabel lblRowRight = new JLabel(String.valueOf(rowChar), JLabel.CENTER);
            lblRowRight.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblRowRight.setForeground(new Color(71, 85, 105));
            lblRowRight.setPreferredSize(new Dimension(28, 42));
            seatMatrixContainer.add(lblRowRight, gbc);
        }

        seatMatrixContainer.revalidate();
        seatMatrixContainer.repaint();
    }

    private void updateSeatButtonAppearance(JButton btn, Seat seat, boolean isVip) {
        if (seat == null) return;

        if (selectedPendingSeats.contains(seat.getCode())) {
            btn.setBackground(new Color(59, 130, 246)); // Xanh dương (Đang chọn)
            btn.setForeground(Color.WHITE);
            if (isVip) {
                btn.setBorder(BorderFactory.createLineBorder(new Color(245, 158, 11), 2));
            } else {
                btn.setBorder(BorderFactory.createLineBorder(new Color(29, 78, 216), 2));
            }
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        } else if (seat.getStatus() == SeatStatus.BOOKED) {
            btn.setBackground(new Color(220, 38, 38)); // Đỏ đặc rực rỡ
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(185, 28, 28), 1));
            btn.setCursor(Cursor.getDefaultCursor());

        } else if (seat.getStatus() == SeatStatus.PENDING) {
            btn.setBackground(new Color(234, 179, 8)); // Vàng (Đang giữ)
            btn.setForeground(Color.BLACK);
            btn.setBorder(BorderFactory.createLineBorder(new Color(180, 83, 9), 1));
            btn.setCursor(Cursor.getDefaultCursor());

        } else {
            // AVAILABLE
            btn.setBackground(new Color(34, 197, 94)); // Xanh lá
            btn.setForeground(Color.WHITE);
            if (isVip) {
                btn.setBorder(BorderFactory.createLineBorder(new Color(245, 158, 11), 2)); // Viền vàng
            } else {
                btn.setBorder(BorderFactory.createLineBorder(new Color(22, 163, 74), 1));
            }
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private void updateSweetboxAppearance(JButton btn, Seat s1, Seat s2) {
        if (s1 == null || s2 == null) return;

        boolean isSelected = selectedPendingSeats.contains(s1.getCode()) || selectedPendingSeats.contains(s2.getCode());
        boolean isBooked = (s1.getStatus() == SeatStatus.BOOKED || s2.getStatus() == SeatStatus.BOOKED);
        boolean isPending = (s1.getStatus() == SeatStatus.PENDING || s2.getStatus() == SeatStatus.PENDING);

        if (isSelected) {
            btn.setBackground(new Color(59, 130, 246));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(219, 39, 119), 2));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        } else if (isBooked) {
            btn.setBackground(new Color(220, 38, 38));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(185, 28, 28), 1));
            btn.setCursor(Cursor.getDefaultCursor());

        } else if (isPending) {
            btn.setBackground(new Color(234, 179, 8));
            btn.setForeground(Color.BLACK);
            btn.setBorder(BorderFactory.createLineBorder(new Color(180, 83, 9), 1));
            btn.setCursor(Cursor.getDefaultCursor());

        } else {
            btn.setBackground(new Color(236, 72, 153)); // Hồng tím Sweetbox
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(219, 39, 119), 2));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private void onSingleSeatClicked(String seatCode, JButton btn, boolean isVip) {
        long clickNow = System.currentTimeMillis();
        if (clickNow - lastSeatClickTimestamp < 150) return; // Debounce spam click
        lastSeatClickTimestamp = clickNow;

        if (currentShowTime == null) return;
        Seat seat = currentShowTime.getSeats().get(seatCode);
        if (seat == null || seat.getStatus() == SeatStatus.BOOKED) return;

        if (selectedPendingSeats.contains(seatCode)) {
            removeSingleSeat(seatCode);
        } else {
            selectedPendingSeats.add(seatCode);
            seat.setStatus(SeatStatus.PENDING);
            seat.setHoldTimestamp(System.currentTimeMillis());
            seat.setHeldByUserId(currentUser != null ? currentUser.getUsername() : "staff");
            if (selectedPendingSeats.size() == 1) {
                remainingSeconds = 300;
            }
        }

        updateSeatButtonAppearance(btn, seat, isVip);
        refreshChipsAndTotals();
    }

    private void onSweetboxClicked(String code1, String code2, JButton btn) {
        long clickNow = System.currentTimeMillis();
        if (clickNow - lastSeatClickTimestamp < 150) return; // Debounce spam click
        lastSeatClickTimestamp = clickNow;

        if (currentShowTime == null) return;
        Seat s1 = currentShowTime.getSeats().get(code1);
        Seat s2 = currentShowTime.getSeats().get(code2);
        if (s1 == null || s2 == null) return;
        if (s1.getStatus() == SeatStatus.BOOKED || s2.getStatus() == SeatStatus.BOOKED) return;

        if (selectedPendingSeats.contains(code1) || selectedPendingSeats.contains(code2)) {
            selectedPendingSeats.remove(code1);
            selectedPendingSeats.remove(code2);
            s1.setStatus(SeatStatus.AVAILABLE);
            s2.setStatus(SeatStatus.AVAILABLE);
            s1.setHoldTimestamp(0);
            s2.setHoldTimestamp(0);
        } else {
            selectedPendingSeats.add(code1);
            selectedPendingSeats.add(code2);
            long now = System.currentTimeMillis();
            s1.setStatus(SeatStatus.PENDING);
            s2.setStatus(SeatStatus.PENDING);
            s1.setHoldTimestamp(now);
            s2.setHoldTimestamp(now);
            if (selectedPendingSeats.size() <= 2) {
                remainingSeconds = 300;
            }
        }

        updateSweetboxAppearance(btn, s1, s2);
        refreshChipsAndTotals();
    }

    private void removeSingleSeat(String code) {
        if (currentShowTime == null) return;
        Seat seat = currentShowTime.getSeats().get(code);
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHoldTimestamp(0);
            seat.setHeldByUserId(null);
        }
        selectedPendingSeats.remove(code);

        JButton btn = seatButtons.get(code);
        if (btn != null && seat != null) {
            boolean isVip = seat.getType() == SeatType.VIP;
            updateSeatButtonAppearance(btn, seat, isVip);
        }
        refreshChipsAndTotals();
    }

    private void refreshChipsAndTotals() {
        chipsPanel.removeAll();

        if (selectedPendingSeats.isEmpty()) {
            JLabel lblEmpty = new JLabel("Chưa chọn ghế nào");
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblEmpty.setForeground(Color.GRAY);
            chipsPanel.add(lblEmpty);

            lblTicketTitle.setText("Tạm tính vé:");
            lblTicketValue.setText("0 VNĐ");
            btnProceedToFnB.setEnabled(false);
        } else {
            PricingStrategy strategy = PricingStrategies.determineStrategy(
                    currentShowTime != null ? currentShowTime.getStartTime() : LocalDateTime.now(), false);
            double total = 0;

            for (String code : selectedPendingSeats) {
                if (currentShowTime != null) {
                    Seat seat = currentShowTime.getSeats().get(code);
                    total += strategy.calculatePrice(currentShowTime.getBasePrice(), seat, currentShowTime.getStartTime());
                }

                JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
                chip.setBackground(new Color(239, 246, 255));
                chip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                        new EmptyBorder(2, 6, 2, 4)
                ));

                JLabel lblCode = new JLabel(code);
                lblCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblCode.setForeground(new Color(29, 78, 216));

                JButton btnClose = new JButton("x");
                btnClose.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnClose.setForeground(new Color(147, 197, 253));
                btnClose.setBorder(null);
                btnClose.setContentAreaFilled(false);
                btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnClose.addActionListener(e -> removeSingleSeat(code));

                chip.add(lblCode);
                chip.add(btnClose);
                chipsPanel.add(chip);
            }

            lblTicketTitle.setText(String.format("Tạm tính vé (%d ghế):", selectedPendingSeats.size()));
            lblTicketValue.setText(String.format("%,.0f VNĐ", total));
            btnProceedToFnB.setEnabled(true);
        }

        chipsPanel.revalidate();
        chipsPanel.repaint();
    }

    private void openFnBDialog() {
        FnBDialog fnbDialog = new FnBDialog(this);
        fnbDialog.setVisible(true);

        if (fnbDialog.isProceedToPayment()) {
            openPaymentDialog(fnbDialog.getSelectedOrderItems());
        }
    }

    private void openPaymentDialog(List<OrderItem> foods) {
        PaymentDialog paymentDialog = new PaymentDialog(this, currentShowTime, selectedPendingSeats, foods);
        paymentDialog.setVisible(true);

        if (paymentDialog.isPaymentSuccessful()) {
            selectedPendingSeats.clear();
            renderSeatMatrixWithAxesAndAisles();
            refreshChipsAndTotals();
            reloadReportDashboard();
        }
    }

    private void releaseSelectedPendingSeats() {
        if (currentShowTime == null) return;
        for (String code : selectedPendingSeats) {
            Seat s = currentShowTime.getSeats().get(code);
            if (s != null && s.getStatus() == SeatStatus.PENDING) {
                s.setStatus(SeatStatus.AVAILABLE);
                s.setHoldTimestamp(0);
                s.setHeldByUserId(null);
            }
        }
        selectedPendingSeats.clear();
        renderSeatMatrixWithAxesAndAisles();
        refreshChipsAndTotals();
    }

    private void startCountdownTimer() {
        swingCountdownTimer = new javax.swing.Timer(1000, e -> {
            if (!selectedPendingSeats.isEmpty()) {
                remainingSeconds--;
                int min = Math.max(0, remainingSeconds / 60);
                int sec = Math.max(0, remainingSeconds % 60);
                lblCountdownTimer.setText(String.format("Thời gian giữ ghế: %02d:%02d", min, sec));

                if (remainingSeconds <= 0) {
                    JOptionPane.showMessageDialog(this, "Đã hết 5 phút giữ chỗ! Ghế đã tự động giải phóng.", "Hết giờ", JOptionPane.WARNING_MESSAGE);
                    releaseSelectedPendingSeats();
                    remainingSeconds = 300;
                }
            } else {
                remainingSeconds = 300;
                lblCountdownTimer.setText("Thời gian giữ ghế: 05:00");
            }
        });
        swingCountdownTimer.start();
    }

    private void initSeatObserver() {
        SeatEventBus.subscribe((showTimeId, seatCode) -> SwingUtilities.invokeLater(() -> {
            if (currentShowTime != null && currentShowTime.getId().equals(showTimeId)) {
                JButton btn = seatButtons.get(seatCode);
                Seat seat = currentShowTime.getSeats().get(seatCode);
                if (btn != null && seat != null) {
                    boolean isVip = seat.getType() == SeatType.VIP;
                    updateSeatButtonAppearance(btn, seat, isVip);
                }
            }
        }));
    }

    // =========================================================================
    // 2. TAB 2: QUẢN LÝ PHIM & LÊN LỊCH SUẤT CHIẾU (SCHEDULING PANEL)
    // =========================================================================
    private JPanel createMovieManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Thanh tìm kiếm phim (BorderLayout: Trái là bộ lọc, Phải là số lượng phim)
        JPanel searchBar = new JPanel(new BorderLayout(15, 0));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        searchBar.setBackground(Color.WHITE);

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);
        leftSearch.add(new JLabel("Tìm kiếm tên phim:"));
        txtSearchMovie = new JTextField(16);
        txtSearchMovie.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên phim...");
        txtSearchMovie.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { filterMoviesTable(); }
        });
        leftSearch.add(txtSearchMovie);

        leftSearch.add(new JLabel("Thể loại:"));
        cbGenreFilter = new JComboBox<>(new String[]{"Tất cả thể loại", "Lịch Sử / Chiến Tranh", "Khoa Học Viễn Tưởng", "Kinh Dị / Bí Ẩn", "Hành Động"});
        cbGenreFilter.addActionListener(e -> filterMoviesTable());
        leftSearch.add(cbGenreFilter);

        JButton btnResetFilter = new JButton("Làm mới");
        btnResetFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnResetFilter.addActionListener(e -> {
            txtSearchMovie.setText("");
            cbGenreFilter.setSelectedIndex(0);
            filterMoviesTable();
        });
        leftSearch.add(btnResetFilter);
        searchBar.add(leftSearch, BorderLayout.WEST);

        lblMovieCount = new JLabel("Tổng số: " + dataStore.getMovieMap().size() + " phim");
        lblMovieCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMovieCount.setForeground(new Color(37, 99, 235));
        searchBar.add(lblMovieCount, BorderLayout.EAST);

        panel.add(searchBar, BorderLayout.NORTH);

        // Bảng danh sách phim
        String[] cols = {"Mã Phim", "Tên Phim", "Thể Loại", "Thời Lượng", "Đạo Diễn", "Độ Tuổi"};
        modelMovies = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tableMovies = new JTable(modelMovies);
        tableMovies.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableMovies.setRowHeight(30);

        // Tối ưu độ rộng cột và căn lề
        tableMovies.setAutoCreateRowSorter(true);
        tableMovies.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableMovies.getColumnModel().getColumn(0).setMaxWidth(100);
        tableMovies.getColumnModel().getColumn(1).setPreferredWidth(260);
        tableMovies.getColumnModel().getColumn(2).setPreferredWidth(160);
        tableMovies.getColumnModel().getColumn(3).setPreferredWidth(95);
        tableMovies.getColumnModel().getColumn(4).setPreferredWidth(180);
        tableMovies.getColumnModel().getColumn(5).setPreferredWidth(80);
        tableMovies.getColumnModel().getColumn(5).setMaxWidth(100);

        DefaultTableCellRenderer centerColRenderer = new DefaultTableCellRenderer();
        centerColRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableMovies.getColumnModel().getColumn(0).setCellRenderer(centerColRenderer);
        tableMovies.getColumnModel().getColumn(3).setCellRenderer(centerColRenderer);

        tableMovies.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String rating = value != null ? value.toString() : "P";
                switch (rating) {
                    case "C18": lbl.setForeground(new Color(220, 38, 38)); break;
                    case "C16": lbl.setForeground(new Color(234, 88, 12)); break;
                    case "C13": lbl.setForeground(new Color(202, 138, 4)); break;
                    default: lbl.setForeground(new Color(22, 163, 74)); break;
                }
                return lbl;
            }
        });

        // Click dòng trên bảng để đổ ngược dữ liệu vào Form 1
        tableMovies.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tableMovies.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = tableMovies.convertRowIndexToModel(selectedRow);
                    String id = (String) modelMovies.getValueAt(modelRow, 0);
                    Movie m = dataStore.getMovieMap().get(id);
                    if (m != null) {
                        txtMovieId.setText(m.getId());
                        txtMovieTitle.setText(m.getTitle());
                        txtMovieGenre.setText(m.getGenre());
                        txtMovieDuration.setText(String.valueOf(m.getDurationMinutes()));
                        txtMovieDirector.setText(m.getDirector());
                        cbAgeRating.setSelectedItem(m.getAgeRating());
                    }
                }
            }
        });

        reloadMovieTable();
        JScrollPane scrollTable = new JScrollPane(tableMovies);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Danh Sách Phim Chiếu Rạp"));
        panel.add(scrollTable, BorderLayout.CENTER);

        // 2 FORM PHÍA DƯỚI: FORM THÊM PHIM & FORM LÊN LỊCH SUẤT CHIẾU (GRIDBAGLAYOUT CHUẨN)
        JPanel bottomFormsContainer = new JPanel(new GridLayout(1, 2, 15, 0));

        // Form 1: Thêm Phim Mới (Bố cục GridBagLayout 4 cột cân xứng)
        JPanel pnlAddMovie = new JPanel(new GridBagLayout());
        pnlAddMovie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("1. Thêm Phim Mới"),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pnlAddMovie.setBackground(Color.WHITE);

        txtMovieId = new JTextField("MOV0" + (dataStore.getMovieMap().size() + 1));
        txtMovieTitle = new JTextField();
        txtMovieGenre = new JTextField("Hành Động");
        txtMovieDuration = new JTextField("120");
        txtMovieDirector = new JTextField("Christopher Nolan");
        cbAgeRating = new JComboBox<>(new String[]{"P", "C13", "C16", "C18"});

        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.insets = new Insets(4, 6, 4, 6);
        gbc1.fill = GridBagConstraints.HORIZONTAL;

        // Dòng 0: Mã Phim & Tên Phim
        gbc1.gridx = 0; gbc1.gridy = 0; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Mã Phim:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 0; gbc1.weightx = 0.35;
        pnlAddMovie.add(txtMovieId, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 0; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Tên Phim:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 0; gbc1.weightx = 0.65;
        pnlAddMovie.add(txtMovieTitle, gbc1);

        // Dòng 1: Thể Loại & Thời Lượng
        gbc1.gridx = 0; gbc1.gridy = 1; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Thể Loại:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 1; gbc1.weightx = 0.35;
        pnlAddMovie.add(txtMovieGenre, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 1; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Thời Lượng:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 1; gbc1.weightx = 0.65;
        pnlAddMovie.add(txtMovieDuration, gbc1);

        // Dòng 2: Độ Tuổi & Đạo Diễn
        gbc1.gridx = 0; gbc1.gridy = 2; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Độ Tuổi:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 2; gbc1.weightx = 0.35;
        pnlAddMovie.add(cbAgeRating, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 2; gbc1.weightx = 0;
        pnlAddMovie.add(new JLabel("Đạo Diễn:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 2; gbc1.weightx = 0.65;
        pnlAddMovie.add(txtMovieDirector, gbc1);

        // Dòng 3: Cụm nút bấm
        btnAddMovie = new JButton("+ Lưu Phim");
        btnAddMovie.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddMovie.setBackground(new Color(220, 38, 38));
        btnAddMovie.setForeground(Color.WHITE);
        btnAddMovie.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnAddMovie.addActionListener(e -> handleAddMovie());

        btnDeleteMovie = new JButton("Xóa Phim");
        btnDeleteMovie.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteMovie.setForeground(new Color(100, 116, 139));
        btnDeleteMovie.putClientProperty(FlatClientProperties.STYLE, "arc: 8; borderWidth: 1;");
        btnDeleteMovie.addActionListener(e -> handleDeleteMovie());

        JButton btnClearForm = new JButton("Nhập lại");
        btnClearForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearForm.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnClearForm.addActionListener(e -> {
            txtMovieId.setText("MOV0" + (dataStore.getMovieMap().size() + 1));
            txtMovieTitle.setText("");
            txtMovieGenre.setText("Hành Động");
            txtMovieDuration.setText("120");
            txtMovieDirector.setText("");
            cbAgeRating.setSelectedIndex(0);
        });

        JPanel btnBoxMovie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBoxMovie.setOpaque(false);
        btnBoxMovie.add(btnClearForm);
        btnBoxMovie.add(btnDeleteMovie);
        btnBoxMovie.add(btnAddMovie);

        gbc1.gridx = 0; gbc1.gridy = 3; gbc1.gridwidth = 4;
        gbc1.fill = GridBagConstraints.NONE;
        gbc1.anchor = GridBagConstraints.EAST;
        gbc1.insets = new Insets(8, 6, 4, 6);
        pnlAddMovie.add(btnBoxMovie, gbc1);

        bottomFormsContainer.add(pnlAddMovie);

        // Form 2: Lên Lịch Suất Chiếu (Bố cục GridBagLayout 2 cột thẳng hàng chuẩn rạp)
        JPanel pnlSchedule = new JPanel(new GridBagLayout());
        pnlSchedule.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2. Lên Lịch Suất Chiếu Cho Phim"),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pnlSchedule.setBackground(Color.WHITE);

        cbScheduleMovie = new JComboBox<>();
        cbScheduleDate = new JComboBox<>();
        reloadScheduleDropdowns();

        // Bộ chọn giờ và phút
        JPanel timePickerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timePickerPanel.setOpaque(false);
        spScheduleHour = new JSpinner(new SpinnerNumberModel(18, 7, 23, 1));
        spScheduleMinute = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        timePickerPanel.add(spScheduleHour);
        timePickerPanel.add(new JLabel("giờ"));
        timePickerPanel.add(Box.createHorizontalStrut(8));
        timePickerPanel.add(spScheduleMinute);
        timePickerPanel.add(new JLabel("phút"));

        cbScheduleRoom = new JComboBox<>(new String[]{"ROOM01 (Phòng 1 - Standard)", "ROOM02 (Phòng 2 - IMAX Laser)"});
        txtSchedulePrice = new JTextField("85000");

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(4, 6, 4, 6);

        // Dòng 0: Chọn Phim
        gbc2.gridx = 0; gbc2.gridy = 0; gbc2.weightx = 0; gbc2.anchor = GridBagConstraints.WEST; gbc2.fill = GridBagConstraints.NONE;
        pnlSchedule.add(new JLabel("Chọn Phim:"), gbc2);
        gbc2.gridx = 1; gbc2.gridy = 0; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        pnlSchedule.add(cbScheduleMovie, gbc2);

        // Dòng 1: Ngày Chiếu
        gbc2.gridx = 0; gbc2.gridy = 1; gbc2.weightx = 0; gbc2.anchor = GridBagConstraints.WEST; gbc2.fill = GridBagConstraints.NONE;
        pnlSchedule.add(new JLabel("Ngày Chiếu:"), gbc2);
        gbc2.gridx = 1; gbc2.gridy = 1; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        pnlSchedule.add(cbScheduleDate, gbc2);

        // Dòng 2: Giờ Bắt Đầu
        gbc2.gridx = 0; gbc2.gridy = 2; gbc2.weightx = 0; gbc2.anchor = GridBagConstraints.WEST; gbc2.fill = GridBagConstraints.NONE;
        pnlSchedule.add(new JLabel("Giờ Bắt Đầu:"), gbc2);
        gbc2.gridx = 1; gbc2.gridy = 2; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        pnlSchedule.add(timePickerPanel, gbc2);

        // Dòng 3: Phòng Chiếu
        gbc2.gridx = 0; gbc2.gridy = 3; gbc2.weightx = 0; gbc2.anchor = GridBagConstraints.WEST; gbc2.fill = GridBagConstraints.NONE;
        pnlSchedule.add(new JLabel("Phòng Chiếu:"), gbc2);
        gbc2.gridx = 1; gbc2.gridy = 3; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        pnlSchedule.add(cbScheduleRoom, gbc2);

        // Dòng 4: Giá Vé Cơ Sở
        gbc2.gridx = 0; gbc2.gridy = 4; gbc2.weightx = 0; gbc2.anchor = GridBagConstraints.WEST; gbc2.fill = GridBagConstraints.NONE;
        pnlSchedule.add(new JLabel("Giá Vé Cơ Sở:"), gbc2);
        gbc2.gridx = 1; gbc2.gridy = 4; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        pnlSchedule.add(txtSchedulePrice, gbc2);

        // Dòng 5: Nút Tạo Suất Chiếu
        btnCreateShowTime = new JButton("+ Tạo Suất Chiếu");
        btnCreateShowTime.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreateShowTime.setBackground(new Color(37, 99, 235));
        btnCreateShowTime.setForeground(Color.WHITE);
        btnCreateShowTime.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnCreateShowTime.addActionListener(e -> handleCreateShowTime());

        JPanel btnBoxSchedule = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnBoxSchedule.setOpaque(false);
        btnBoxSchedule.add(btnCreateShowTime);

        gbc2.gridx = 0; gbc2.gridy = 5; gbc2.gridwidth = 2;
        gbc2.fill = GridBagConstraints.NONE;
        gbc2.anchor = GridBagConstraints.EAST;
        gbc2.insets = new Insets(8, 6, 4, 6);
        pnlSchedule.add(btnBoxSchedule, gbc2);

        bottomFormsContainer.add(pnlSchedule);
        panel.add(bottomFormsContainer, BorderLayout.SOUTH);

        return panel;
    }

    private void reloadScheduleDropdowns() {
        cbScheduleMovie.removeAllItems();
        for (Movie m : dataStore.getMovieMap().values()) {
            cbScheduleMovie.addItem(String.format("[%s] %s", m.getId(), m.getTitle()));
        }

        cbScheduleDate.removeAllItems();
        LocalDate now = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < 7; i++) {
            cbScheduleDate.addItem(now.plusDays(i).format(dtf));
        }
    }

    private void reloadMovieTable() {
        modelMovies.setRowCount(0);
        for (Movie m : dataStore.getMovieMap().values()) {
            modelMovies.addRow(new Object[]{
                    m.getId(), m.getTitle(), m.getGenre(), m.getDurationMinutes() + " phút", m.getDirector(), m.getAgeRating()
            });
        }
        if (lblMovieCount != null) {
            lblMovieCount.setText("Tổng số: " + dataStore.getMovieMap().size() + " phim");
        }
    }

    private void filterMoviesTable() {
        String keyword = txtSearchMovie.getText().trim().toLowerCase();
        String genre = (String) cbGenreFilter.getSelectedItem();
        boolean filterGenre = genre != null && !genre.equals("Tất cả thể loại");

        modelMovies.setRowCount(0);
        int count = 0;
        for (Movie m : dataStore.getMovieMap().values()) {
            boolean matchName = m.getTitle().toLowerCase().contains(keyword);
            boolean matchGenre = !filterGenre || m.getGenre().contains(genre);
            if (matchName && matchGenre) {
                modelMovies.addRow(new Object[]{
                        m.getId(), m.getTitle(), m.getGenre(), m.getDurationMinutes() + " phút", m.getDirector(), m.getAgeRating()
                });
                count++;
            }
        }
        if (lblMovieCount != null) {
            lblMovieCount.setText(String.format("Hiển thị %d/%d phim", count, dataStore.getMovieMap().size()));
        }
    }

    private void handleAddMovie() {
        String id = txtMovieId.getText().trim();
        String title = txtMovieTitle.getText().trim();
        String genre = txtMovieGenre.getText().trim();
        int dur;
        try {
            dur = Integer.parseInt(txtMovieDuration.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Thời lượng phim phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (id.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mã và tên phim!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String rating = (String) cbAgeRating.getSelectedItem();
        Movie m = new Movie(id, title, genre, dur, LocalDate.now(), "", "Phim mới", txtMovieDirector.getText(), rating);
        dataStore.getMovieMap().put(id, m);
        dataStore.saveAllData();

        reloadMovieTable();
        reloadScheduleDropdowns();
        reloadMovieLobbyGrid();

        JOptionPane.showMessageDialog(this, "Thêm phim thành công! Hãy lên lịch suất chiếu ở khung bên phải.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        txtMovieTitle.setText("");
        txtMovieId.setText("MOV0" + (dataStore.getMovieMap().size() + 1));
    }

    private void handleCreateShowTime() {
        String movieItem = (String) cbScheduleMovie.getSelectedItem();
        String dateStr = (String) cbScheduleDate.getSelectedItem();
        String roomItem = (String) cbScheduleRoom.getSelectedItem();

        if (movieItem == null || dateStr == null || roomItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ thông tin để tạo suất chiếu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(txtSchedulePrice.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá vé phải là số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String movieId = movieItem.substring(movieItem.indexOf('[') + 1, movieItem.indexOf(']'));
        String roomId = roomItem.startsWith("ROOM02") ? "ROOM02" : "ROOM01";

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int hour = (int) spScheduleHour.getValue();
        int minute = (int) spScheduleMinute.getValue();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        Movie m = dataStore.getMovieMap().get(movieId);
        int dur = (m != null) ? m.getDurationMinutes() : 120;
        LocalDateTime endTime = startTime.plusMinutes(dur + 15);

        // KIỂM TRA XUNG ĐỘT TRÙNG LỊCH PHÒNG CHIẾU (SCHEDULE CONFLICT CHECK)
        for (ShowTime st : dataStore.getShowTimeMap().values()) {
            if (st.getRoomId().equals(roomId) && st.getStartTime() != null && st.getEndTime() != null) {
                if (startTime.isBefore(st.getEndTime()) && endTime.isAfter(st.getStartTime())) {
                    Movie conflictMovie = dataStore.getMovieMap().get(st.getMovieId());
                    String conflictTitle = (conflictMovie != null) ? conflictMovie.getTitle() : st.getMovieId();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                    JOptionPane.showMessageDialog(this,
                            "XUNG ĐỘT LỊCH CHIẾU TẠI " + roomId + "!\n"
                                    + "Phòng này đã có phim: '" + conflictTitle + "'\n"
                                    + "Khung giờ chiếu: " + st.getStartTime().format(fmt) + " - " + st.getEndTime().format(fmt) + " ngày " + dateStr + ".\n"
                                    + "Vui lòng chọn khung giờ khác hoặc phòng chiếu khác!",
                            "Cảnh Báo Trùng Lịch", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        String stId = "ST" + (dataStore.getShowTimeMap().size() + 1);

        // Tạo ma trận ghế tương ứng theo phòng
        Room room = dataStore.getRoomMap().get(roomId);
        int rows = (room != null) ? room.getTotalRows() : 8;
        int cols = (room != null) ? room.getTotalCols() : 10;
        Map<String, Seat> seats = DataStore.generateRoomSeats(rows, cols);

        ShowTime newSt = new ShowTime(stId, movieId, roomId, startTime, endTime, price, seats);
        dataStore.getShowTimeMap().put(stId, newSt);
        dataStore.saveAllData();

        reloadMovieLobbyGrid();
        JOptionPane.showMessageDialog(this,
                "Tạo suất chiếu thành công!\n"
                        + "Mã suất: " + stId + "\n"
                        + "Phim: " + (m != null ? m.getTitle() : movieId) + "\n"
                        + "Thời gian: " + startTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + "\n"
                        + "Phòng: " + roomId,
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteMovie() {
        int row = tableMovies.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phim cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = (String) modelMovies.getValueAt(row, 0);
        dataStore.getMovieMap().remove(id);
        dataStore.getShowTimeMap().values().removeIf(st -> st.getMovieId().equals(id));
        dataStore.saveAllData();

        reloadMovieTable();
        reloadScheduleDropdowns();
        reloadMovieLobbyGrid();
        JOptionPane.showMessageDialog(this, "Đã xóa phim: " + id, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // 3. TAB 3: THỐNG KÊ & DASHBOARD KPI CARDS
    // =========================================================================
    private JPanel createReportTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        lblKpiRevenue = new JLabel("0 VNĐ", JLabel.CENTER);
        lblKpiTickets = new JLabel("0 vé", JLabel.CENTER);
        lblKpiFnB = new JLabel("0 VNĐ", JLabel.CENTER);
        lblKpiTopMovie = new JLabel("N/A", JLabel.CENTER);

        kpiContainer.add(createKpiCard("TỔNG DOANH THU", lblKpiRevenue, new Color(220, 38, 38)));
        kpiContainer.add(createKpiCard("VÉ ĐÃ BÁN", lblKpiTickets, new Color(37, 99, 235)));
        kpiContainer.add(createKpiCard("DOANH THU F&B", lblKpiFnB, new Color(217, 119, 6)));
        kpiContainer.add(createKpiCard("TOP PHIM ĂN KHÁCH", lblKpiTopMovie, new Color(22, 163, 74)));

        panel.add(kpiContainer, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.add(new JLabel("Lọc theo thời gian:"));
        cbFilterPeriod = new JComboBox<>(new String[]{"Tất cả thời gian", "Hôm nay", "7 ngày gần nhất"});
        cbFilterPeriod.addActionListener(e -> reloadReportDashboard());
        filterBar.add(cbFilterPeriod);
        centerPanel.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"Mã HĐ", "Phim", "Thu Ngân", "Khách Hàng", "Số Vé", "Ngày Tạo", "Thành Tiền"};
        modelInvoices = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tableInvoices = new JTable(modelInvoices);
        tableInvoices.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableInvoices.setRowHeight(28);

        // Căn lề và tỉ lệ cột cho bảng Hóa đơn chuẩn tài chính
        tableInvoices.setAutoCreateRowSorter(true);
        tableInvoices.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableInvoices.getColumnModel().getColumn(0).setMaxWidth(100);
        tableInvoices.getColumnModel().getColumn(1).setPreferredWidth(200);
        tableInvoices.getColumnModel().getColumn(2).setPreferredWidth(100);
        tableInvoices.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableInvoices.getColumnModel().getColumn(4).setPreferredWidth(70);
        tableInvoices.getColumnModel().getColumn(5).setPreferredWidth(130);
        tableInvoices.getColumnModel().getColumn(6).setPreferredWidth(130);

        DefaultTableCellRenderer centerInvRenderer = new DefaultTableCellRenderer();
        centerInvRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableInvoices.getColumnModel().getColumn(0).setCellRenderer(centerInvRenderer);
        tableInvoices.getColumnModel().getColumn(2).setCellRenderer(centerInvRenderer);
        tableInvoices.getColumnModel().getColumn(4).setCellRenderer(centerInvRenderer);
        tableInvoices.getColumnModel().getColumn(5).setCellRenderer(centerInvRenderer);

        tableInvoices.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.RIGHT);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setForeground(new Color(220, 38, 38));
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(tableInvoices);
        scroll.setBorder(BorderFactory.createTitledBorder("Lịch Sử Giao Dịch Hóa Đơn"));
        centerPanel.add(scroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnSortByRevenue = new JButton("Sắp xếp theo Tiền (Giảm dần)");
        btnSortByRevenue.addActionListener(e -> sortInvoicesByAmount());
        btnSortByDate = new JButton("Sắp xếp theo Ngày");
        btnSortByDate.addActionListener(e -> sortInvoicesByDate());
        btnExportReport = new JButton("Xuất Báo Cáo Ra File TXT");
        btnExportReport.addActionListener(e -> exportReportToFile());

        bottomTools.add(btnSortByRevenue);
        bottomTools.add(btnSortByDate);
        bottomTools.add(btnExportReport);
        panel.add(bottomTools, BorderLayout.SOUTH);

        reloadReportDashboard();
        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void reloadReportDashboard() {
        modelInvoices.setRowCount(0);
        double totalRev = 0;
        int totalTickets = 0;
        double totalFnB = 0;
        Map<String, Integer> movieTicketCount = new HashMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String period = (String) cbFilterPeriod.getSelectedItem();
        LocalDate today = LocalDate.now();

        for (Invoice inv : dataStore.getInvoiceMap().values()) {
            if (period != null && period.equals("Hôm nay")) {
                if (inv.getCreatedAt() == null || !inv.getCreatedAt().toLocalDate().isEqual(today)) continue;
            } else if (period != null && period.equals("7 ngày gần nhất")) {
                if (inv.getCreatedAt() == null || inv.getCreatedAt().toLocalDate().isBefore(today.minusDays(7))) continue;
            }

            totalRev += inv.getFinalAmount();
            int ticketCount = inv.getTickets() != null ? inv.getTickets().size() : 0;
            totalTickets += ticketCount;

            if (inv.getFoods() != null) {
                totalFnB += inv.getFoods().stream().mapToDouble(OrderItem::getTotalPrice).sum();
            }

            movieTicketCount.put(inv.getMovieTitle(),
                    movieTicketCount.getOrDefault(inv.getMovieTitle(), 0) + ticketCount);

            modelInvoices.addRow(new Object[]{
                    inv.getInvoiceId(),
                    inv.getMovieTitle(),
                    inv.getStaffUsername(),
                    inv.getCustomerName(),
                    ticketCount,
                    inv.getCreatedAt() != null ? inv.getCreatedAt().format(dtf) : "N/A",
                    String.format("%,.0f VNĐ", inv.getFinalAmount())
            });
        }

        lblKpiRevenue.setText(String.format("%,.0f VNĐ", totalRev));
        lblKpiTickets.setText(totalTickets + " vé");
        lblKpiFnB.setText(String.format("%,.0f VNĐ", totalFnB));

        String topMovie = "Chưa có";
        int maxTickets = 0;
        for (Map.Entry<String, Integer> entry : movieTicketCount.entrySet()) {
            if (entry.getValue() > maxTickets) {
                maxTickets = entry.getValue();
                topMovie = entry.getKey();
            }
        }
        lblKpiTopMovie.setText(topMovie);
    }

    private void sortInvoicesByAmount() {
        List<Invoice> list = new ArrayList<>(dataStore.getInvoiceMap().values());
        list.sort((a, b) -> Double.compare(b.getFinalAmount(), a.getFinalAmount()));
        populateInvoiceTable(list);
    }

    private void sortInvoicesByDate() {
        List<Invoice> list = new ArrayList<>(dataStore.getInvoiceMap().values());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        populateInvoiceTable(list);
    }

    private void populateInvoiceTable(List<Invoice> list) {
        modelInvoices.setRowCount(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Invoice inv : list) {
            modelInvoices.addRow(new Object[]{
                    inv.getInvoiceId(),
                    inv.getMovieTitle(),
                    inv.getStaffUsername(),
                    inv.getCustomerName(),
                    inv.getTickets() != null ? inv.getTickets().size() : 0,
                    inv.getCreatedAt() != null ? inv.getCreatedAt().format(dtf) : "N/A",
                    String.format("%,.0f VNĐ", inv.getFinalAmount())
            });
        }
    }

    private void exportReportToFile() {
        try {
            File reportFile = new File("BaoCaoDoanhThu_PTIT.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(reportFile, "UTF-8");
            pw.println("=======================================================");
            pw.println("      BÁO CÁO THỐNG KÊ DOANH THU RẠP CHIẾU PHIM PTIT   ");
            pw.println("             Thời gian xuất: " + LocalDateTime.now());
            pw.println("=======================================================");
            double total = 0;
            for (Invoice inv : dataStore.getInvoiceMap().values()) {
                total += inv.getFinalAmount();
                pw.printf("HĐ: %-10s | Phim: %-22s | Khách: %-15s | Tiền: %,12.0f VNĐ\n",
                        inv.getInvoiceId(), inv.getMovieTitle(), inv.getCustomerName(), inv.getFinalAmount());
            }
            pw.println("-------------------------------------------------------");
            pw.printf("TỔNG CỘNG DOANH THU: %,.0f VNĐ\n", total);
            pw.println("=======================================================");
            pw.close();

            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo thành công ra file: " + reportFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi xuất báo cáo: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }
}
