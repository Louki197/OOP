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
import vn.edu.ptit.cinema.util.PosterManager;
import vn.edu.ptit.cinema.view.auth.LoginFrame;
import vn.edu.ptit.cinema.view.component.CurvedScreenPanel;
import vn.edu.ptit.cinema.view.component.WrapLayout;
import vn.edu.ptit.cinema.view.staff.FnBDialog;
import vn.edu.ptit.cinema.view.staff.PaymentDialog;
import vn.edu.ptit.cinema.view.staff.BetaShowtimeDialog;
import vn.edu.ptit.cinema.view.staff.CustomerManagementDialog;
import vn.edu.ptit.cinema.view.staff.ShowtimeManagementDialog;
import vn.edu.ptit.cinema.view.staff.OnlineTicketPickupPanel;

import vn.edu.ptit.cinema.service.BookingRules;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

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

    private JLabel lblCurrentShowtimeInfo;
    private JButton btnChangeShowtime;

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
    // --- LOBBY LIVE SEARCH & FILTER ---
    private JTextField txtSearchLobby;
    private JComboBox<String> cbLobbyGenre;
    private JComboBox<String> cbLobbyRating;

    // --- FORM QUẢN LÝ PHIM (POSTER & UPDATE) ---
    private JLabel lblPosterPreview;
    private JButton btnBrowsePoster;
    private JButton btnUpdateMovie;
    private String selectedPosterPath = "";

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
    private vn.edu.ptit.cinema.view.component.RevenueChartsPanel reportCharts;
    private JButton btnSortByRevenue, btnSortByDate, btnExportReport;

    // --- TAB 4: LẤY VÉ VÀ IN VÉ ĐẶT ONLINE (KIOSK & POS) ---
    private OnlineTicketPickupPanel onlinePickupPanel;

    public MainFrame() {
        initUI();
        initSeatObserver();
    }

    private void initUI() {
        String roleStr = currentUser != null ? currentUser.getRole().getDisplayName() : "Khách";
        String nameStr = currentUser != null ? currentUser.getFullName() : "N/A";
        setTitle("HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM PTIT - " + nameStr + " | " + roleStr);
        PosterManager.preloadAll(dataStore.getMovieMap().values(), 205, 290);

        setSize(1360, 860);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // 1. Tab Bán vé & Sơ đồ rạp (Tại quầy)
        tabbedPane.addTab("BÁN VÉ & SƠ ĐỒ RẠP", createBookingCardContainer());

        // 2. Tab Lấy vé & In vé Online (Dành cho cả Thu ngân & Quản trị)
        onlinePickupPanel = new OnlineTicketPickupPanel(this);
        tabbedPane.addTab("LẤY VÉ ONLINE (KIOSK & POS)", onlinePickupPanel);

        if (SessionManager.getInstance().isAdmin()) {
            tabbedPane.addTab("QUẢN LÝ PHIM & LỊCH CHIẾU", createMovieManagementTab());
            tabbedPane.addTab("THỐNG KÊ & DOANH THU", createReportTab());
        }

        // LẮNG NGHE CHUYỂN TAB ĐỒNG BỘ DỮ LIỆU BẰNG TIÊU ĐỀ TAB (KHÔNG DÙNG INDEX CỨNG)
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                String tabTitle = tabbedPane.getTitleAt(selectedIndex);
                if (tabTitle.contains("BÁN VÉ")) {
                    refreshBookingTabState();
                } else if (tabTitle.contains("LẤY VÉ ONLINE")) {
                    if (onlinePickupPanel != null) {
                        onlinePickupPanel.reloadOnlineBookings();
                    }
                } else if (tabTitle.contains("THỐNG KÊ")) {
                    reloadReportDashboard();
                }
            }
        });

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(createTopHeaderBar(), BorderLayout.NORTH);
        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    
    private JPanel createTopHeaderBar() {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(8, 20, 8, 20)
        ));

        // Bên trái: Logo rạp
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        JLabel lblLogo = new JLabel("PTIT CINEMA", JLabel.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLogo.setForeground(new Color(220, 38, 38));

        JLabel lblSub = new JLabel("|  HỆ THỐNG QUẢN LÝ BÁN VÉ POS");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        leftPanel.add(lblLogo);
        leftPanel.add(lblSub);
        topBar.add(leftPanel, BorderLayout.WEST);

        // Ở giữa: Đồng hồ điện tử chạy từng giây
        JLabel lblClock = new JLabel("--/--/---- --:--:--");
        lblClock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblClock.setForeground(new Color(30, 41, 59));
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, e -> {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy - HH:mm:ss", Locale.forLanguageTag("vi-VN"));
            lblClock.setText(LocalDateTime.now().format(dtf));
        });
        clockTimer.start();
        JPanel centerClock = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerClock.setOpaque(false);
        centerClock.add(lblClock);
        topBar.add(centerClock, BorderLayout.CENTER);

        // Bên phải: Ca trực thu ngân & Nút Đăng Xuất
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        String nameStr = currentUser != null ? currentUser.getFullName() : "N/A";
        String roleStr = currentUser != null ? currentUser.getRole().getDisplayName() : "Khách";
        JLabel lblUser = new JLabel("Tài khoản: " + nameStr + " (" + roleStr + ")");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(71, 85, 105));

        JButton btnLogout = new JButton("Đăng Xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnLogout.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnLogout.addActionListener(e -> {
            vn.edu.ptit.cinema.pattern.observer.SeatEventBus.clear();
            SessionManager.getInstance().logout();
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        JButton btnManageCustomers = new JButton("Thẻ Hội Viên");
        btnManageCustomers.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnManageCustomers.setBackground(new Color(241, 245, 249));
        btnManageCustomers.setForeground(new Color(37, 99, 235));
        btnManageCustomers.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnManageCustomers.addActionListener(e -> new CustomerManagementDialog(this).setVisible(true));

        rightPanel.add(btnManageCustomers);
        rightPanel.add(lblUser);
        rightPanel.add(btnLogout);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuSystem = new JMenu("Hệ thống");
        menuSystem.setMnemonic('H');

        JMenuItem miCustomers = new JMenuItem("Quản lý Thẻ Tích Điểm & Hội Viên (Ctrl+M)");
        miCustomers.setAccelerator(KeyStroke.getKeyStroke("control M"));
        miCustomers.addActionListener(e -> new CustomerManagementDialog(this).setVisible(true));
        menuSystem.add(miCustomers);

        JMenuItem miPickup = new JMenuItem("Lấy vé & In vé Online (Ctrl+O)");
        miPickup.setAccelerator(KeyStroke.getKeyStroke("control O"));
        miPickup.addActionListener(e -> {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).contains("LẤY VÉ ONLINE")) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        });
        menuSystem.add(miPickup);

        JMenuItem miShowtimes = new JMenuItem("Quản lý & Chỉnh Sửa Lịch Chiếu (Ctrl+L)");
        miShowtimes.setAccelerator(KeyStroke.getKeyStroke("control L"));
        miShowtimes.addActionListener(e -> {
            if (SessionManager.getInstance().isAdmin()) {
                new ShowtimeManagementDialog(this, () -> {
                    reloadScheduleDropdowns();
                    reloadMovieLobbyGrid();
                }).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Chức năng chỉ dành riêng cho Quản trị viên (Admin)!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        menuSystem.add(miShowtimes);

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

        // Tiêu đề Sảnh + Thanh Tìm Kiếm Realtime
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBox.setOpaque(true);
        titleBox.setBackground(new Color(248, 250, 252));
        JLabel lblTitle = new JLabel("DANH SÁCH PHIM ĐANG CHIẾU TẠI RẠP PTIT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));
        titleBox.add(lblTitle);

        // Thanh công cụ tìm kiếm và lọc phim trực tiếp (Live Search)
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);

        txtSearchLobby = new JTextField(20);
        txtSearchLobby.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearchLobby.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm tên phim, đạo diễn...");
        txtSearchLobby.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        txtSearchLobby.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { reloadMovieLobbyGrid(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { reloadMovieLobbyGrid(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { reloadMovieLobbyGrid(); }
        });

        cbLobbyGenre = new JComboBox<>(new String[]{"Tất cả thể loại", "Hành Động", "Kinh Dị, Bí Ẩn", "Lịch Sử / Chiến Tranh", "Khoa Học Viễn Tưởng", "Hài Hước", "Hoạt Hình"});
        cbLobbyGenre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbLobbyGenre.addActionListener(e -> reloadMovieLobbyGrid());

        cbLobbyRating = new JComboBox<>(new String[]{"Tất cả độ tuổi", "P", "C13", "C16", "C18"});
        cbLobbyRating.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbLobbyRating.addActionListener(e -> reloadMovieLobbyGrid());

        searchBar.add(new JLabel("Tìm kiếm:"));
        searchBar.add(txtSearchLobby);
        searchBar.add(Box.createHorizontalStrut(5));
        searchBar.add(new JLabel("Thể loại:"));
        searchBar.add(cbLobbyGenre);
        searchBar.add(Box.createHorizontalStrut(5));
        searchBar.add(new JLabel("Độ tuổi:"));
        searchBar.add(cbLobbyRating);

        header.setOpaque(true);
        header.setBackground(new Color(248, 250, 252));
        titleBox.setOpaque(true);
        titleBox.setBackground(new Color(248, 250, 252));
        searchBar.setOpaque(true);
        searchBar.setBackground(new Color(248, 250, 252));

        header.add(titleBox, BorderLayout.NORTH);
        header.add(searchBar, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        // Lưới danh sách phim bố trí 4 cột chuẩn tỷ lệ
        movieGridPanel = new JPanel(new GridLayout(0, 4, 18, 20));
        movieGridPanel.setOpaque(true);
        movieGridPanel.setBackground(new Color(248, 250, 252));

        JScrollPane scrollPane = new JScrollPane(movieGridPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(new Color(248, 250, 252));
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252));
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        reloadMovieLobbyGrid();
        return panel;
    }

    private void reloadMovieLobbyGrid() {
        if (movieGridPanel == null) return;
        movieGridPanel.removeAll();

        String keyword = txtSearchLobby != null ? txtSearchLobby.getText().trim().toLowerCase() : "";
        String genreFilter = cbLobbyGenre != null ? (String) cbLobbyGenre.getSelectedItem() : "Tất cả thể loại";
        String ratingFilter = cbLobbyRating != null ? (String) cbLobbyRating.getSelectedItem() : "Tất cả độ tuổi";

        boolean hasFilterGenre = genreFilter != null && !genreFilter.equals("Tất cả thể loại");
        boolean hasFilterRating = ratingFilter != null && !ratingFilter.equals("Tất cả độ tuổi");

        int matchedCount = 0;
        for (Movie m : dataStore.getMovieMap().values()) {
            // Kiểm tra bộ lọc
            if (!keyword.isEmpty()) {
                boolean matchTitle = m.getTitle() != null && m.getTitle().toLowerCase().contains(keyword);
                boolean matchDirector = m.getDirector() != null && m.getDirector().toLowerCase().contains(keyword);
                boolean matchGenre = m.getGenre() != null && m.getGenre().toLowerCase().contains(keyword);
                if (!matchTitle && !matchDirector && !matchGenre) continue;
            }
            if (hasFilterGenre && (m.getGenre() == null || !m.getGenre().toLowerCase().contains(genreFilter.toLowerCase()))) {
                continue;
            }
            if (hasFilterRating && (m.getAgeRating() == null || !m.getAgeRating().equalsIgnoreCase(ratingFilter))) {
                continue;
            }

            matchedCount++;
            // DỰNG THẺ PHIM VERTICAL CHUẨN CGV / BETA CINEMAS
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setOpaque(true);
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(10, 10, 12, 10)
            ));

            // 1. POSTER DỌC BO GÓC 2:3 VỚI BADGE ĐỘ TUỔI GHIM TRÊN ĐẦU (CĂN GIỮA CHUẨN CGV)
            JPanel posterContainer = createPosterPanel(m, 205, 290);
            JPanel posterWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            posterWrap.setOpaque(true);
            posterWrap.setBackground(Color.WHITE);
            posterWrap.add(posterContainer);
            card.add(posterWrap, BorderLayout.NORTH);

            // 2. KHỐI THÔNG TIN DƯỚI POSTER
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(true);
            infoPanel.setBackground(Color.WHITE);
            infoPanel.setBorder(new EmptyBorder(6, 4, 8, 4));

            JLabel lblName = new JLabel(m.getTitle());
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblName.setForeground(new Color(15, 23, 42));
            lblName.setToolTipText(m.getTitle());
            lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblGenre = new JLabel("" + (m.getGenre() != null ? m.getGenre() : "Đang cập nhật"));
            lblGenre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblGenre.setForeground(new Color(71, 85, 105));
            lblGenre.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblDur = new JLabel("" + m.getDurationMinutes() + " phút");
            lblDur.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDur.setForeground(new Color(71, 85, 105));
            lblDur.setAlignmentX(Component.LEFT_ALIGNMENT);

            long showCount = dataStore.getShowTimeMap().values().stream()
                    .filter(st -> st.getMovieId().equals(m.getId()))
                    .count();
            JLabel lblShows = new JLabel("" + showCount + " suất chiếu");
            lblShows.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblShows.setForeground(new Color(37, 99, 235));
            lblShows.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(lblName);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(lblGenre);
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(lblDur);
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(lblShows);
            card.add(infoPanel, BorderLayout.CENTER);

            // 3. NÚT CHỌN PHIM / MUA VÉ
            JButton btnSelect = new JButton("CHỌN PHIM NÀY");
            btnSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnSelect.setBackground(new Color(220, 38, 38));
            btnSelect.setForeground(Color.WHITE);
            btnSelect.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 6,12,6,12;");
            btnSelect.addActionListener(e -> openBetaShowtimeDialog(m));

            card.add(btnSelect, BorderLayout.SOUTH);

            // Bấm bất kỳ đâu trên thẻ phim hoặc poster đều mở ngay Popup Suất Chiếu Beta
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            posterContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter cardClickListener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(2, 132, 199), 2),
                            new EmptyBorder(9, 9, 11, 9)
                    ));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                            new EmptyBorder(10, 10, 12, 10)
                    ));
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    openBetaShowtimeDialog(m);
                }
            };
            card.addMouseListener(cardClickListener);
            posterContainer.addMouseListener(cardClickListener);
            infoPanel.addMouseListener(cardClickListener);

            movieGridPanel.add(card);
        }

        if (matchedCount == 0) {
            JPanel emptyNotice = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
            emptyNotice.setOpaque(false);
            JLabel lblEmpty = new JLabel("Không tìm thấy bộ phim nào phù hợp với bộ lọc!");
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            lblEmpty.setForeground(Color.GRAY);
            emptyNotice.add(lblEmpty);
            movieGridPanel.add(emptyNotice);
        }

        movieGridPanel.revalidate();
        movieGridPanel.repaint();
    }

    /**
     * VẼ POSTER ẢNH TỶ LỆ CHUẨN 2:3 KHÔNG GIẬT LAG EDT
     * Sử dụng PosterManager In-Memory RAM Cache & Center-crop
     */
    private JPanel createPosterPanel(Movie m, int width, int height) {
        JPanel posterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                PosterManager.drawPosterOrPlaceholder(g2, m, getWidth(), getHeight());
                g2.dispose();
            }
        };

        posterPanel.setOpaque(true);
        posterPanel.setBackground(Color.WHITE);
        posterPanel.setPreferredSize(new Dimension(width, height));
        posterPanel.setMinimumSize(new Dimension(width, height));
        posterPanel.setMaximumSize(new Dimension(width, height));
        posterPanel.setLayout(null); // Cho phép đặt badge tọa độ tuyệt đối

        // Badge Độ Tuổi ghim nổi bật ở góc trên bên trái của ảnh poster (chuẩn CGV)
        String rating = m.getAgeRating() != null ? m.getAgeRating() : "P";
        JLabel lblBadge = new JLabel(rating, JLabel.CENTER);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBadge.setOpaque(true);
        lblBadge.setBounds(10, 10, 48, 26);
        setAgeBadgeStyle(lblBadge, rating);
        posterPanel.add(lblBadge);

        return posterPanel;
    }

    private void setAgeBadgeStyle(JLabel lbl, String rating) {
        if (rating == null) rating = "P";
        lbl.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        switch (rating.toUpperCase()) {
            case "C18":
            case "T18":
                // Màu hồng đỏ rực rỡ như ảnh CGV
                lbl.setBackground(new Color(244, 63, 94));
                lbl.setForeground(Color.WHITE);
                lbl.setBorder(BorderFactory.createLineBorder(new Color(225, 29, 72), 1));
                break;
            case "C16":
                // Màu cam
                lbl.setBackground(new Color(249, 115, 22));
                lbl.setForeground(Color.WHITE);
                lbl.setBorder(BorderFactory.createLineBorder(new Color(234, 88, 12), 1));
                break;
            case "C13":
                // Màu vàng hổ phách
                lbl.setBackground(new Color(234, 179, 8));
                lbl.setForeground(Color.BLACK);
                lbl.setBorder(BorderFactory.createLineBorder(new Color(202, 138, 4), 1));
                break;
            default:
                // P: Màu xanh lá an toàn
                lbl.setBackground(new Color(34, 197, 94));
                lbl.setForeground(Color.WHITE);
                lbl.setBorder(BorderFactory.createLineBorder(new Color(22, 163, 74), 1));
                break;
        }
    }

    /**
     * MỞ POPUP CHỌN SUẤT CHIẾU CHUẨN BETA CINEMAS
     */
    private void openBetaShowtimeDialog(Movie m) {
        if (m == null) return;
        this.selectedMovieForBooking = m;
        BetaShowtimeDialog dlg = new BetaShowtimeDialog(this, m, st -> {
            selectShowtimeAndOpenSeatMap(m, st);
        });
        dlg.setVisible(true);
    }

    /**
     * CHUYỂN THẲNG VÀO SƠ ĐỒ GHẾ CỦA SUẤT CHIẾU ĐÃ CHỌN TỪ POPUP BETA
     */
    public void selectShowtimeAndOpenSeatMap(Movie m, ShowTime st) {
        if (m == null || st == null) return;
        if (!BookingRules.canSell(st, LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Suất chiếu đã bắt đầu. Vui lòng chọn suất khác.");
            return;
        }
        if (currentShowTime != st) releaseSelectedPendingSeats();
        this.selectedMovieForBooking = m;
        this.currentShowTime = st;

        lblCurrentMovieTitle.setText(m.getTitle());
        lblCurrentMovieBadge.setText(m.getAgeRating());
        setAgeBadgeStyle(lblCurrentMovieBadge, m.getAgeRating());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM");
        Room room = dataStore.getRoomMap().get(st.getRoomId());
        String rName = room != null ? room.getName() : st.getRoomId();
        if (lblCurrentShowtimeInfo != null) {
            lblCurrentShowtimeInfo.setText(String.format("Suất chiếu: %s (%s)", st.getStartTime().format(dtf), rName));
        }

        restoreUserHeldSeatsIfAny();
        renderSeatMatrixWithAxesAndAisles();
        bookingCardLayout.show(bookingCardContainer, CARD_SEATMAP);
    }



    /**
     * MÀN HÌNH 2: PHÒNG CHIẾU & SƠ ĐỒ GHẾ
     */
    private JPanel createSeatMapPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(12, 15, 12, 15));

        // --- 1.1 Thanh Điều Hướng Trên 2 Tầng: Tầng 1 Tên Phim & Chú Thích Ghế; Tầng 2 Suất Chiếu & Đổi Suất ---
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        topContainer.setBackground(Color.WHITE);

        // TẦNG 1: [Quay Lại] [Tên Phim] [Độ Tuổi] <--- KHÔNG GIAN THOÁNG ---> [CHÚ THÍCH HẠNG GHẾ & TRẠNG THÁI Ở TRÊN ĐỈNH]
        JPanel row1 = new JPanel(new BorderLayout(15, 0));
        row1.setOpaque(false);

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1Left.setOpaque(false);

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
        row1Left.add(btnBackToLobby);

        lblCurrentMovieTitle = new JLabel("Tên Phim Đang Chọn");
        lblCurrentMovieTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCurrentMovieTitle.setForeground(new Color(15, 23, 42));
        row1Left.add(lblCurrentMovieTitle);

        lblCurrentMovieBadge = new JLabel("P", JLabel.CENTER);
        lblCurrentMovieBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCurrentMovieBadge.setOpaque(true);
        lblCurrentMovieBadge.setPreferredSize(new Dimension(36, 22));
        row1Left.add(lblCurrentMovieBadge);

        row1.add(row1Left, BorderLayout.WEST);

        // Chú thích 2 tầng nằm cố định trên đỉnh, không bao giờ bị đè lấn
        JPanel legendBox = new JPanel(new GridLayout(2, 1, 0, 3));
        legendBox.setOpaque(false);

        JPanel rowTier = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rowTier.setOpaque(false);
        rowTier.add(new JLabel("Hạng ghế:"));
        rowTier.add(createLegendBadge("Ghế Thường", new Color(34, 197, 94), null));
        rowTier.add(createLegendBadge("Ghế VIP (Viền vàng)", new Color(34, 197, 94), new Color(245, 158, 11)));
        rowTier.add(createLegendBadge("Sweetbox Đôi", new Color(236, 72, 153), new Color(219, 39, 119)));

        JPanel rowStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rowStatus.setOpaque(false);
        rowStatus.add(new JLabel("Trạng thái:"));
        rowStatus.add(createLegendBadge("Trống", new Color(34, 197, 94), null));
        rowStatus.add(createLegendBadge("Đang chọn", new Color(59, 130, 246), null));
        rowStatus.add(createLegendBadge("Đang giữ (5p)", new Color(234, 179, 8), null));
        rowStatus.add(createLegendBadge("Đã bán", new Color(220, 38, 38), null));

        legendBox.add(rowTier);
        legendBox.add(rowStatus);
        row1.add(legendBox, BorderLayout.EAST);

        topContainer.add(row1);
        topContainer.add(Box.createVerticalStrut(6));

        // Đường phân cách mỏng trang nhã
        JSeparator headerSep = new JSeparator();
        headerSep.setForeground(new Color(241, 245, 249));
        headerSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        topContainer.add(headerSep);
        topContainer.add(Box.createVerticalStrut(6));

        // TẦNG 2: [Suất Chiếu Banner] [Đổi Suất Chiếu] (Độc lập, rộng rãi, không đè lên bất kỳ chú thích nào)
        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);

        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2Left.setOpaque(false);



        lblCurrentShowtimeInfo = new JLabel("Suất chiếu: --:-- (Phòng --)");
        lblCurrentShowtimeInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCurrentShowtimeInfo.setForeground(new Color(2, 132, 199));
        lblCurrentShowtimeInfo.setBackground(new Color(240, 249, 255));
        lblCurrentShowtimeInfo.setOpaque(true);
        lblCurrentShowtimeInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 230, 253), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        lblCurrentShowtimeInfo.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        btnChangeShowtime = new JButton("Đổi Suất Chiếu");
        btnChangeShowtime.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnChangeShowtime.setBackground(new Color(241, 245, 249));
        btnChangeShowtime.setForeground(new Color(2, 132, 199));
        btnChangeShowtime.putClientProperty(FlatClientProperties.STYLE, "arc: 6;");
        btnChangeShowtime.addActionListener(e -> {
            if (selectedMovieForBooking != null) {
                openBetaShowtimeDialog(selectedMovieForBooking);
            }
        });

        row2Left.add(lblCurrentShowtimeInfo);
        row2Left.add(btnChangeShowtime);
        row2.add(row2Left, BorderLayout.WEST);

        JLabel lblPricingHint = new JLabel("Chính sách giá: Ghế VIP +20% | Sweetbox x2.0");
        lblPricingHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblPricingHint.setForeground(new Color(100, 116, 139));
        row2.add(lblPricingHint, BorderLayout.EAST);

        topContainer.add(row2);
        panel.add(topContainer, BorderLayout.NORTH);

        // --- 1.2 Không gian rạp: Màn hình cong 3D + Trục tọa độ + Lối đi ---
        JPanel theaterCenterPanel = new JPanel(new BorderLayout(0, 6));
        theaterCenterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        theaterCenterPanel.setBackground(Color.WHITE);

        theaterCenterPanel.add(new CurvedScreenPanel(), BorderLayout.NORTH);

        seatMatrixContainer = new JPanel(new GridBagLayout());
        seatMatrixContainer.setOpaque(false);

        JScrollPane scrollSeats = new JScrollPane(seatMatrixContainer);
        scrollSeats.setBorder(null);
        scrollSeats.setOpaque(false);
        scrollSeats.getViewport().setOpaque(false);
        scrollSeats.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollSeats.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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

        // Sử dụng WrapLayout để các ghế được chọn tự động rớt xuống dòng khi hết chỗ ngang
        chipsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
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
        scrollChips.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollChips.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
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
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.BOTH;

        int rows = currentShowTime.getSeats().values().stream().mapToInt(Seat::getRowIndex).max().orElse(-1) + 1;
        int cols = currentShowTime.getSeats().values().stream().mapToInt(Seat::getColIndex).max().orElse(-1) + 1;
        PricingStrategy displayedPricing = PricingStrategies.determineStrategy(currentShowTime.getStartTime(), false);

        // 1. Hàng số thứ tự cột trên đỉnh (1 ... 10)
        gbc.gridy = 0;
        gbc.gridx = 0;
        seatMatrixContainer.add(new JLabel(""), gbc);

        int gridX = 1;
        for (int c = 1; c <= cols; c++) {
            if (c == 3 || c == 9) {
                gbc.gridx = gridX++;
                seatMatrixContainer.add(Box.createHorizontalStrut(18), gbc);
            }
            gbc.gridx = gridX++;
            JLabel lblCol = new JLabel(String.valueOf(c), JLabel.CENTER);
            lblCol.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblCol.setForeground(new Color(100, 116, 139));
            lblCol.setPreferredSize(new Dimension(48, 18));
            seatMatrixContainer.add(lblCol, gbc);
        }
        gbc.gridx = gridX;
        seatMatrixContainer.add(new JLabel(""), gbc);

        // 2. Các hàng ghế (A -> H)
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            boolean isSweetboxRow = (r >= rows - 2);
            boolean isVipRow = (r >= 2 && r < rows - 2);

            gbc.gridy = r + 1;

            // Nhãn chữ cái bên trái (A-H)
            gbc.gridx = 0;
            JLabel lblRowLeft = new JLabel(String.valueOf(rowChar), JLabel.CENTER);
            lblRowLeft.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblRowLeft.setForeground(new Color(71, 85, 105));
            lblRowLeft.setPreferredSize(new Dimension(26, 38));
            seatMatrixContainer.add(lblRowLeft, gbc);

            gridX = 1;

            if (isSweetboxRow) {
                // SWEETBOX: ĐÔI 1-2, ĐÔI 3-4...
                for (int c1 = 1; c1 < cols; c1 += 2) {
                    int c2 = c1 + 1;

                    if (c1 == 3 || c1 == 9) {
                        gbc.gridx = gridX++;
                        gbc.gridwidth = 1;
                        seatMatrixContainer.add(Box.createHorizontalStrut(18), gbc);
                    }

                    String code1 = String.format("%c%02d", rowChar, c1);
                    String code2 = String.format("%c%02d", rowChar, c2);
                    Seat s1 = currentShowTime.getSeats().get(code1);
                    Seat s2 = currentShowTime.getSeats().get(code2);
                    if (s1 == null || s2 == null) continue;

                    JButton btnSweetbox = new JButton(String.format("ĐÔI %d-%d", c1, c2));
                    btnSweetbox.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnSweetbox.setPreferredSize(new Dimension(100, 38));
                    btnSweetbox.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

                    updateSweetboxAppearance(btnSweetbox, s1, s2);
                    double pairPrice = displayedPricing.calculatePrice(currentShowTime.getBasePrice(), s1, currentShowTime.getStartTime())
                            + displayedPricing.calculatePrice(currentShowTime.getBasePrice(), s2, currentShowTime.getStartTime());
                    btnSweetbox.setToolTipText(String.format("Sweetbox %s - %s: 2 chỗ, cả cặp %,.0f VNĐ (%s)",
                            code1, code2, pairPrice, displayedPricing.getStrategyName()));

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
                        seatMatrixContainer.add(Box.createHorizontalStrut(18), gbc);
                    }

                    String code = String.format("%c%02d", rowChar, c);
                    Seat seat = currentShowTime.getSeats().get(code);
                    if (seat == null) continue;

                    JButton btnSeat = new JButton(String.valueOf(c));
                    btnSeat.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnSeat.setPreferredSize(new Dimension(48, 38));
                    btnSeat.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

                    updateSeatButtonAppearance(btnSeat, seat, isVipRow);

                    double price = displayedPricing.calculatePrice(currentShowTime.getBasePrice(), seat, currentShowTime.getStartTime());
                    btnSeat.setToolTipText(String.format("Ghế %s %s - Giá: %,.0f VNĐ",
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
            lblRowRight.setPreferredSize(new Dimension(26, 38));
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

    private long selectionHoldStart() {
        return selectedPendingSeats.stream().map(currentShowTime.getSeats()::get)
                .filter(java.util.Objects::nonNull).mapToLong(Seat::getHoldTimestamp)
                .filter(t -> t > 0).min().orElse(System.currentTimeMillis());
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
            seat.setHoldTimestamp(selectionHoldStart());
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
            s1.setHeldByUserId(null);
            s2.setHeldByUserId(null);
        } else {
            selectedPendingSeats.add(code1);
            selectedPendingSeats.add(code2);
            long now = selectionHoldStart();
            s1.setStatus(SeatStatus.PENDING);
            s2.setStatus(SeatStatus.PENDING);
            s1.setHoldTimestamp(now);
            s2.setHoldTimestamp(now);
            String holder = currentUser != null ? currentUser.getUsername() : "staff";
            s1.setHeldByUserId(holder);
            s2.setHeldByUserId(holder);
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
        if (seat != null && seat.getType() == SeatType.SWEETBOX) {
            int partnerCol = seat.getColIndex() % 2 == 0 ? seat.getColIndex() + 1 : seat.getColIndex() - 1;
            for (Seat member : currentShowTime.getSeats().values()) {
                if (member.getRowIndex() == seat.getRowIndex() && (member.getColIndex() == seat.getColIndex() || member.getColIndex() == partnerCol)
                        && selectedPendingSeats.remove(member.getCode()) && member.getStatus() == SeatStatus.PENDING) {
                    member.setStatus(SeatStatus.AVAILABLE);
                    member.setHoldTimestamp(0);
                    member.setHeldByUserId(null);
                }
            }
            renderSeatMatrixWithAxesAndAisles();
            refreshChipsAndTotals();
            return;
        }
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

        if (paymentDialog.isPaymentSuccessful() || !BookingRules.validHold(currentShowTime, selectedPendingSeats, System.currentTimeMillis())) {
            selectedPendingSeats.clear();
            renderSeatMatrixWithAxesAndAisles();
            refreshChipsAndTotals();
            reloadReportDashboard();
            if (onlinePickupPanel != null) {
                onlinePickupPanel.reloadOnlineBookings();
            }
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
                long deadline = selectionHoldStart() + DataStore.SEAT_HOLD_TIMEOUT_MILLIS;
                remainingSeconds = BookingRules.validHold(currentShowTime, selectedPendingSeats, System.currentTimeMillis())
                        ? (int) Math.max(0, (deadline - System.currentTimeMillis() + 999) / 1000) : 0;
                int min = Math.max(0, remainingSeconds / 60);
                int sec = Math.max(0, remainingSeconds % 60);
                lblCountdownTimer.setText(String.format("Thời gian giữ ghế: %02d:%02d", min, sec));

                if (remainingSeconds <= 0) {
                    releaseSelectedPendingSeats();
                    remainingSeconds = 300;
                    boolean paymentOpen = Arrays.stream(getOwnedWindows()).anyMatch(w -> w instanceof PaymentDialog && w.isShowing());
                    if (!paymentOpen) JOptionPane.showMessageDialog(this, "Đã hết 5 phút giữ chỗ! Ghế đã tự động giải phóng.", "Hết giờ", JOptionPane.WARNING_MESSAGE);
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
                renderSeatMatrixWithAxesAndAisles();
                refreshChipsAndTotals();
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

        JButton btnAddNewMovieToolbar = new JButton("+ THÊM PHIM MỚI");
        btnAddNewMovieToolbar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddNewMovieToolbar.setBackground(new Color(22, 163, 74));
        btnAddNewMovieToolbar.setForeground(Color.WHITE);
        btnAddNewMovieToolbar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,12,4,12;");
        btnAddNewMovieToolbar.addActionListener(e -> {
            tableMovies.clearSelection();
            resetMovieForm();
            txtMovieTitle.requestFocusInWindow();
        });
        leftSearch.add(Box.createHorizontalStrut(10));
        leftSearch.add(btnAddNewMovieToolbar);

        JButton btnManageShowtimesToolbar = new JButton("QUẢN LÝ LỊCH CHIẾU");
        btnManageShowtimesToolbar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnManageShowtimesToolbar.setBackground(new Color(37, 99, 235));
        btnManageShowtimesToolbar.setForeground(Color.WHITE);
        btnManageShowtimesToolbar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,12,4,12;");
        btnManageShowtimesToolbar.addActionListener(e -> {
            new ShowtimeManagementDialog(this, () -> {
                reloadScheduleDropdowns();
                reloadMovieLobbyGrid();
            }).setVisible(true);
        });
        leftSearch.add(Box.createHorizontalStrut(8));
        leftSearch.add(btnManageShowtimesToolbar);

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
                        txtMovieId.setEditable(false);
                        txtMovieId.setBackground(new Color(241, 245, 249));
                        txtMovieTitle.setText(m.getTitle());
                        txtMovieGenre.setText(m.getGenre());
                        txtMovieDuration.setText(String.valueOf(m.getDurationMinutes()));
                        txtMovieDirector.setText(m.getDirector());
                        cbAgeRating.setSelectedItem(m.getAgeRating());
                        selectedPosterPath = m.getPosterPath();
                        updatePosterPreview(selectedPosterPath);
                        btnAddMovie.setEnabled(false);
                        btnUpdateMovie.setEnabled(true);
                        btnDeleteMovie.setEnabled(true);
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

        // Form 1: Quản Lý Phim (Thêm mới, Cập nhật, Upload Poster chuẩn JFileChooser)
        JPanel pnlAddMovie = new JPanel(new BorderLayout(12, 8));
        pnlAddMovie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("1. Thông Tin & Poster Phim"),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pnlAddMovie.setBackground(Color.WHITE);

        // Khung bên trái: Poster Preview (105x145px) + Nút Chọn Ảnh
        JPanel posterBox = new JPanel(new BorderLayout(0, 6));
        posterBox.setOpaque(false);
        lblPosterPreview = new JLabel("Chưa có ảnh", JLabel.CENTER);
        lblPosterPreview.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPosterPreview.setForeground(Color.GRAY);
        lblPosterPreview.setPreferredSize(new Dimension(105, 140));
        lblPosterPreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        lblPosterPreview.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        btnBrowsePoster = new JButton("Chọn Ảnh...");
        btnBrowsePoster.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnBrowsePoster.putClientProperty(FlatClientProperties.STYLE, "arc: 6;");
        btnBrowsePoster.addActionListener(e -> handleBrowsePoster());

        posterBox.add(lblPosterPreview, BorderLayout.CENTER);
        posterBox.add(btnBrowsePoster, BorderLayout.SOUTH);
        pnlAddMovie.add(posterBox, BorderLayout.WEST);

        // Khung bên phải: Các trường nhập liệu
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);

        txtMovieId = new JTextField("MOV0" + (dataStore.getMovieMap().size() + 1));
        txtMovieTitle = new JTextField();
        txtMovieGenre = new JTextField("Hành Động");
        txtMovieDuration = new JTextField("120");
        txtMovieDirector = new JTextField("Christopher Nolan");
        cbAgeRating = new JComboBox<>(new String[]{"P", "C13", "C16", "C18"});

        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.insets = new Insets(3, 4, 3, 4);
        gbc1.fill = GridBagConstraints.HORIZONTAL;

        // Dòng 0: Mã & Tên
        gbc1.gridx = 0; gbc1.gridy = 0; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Mã:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 0; gbc1.weightx = 0.35;
        fieldsPanel.add(txtMovieId, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 0; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Tên Phim:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 0; gbc1.weightx = 0.65;
        fieldsPanel.add(txtMovieTitle, gbc1);

        // Dòng 1: Thể loại & Thời lượng
        gbc1.gridx = 0; gbc1.gridy = 1; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Thể Loại:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 1; gbc1.weightx = 0.35;
        fieldsPanel.add(txtMovieGenre, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 1; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Thời Lượng:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 1; gbc1.weightx = 0.65;
        fieldsPanel.add(txtMovieDuration, gbc1);

        // Dòng 2: Độ tuổi & Đạo diễn
        gbc1.gridx = 0; gbc1.gridy = 2; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Độ Tuổi:"), gbc1);
        gbc1.gridx = 1; gbc1.gridy = 2; gbc1.weightx = 0.35;
        fieldsPanel.add(cbAgeRating, gbc1);
        gbc1.gridx = 2; gbc1.gridy = 2; gbc1.weightx = 0;
        fieldsPanel.add(new JLabel("Đạo Diễn:"), gbc1);
        gbc1.gridx = 3; gbc1.gridy = 2; gbc1.weightx = 0.65;
        fieldsPanel.add(txtMovieDirector, gbc1);

        pnlAddMovie.add(fieldsPanel, BorderLayout.CENTER);

        // Dưới cùng: Cụm nút bấm logic chuẩn CRUD
        btnAddMovie = new JButton("+ Lưu Phim Mới");
        btnAddMovie.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddMovie.setBackground(new Color(22, 163, 74));
        btnAddMovie.setForeground(Color.WHITE);
        btnAddMovie.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnAddMovie.addActionListener(e -> handleAddMovie());

        btnUpdateMovie = new JButton("Lưu Cập Nhật");
        btnUpdateMovie.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdateMovie.setBackground(new Color(37, 99, 235));
        btnUpdateMovie.setForeground(Color.WHITE);
        btnUpdateMovie.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnUpdateMovie.setEnabled(false);
        btnUpdateMovie.addActionListener(e -> handleUpdateMovie());

        btnDeleteMovie = new JButton("Xóa Phim");
        btnDeleteMovie.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteMovie.setForeground(new Color(220, 38, 38));
        btnDeleteMovie.putClientProperty(FlatClientProperties.STYLE, "arc: 8; borderWidth: 1;");
        btnDeleteMovie.setEnabled(false);
        btnDeleteMovie.addActionListener(e -> handleDeleteMovie());

        JButton btnClearForm = new JButton("Nhập Mới");
        btnClearForm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClearForm.setBackground(new Color(241, 245, 249));
        btnClearForm.setForeground(new Color(30, 41, 59));
        btnClearForm.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnClearForm.addActionListener(e -> {
            tableMovies.clearSelection();
            resetMovieForm();
            txtMovieTitle.requestFocusInWindow();
        });

        JPanel btnBoxMovie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBoxMovie.setOpaque(false);
        btnBoxMovie.add(btnClearForm);
        btnBoxMovie.add(btnDeleteMovie);
        btnBoxMovie.add(btnUpdateMovie);
        btnBoxMovie.add(btnAddMovie);
        pnlAddMovie.add(btnBoxMovie, BorderLayout.SOUTH);

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

        JPanel btnBoxSchedule = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBoxSchedule.setOpaque(false);

        JButton btnOpenShowtimeDlg = new JButton("Danh Sách & Sửa Lịch Chiếu");
        btnOpenShowtimeDlg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenShowtimeDlg.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnOpenShowtimeDlg.addActionListener(e -> {
            new ShowtimeManagementDialog(this, () -> {
                reloadScheduleDropdowns();
                reloadMovieLobbyGrid();
            }).setVisible(true);
        });

        btnBoxSchedule.add(btnOpenShowtimeDlg);
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
            cbScheduleMovie.addItem(String.format("%s | %s", m.getId(), m.getTitle()));
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

    private void handleBrowsePoster() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Chọn Ảnh Poster Phim");
        chooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (*.jpg, *.png, *.jpeg, *.webp)", "jpg", "png", "jpeg", "webp"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File src = chooser.getSelectedFile();
            File posterDir = new File("posters");
            if (!posterDir.exists()) posterDir.mkdirs();

            String ext = ".jpg";
            int dotIdx = src.getName().lastIndexOf('.');
            if (dotIdx > 0) ext = src.getName().substring(dotIdx);

            String destName = "poster_" + System.currentTimeMillis() + ext;
            File dest = new File(posterDir, destName);
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                selectedPosterPath = "posters/" + destName;
                updatePosterPreview(selectedPosterPath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể lưu ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updatePosterPreview(String path) {
        if (lblPosterPreview == null) return;
        if (path != null && !path.trim().isEmpty()) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    java.awt.image.BufferedImage scaled = PosterManager.getPoster(path, 105, 140);
                    if (scaled != null) {
                        lblPosterPreview.setIcon(new ImageIcon(scaled));
                        lblPosterPreview.setText("");
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
        lblPosterPreview.setIcon(null);
        lblPosterPreview.setText("Chưa có ảnh");
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

        if (dur <= 0 || dataStore.getMovieMap().containsKey(id)) {
            JOptionPane.showMessageDialog(this, "Thời lượng phải lớn hơn 0 và mã phim không được trùng.");
            return;
        }
        String rating = (String) cbAgeRating.getSelectedItem();
        Movie m = new Movie(id, title, genre, dur, LocalDate.now(), selectedPosterPath, "Phim mới", txtMovieDirector.getText(), rating);
        dataStore.getMovieMap().put(id, m);
        dataStore.saveAllData();

        reloadMovieTable();
        reloadScheduleDropdowns();
        reloadMovieLobbyGrid();

        JOptionPane.showMessageDialog(this, "Thêm phim mới thành công! Hãy lên lịch suất chiếu ở khung bên phải.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        resetMovieForm();
    }

    private void handleUpdateMovie() {
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
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tên phim!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Movie m = dataStore.getMovieMap().get(id);
        if (m != null) {
            if (dur <= 0) {
                JOptionPane.showMessageDialog(this, "Thời lượng phải lớn hơn 0."); return;
            }
            if (dur != m.getDurationMinutes() && dataStore.getShowTimeMap().values().stream().anyMatch(st -> st.getMovieId().equals(id))) {
                JOptionPane.showMessageDialog(this, "Phim đã có lịch chiếu; không đổi thời lượng để tránh sai lịch."); return;
            }
            m.setTitle(title);
            m.setGenre(genre);
            m.setDurationMinutes(dur);
            m.setDirector(txtMovieDirector.getText().trim());
            m.setAgeRating((String) cbAgeRating.getSelectedItem());
            if (selectedPosterPath != null && !selectedPosterPath.isEmpty()) {
                PosterManager.invalidate(m.getPosterPath());
                m.setPosterPath(selectedPosterPath);
                PosterManager.invalidate(selectedPosterPath);
            }
            dataStore.saveAllData();

            reloadMovieTable();
            reloadScheduleDropdowns();
            reloadMovieLobbyGrid();

            JOptionPane.showMessageDialog(this, "CẬP NHẬT PHIM THÀNH CÔNG!\nPhim: " + title, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            resetMovieForm();
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy phim có mã: " + id, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetMovieForm() {
        int maxId = 0;
        for (String key : dataStore.getMovieMap().keySet()) {
            if (key.startsWith("MOV")) {
                try {
                    int num = Integer.parseInt(key.substring(3));
                    if (num > maxId) maxId = num;
                } catch (Exception ignored) {}
            }
        }
        String nextId = String.format("MOV%02d", maxId + 1);

        txtMovieId.setText(nextId);
        txtMovieId.setEditable(true);
        txtMovieId.setBackground(Color.WHITE);
        txtMovieTitle.setText("");
        txtMovieGenre.setText("Hành Động");
        txtMovieDuration.setText("120");
        txtMovieDirector.setText("");
        cbAgeRating.setSelectedIndex(0);
        selectedPosterPath = "";
        updatePosterPreview(null);
        btnAddMovie.setEnabled(true);
        btnUpdateMovie.setEnabled(false);
        btnDeleteMovie.setEnabled(false);
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
            if (!Double.isFinite(price) || price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá vé phải là số hữu hạn lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String movieId = movieItem.substring(0, movieItem.indexOf(" | "));
        String roomId = roomItem.startsWith("ROOM02") ? "ROOM02" : "ROOM01";

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int hour = (int) spScheduleHour.getValue();
        int minute = (int) spScheduleMinute.getValue();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        Movie m = dataStore.getMovieMap().get(movieId);
        int dur = (m != null) ? m.getDurationMinutes() : 120;
        if (!startTime.isAfter(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Giờ chiếu phải ở tương lai.");
            return;
        }
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

        int maxSt = 0;
        for (String k : dataStore.getShowTimeMap().keySet()) {
            if (k.startsWith("ST")) {
                try {
                    int num = Integer.parseInt(k.substring(2));
                    if (num > maxSt) maxSt = num;
                } catch (Exception ignored) {}
            }
        }
        String stId = String.format("ST%02d", maxSt + 1);

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
        if (dataStore.getShowTimeMap().values().stream().anyMatch(st -> st.getMovieId().equals(id) && BookingRules.hasBookedSeats(st))) {
            JOptionPane.showMessageDialog(this, "Phim có suất đã bán vé, không thể xóa.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa phim và các suất chưa bán vé?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
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
        kpiContainer.add(createKpiCard("F&B TRƯỚC GIẢM GIÁ", lblKpiFnB, new Color(217, 119, 6)));
        kpiContainer.add(createKpiCard("TOP PHIM ĂN KHÁCH", lblKpiTopMovie, new Color(22, 163, 74)));

        panel.setBackground(new Color(245, 246, 248));
        JPanel overview = new JPanel(new BorderLayout(0, 16));
        overview.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel reportTitle = new JLabel("Thống kê & doanh thu");
        reportTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.add(reportTitle, BorderLayout.NORTH);
        JLabel reportSubtitle = new JLabel("Tổng quan hoạt động kinh doanh của rạp");
        reportSubtitle.setForeground(new Color(103, 113, 131));
        heading.add(reportSubtitle, BorderLayout.SOUTH);
        overview.add(heading, BorderLayout.NORTH);
        overview.add(kpiContainer, BorderLayout.CENTER);
        panel.add(overview, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.add(new JLabel("Lọc theo thời gian:"));
        cbFilterPeriod = new JComboBox<>(new String[]{"Tất cả thời gian", "Hôm nay", "7 ngày gần nhất"});
        cbFilterPeriod.addActionListener(e -> reloadReportDashboard());
        filterBar.add(cbFilterPeriod);
        centerPanel.setOpaque(false);
        filterBar.setOpaque(false);
        JPanel chartArea = new JPanel(new BorderLayout(0, 12));
        chartArea.setOpaque(false);
        chartArea.add(filterBar, BorderLayout.NORTH);
        reportCharts = new vn.edu.ptit.cinema.view.component.RevenueChartsPanel();
        chartArea.add(reportCharts, BorderLayout.CENTER);
        centerPanel.add(chartArea, BorderLayout.NORTH);

        String[] cols = {"Mã HĐ", "Phim", "Thu Ngân", "Khách Hàng", "Số Vé", "Ngày Tạo", "Thành Tiền"};
        modelInvoices = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tableInvoices = new JTable(modelInvoices);
        tableInvoices.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableInvoices.setRowHeight(28);

        // Click đúp vào 1 dòng để xem chi tiết hóa đơn in nhiệt POS
        tableInvoices.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableInvoices.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = tableInvoices.convertRowIndexToModel(row);
                        String invId = (String) modelInvoices.getValueAt(modelRow, 0);
                        Invoice inv = dataStore.getInvoiceMap().get(invId);
                        if (inv != null) {
                            showInvoiceDetailDialog(inv);
                        }
                    }
                }
            }
        });

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

        JButton btnClearAllInvoices = new JButton("Xóa Lịch Sử Giao Dịch");
        btnClearAllInvoices.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClearAllInvoices.setForeground(new Color(220, 38, 38));
        btnClearAllInvoices.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnClearAllInvoices.addActionListener(e -> handleClearAllInvoices());

        btnSortByRevenue = new JButton("Sắp xếp theo Tiền (Giảm dần)");
        btnSortByRevenue.addActionListener(e -> sortInvoicesByAmount());
        btnSortByDate = new JButton("Sắp xếp theo Ngày");
        btnSortByDate.addActionListener(e -> sortInvoicesByDate());
        btnExportReport = new JButton("Xuất Báo Cáo Ra File TXT");
        btnExportReport.addActionListener(e -> exportReportToFile());

        bottomTools.add(btnClearAllInvoices);
        bottomTools.add(btnSortByRevenue);
        bottomTools.add(btnSortByDate);
        bottomTools.add(btnExportReport);
        panel.add(bottomTools, BorderLayout.SOUTH);

        reloadReportDashboard();
        return panel;
    }

    private void handleClearAllInvoices() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "BẠN CÓ CHẮC CHẮN MUỐN XÓA TOÀN BỘ LỊCH SỬ GIAO DỊCH KHÔNG?\n"
                        + "• Toàn bộ hóa đơn và doanh thu sẽ được đặt lại về 0 VNĐ.\n"
                        + "• Các ghế đã bán trên sơ đồ rạp sẽ được trả về trạng thái trống để bắt đầu ca mới.\n"
                        + "Lưu ý: Thao tác này KHÔNG thể hoàn tác!",
                "Xác nhận xóa sạch lịch sử giao dịch",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.getInvoiceMap().clear();
            for (ShowTime st : dataStore.getShowTimeMap().values()) {
                if (st.getSeats() != null) {
                    for (Seat s : st.getSeats().values()) {
                        s.setStatus(SeatStatus.AVAILABLE);
                        s.setHoldTimestamp(0);
                        s.setHeldByUserId(null);
                    }
                }
            }
            dataStore.saveAllData();

            reloadReportDashboard();
            refreshBookingTabState();
            if (onlinePickupPanel != null) {
                onlinePickupPanel.reloadOnlineBookings();
            }

            JOptionPane.showMessageDialog(this,
                    "ĐÃ XÓA TOÀN BỘ LỊCH SỬ GIAO DỊCH THÀNH CÔNG!\n"
                            + "Doanh thu rạp hiện tại đã được đặt lại về 0 VNĐ.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
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

    private List<Invoice> filteredInvoices() {
        String period = (String) cbFilterPeriod.getSelectedItem();
        LocalDate today = LocalDate.now();
        List<Invoice> result = new ArrayList<>();
        for (Invoice inv : dataStore.getInvoiceMap().values()) {
            if (BookingRules.inPeriod(inv.getCreatedAt() == null ? null : inv.getCreatedAt().toLocalDate(), today, period)) result.add(inv);
        }
        return result;
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

        for (Invoice inv : filteredInvoices()) {
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
        lblKpiTopMovie.setToolTipText(topMovie + " — " + maxTickets + " vé");
        LocalDate firstDay = "Hôm nay".equals(period) ? today
                : "7 ngày gần nhất".equals(period) ? today.minusDays(6) : null;
        LocalDate lastDay = "Tất cả thời gian".equals(period) ? null : today;
        reportCharts.setInvoices(filteredInvoices(), firstDay, lastDay);
    }

    private void sortInvoicesByAmount() {
        List<Invoice> list = new ArrayList<>(filteredInvoices());
        list.sort((a, b) -> Double.compare(b.getFinalAmount(), a.getFinalAmount()));
        populateInvoiceTable(list);
    }

    private void sortInvoicesByDate() {
        List<Invoice> list = new ArrayList<>(filteredInvoices());
        list.sort((a, b) -> Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()).compare(a.getCreatedAt(), b.getCreatedAt()));
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
            pw.println("Phạm vi: " + cbFilterPeriod.getSelectedItem());
            pw.println("=======================================================");
            double total = 0;
            for (Invoice inv : filteredInvoices()) {
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





    private void showInvoiceDetailDialog(Invoice invoice) {
        if (invoice == null) return;
        JDialog dialog = new JDialog(this, "CHI TIẾT HÓA ĐƠN POS: " + invoice.getInvoiceId(), true);
        dialog.setSize(520, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea txtReceipt = new JTextArea();
        txtReceipt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReceipt.setEditable(false);
        txtReceipt.setBackground(new Color(254, 252, 232));
        txtReceipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        sb.append("=====================================================\n");
        sb.append("          RẠP CHIẾU PHIM PTIT CINEMA COMPLEX         \n");
        sb.append("       Đ/C: Km 10 Đ. Nguyễn Trãi, Q. Hà Đông, Hà Nội \n");
        sb.append("              HOTLINE: 1900 8888 - PTIT.EDU.VN       \n");
        sb.append("=====================================================\n");
        sb.append(String.format("Mã hóa đơn : %s\n", invoice.getInvoiceId()));
        sb.append(String.format("Thời gian   : %s\n", invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(dtf) : "N/A"));
        sb.append(String.format("Thu ngân    : %s\n", invoice.getStaffUsername()));
        sb.append(String.format("Khách hàng  : %s (%s)\n", invoice.getCustomerName(), invoice.getCustomerPhone()));
        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("Phim        : %s\n", invoice.getMovieTitle()));
        sb.append(String.format("Phòng chiếu : %s\n", invoice.getRoomName()));
        sb.append("Suất chiếu  : " + (invoice.getShowStartTime() == null ? "N/A" : invoice.getShowStartTime().format(dtf)) + "\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("DANH SÁCH VÉ:\n");
        for (vn.edu.ptit.cinema.model.booking.Ticket t : invoice.getTickets()) {
            sb.append(String.format("  - Ghế: %-8s Đơn giá: %,10.0f VNĐ\n", t.getSeatCode(), t.getPrice()));
        }
        if (!invoice.getFoods().isEmpty()) {
            sb.append("-----------------------------------------------------\n");
            sb.append("BẮP NƯỚC (F&B):\n");
            for (vn.edu.ptit.cinema.model.booking.OrderItem item : invoice.getFoods()) {
                sb.append(String.format("  - %-20s x%-2d %,10.0f VNĐ\n",
                        item.getFoodName(), item.getQuantity(), item.getTotalPrice()));
            }
        }
        sb.append("=====================================================\n");
        sb.append(String.format("Tổng tiền gốc    : %,12.0f VNĐ\n", invoice.getOriginalAmount()));
        if (invoice.getVoucherCode() != null && !invoice.getVoucherCode().isEmpty()) {
            sb.append("Mã voucher      : " + invoice.getVoucherCode() + "\n");
        }
        sb.append(String.format("Tổng giảm giá   : %,.0f VNĐ\n", invoice.getDiscountAmount()));
        sb.append(String.format("THANH TOÁN       : %,12.0f VNĐ\n", invoice.getFinalAmount()));
        if (invoice.getPaymentMethod() != null) {
            sb.append("Phương thức     : " + ("CASH".equals(invoice.getPaymentMethod()) ? "Tiền mặt" : "QR mô phỏng") + "\n");
            if ("CASH".equals(invoice.getPaymentMethod()))
                sb.append(String.format("Khách đưa       : %,.0f VNĐ\nTiền thừa       : %,.0f VNĐ\n", invoice.getCashReceived(), invoice.getChangeAmount()));
        }
        sb.append("=====================================================\n");
        sb.append("       CẢM ƠN QUÝ KHÁCH & CHÚC XEM PHIM VUI VẺ!      \n");
        sb.append("=====================================================\n");

        txtReceipt.setText(sb.toString());
        txtReceipt.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(txtReceipt);
        content.add(scroll, BorderLayout.CENTER);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnOpenFile = new JButton("Mở File Text");
        btnOpenFile.addActionListener(e -> {
            File file = new File("receipts", "Receipt_" + invoice.getInvoiceId() + ".txt");
            if (!file.exists()) {
                file = vn.edu.ptit.cinema.util.ReceiptPrinter.printReceipt(invoice);
            }
            if (file != null && file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Không thể mở file: " + ex.getMessage());
                }
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());

        JButton btnReprint = new JButton("In lại hóa đơn");
        btnReprint.addActionListener(e -> {
            File printed = vn.edu.ptit.cinema.util.ReceiptPrinter.printReceipt(invoice);
            JOptionPane.showMessageDialog(dialog, printed == null ? "Không thể ghi file hóa đơn." : "Đã in lại: " + printed.getAbsolutePath());
        });
        btnBox.add(btnReprint);
        btnBox.add(btnOpenFile);
        btnBox.add(btnClose);
        content.add(btnBox, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }
}
