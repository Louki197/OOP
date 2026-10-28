package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.booking.Invoice;
import vn.edu.ptit.cinema.model.booking.OrderItem;
import vn.edu.ptit.cinema.model.booking.Ticket;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.service.SessionManager;
import vn.edu.ptit.cinema.util.ReceiptPrinter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * [PHÂN HỆ LẤY VÉ & IN VÉ ĐẶT ONLINE - KIOSK & POS PICKUP]
 * Cho phép khách hàng hoặc nhân viên thu ngân tại rạp:
 * 1. Tra cứu đơn đặt online theo Mã Đặt Vé (Booking Code) hoặc Số Điện Thoại.
 * 2. Xem chi tiết thông tin vé, phòng chiếu, danh sách ghế ngồi và combo bắp nước.
 * 3. In vé xem phim nhiệt POS ra file .txt và đánh dấu "ĐÃ IN VÉ" (chống in trùng vé).
 * 4. Tích hợp tính năng "Mô phỏng khách đặt vé Online" để dễ dàng kiểm thử và trình diễn.
 */
@SuppressWarnings("serial")
public class OnlineTicketPickupPanel extends JPanel {

    private final DataStore dataStore = DataStore.getInstance();
    private final Frame parentFrame;

    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    private JTable tableBookings;
    private DefaultTableModel modelBookings;

    // Khung chi tiết
    private JLabel lblDetailBookingId;
    private JLabel lblDetailMovie;
    private JLabel lblDetailShowtime;
    private JLabel lblDetailRoom;
    private JLabel lblDetailSeats;
    private JLabel lblDetailCustomer;
    private JLabel lblDetailFnB;
    private JLabel lblDetailAmount;
    private JLabel lblDetailPrintStatus;

    private JButton btnPrintTicket;
    private JButton btnOpenTicketFile;

    private Invoice selectedInvoice = null;

    public OnlineTicketPickupPanel(Frame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        initUI();
        reloadOnlineBookings();
    }

    private void initUI() {
        // 1. THANH CÔNG CỤ TÌM KIẾM & THAO TÁC TRÊN ĐỈNH
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        // Header Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("HỆ THỐNG LẤY VÉ & IN VÉ ĐẶT ONLINE (ONLINE TICKET PICKUP)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Khách hàng xuất trình Mã Đặt Vé hoặc Số Điện Thoại để in vé cứng vào phòng chiếu");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        titlePanel.add(lblTitle, BorderLayout.NORTH);
        titlePanel.add(lblSub, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.NORTH);

        // Thanh tìm kiếm
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        toolbar.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);

        leftSearch.add(new JLabel("Tra cứu:"));
        txtSearch = new JTextField(22);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập Mã Đặt Vé hoặc SĐT khách...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        txtSearch.addActionListener(e -> reloadOnlineBookings());
        leftSearch.add(txtSearch);

        JButton btnSearch = new JButton("🔍 Tìm Kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnSearch.addActionListener(e -> reloadOnlineBookings());
        leftSearch.add(btnSearch);

        leftSearch.add(Box.createHorizontalStrut(8));
        leftSearch.add(new JLabel("Trạng thái in:"));
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả đơn", "⏳ Chưa in vé", "✅ Đã in vé"});
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbFilterStatus.addActionListener(e -> reloadOnlineBookings());
        leftSearch.add(cbFilterStatus);

        JButton btnResetFilter = new JButton("Làm mới");
        btnResetFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnResetFilter.addActionListener(e -> {
            txtSearch.setText("");
            cbFilterStatus.setSelectedIndex(0);
            reloadOnlineBookings();
        });
        leftSearch.add(btnResetFilter);

        toolbar.add(leftSearch, BorderLayout.WEST);

        // Nút mô phỏng khách đặt vé online (Dùng để thuyết trình / demo)
        JButton btnSimulate = new JButton("+ MÔ PHỎNG KHÁCH ĐẶT VÉ ONLINE");
        btnSimulate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSimulate.setBackground(new Color(37, 99, 235));
        btnSimulate.setForeground(Color.WHITE);
        btnSimulate.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,12,4,12;");
        btnSimulate.addActionListener(e -> openSimulateOnlineBookingDialog());
        toolbar.add(btnSimulate, BorderLayout.EAST);

        topPanel.add(toolbar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 2. KHU VỰC TRUNG TÂM: CHIA 2 PHẦN (BẢNG DANH SÁCH & KHUNG CHI TIẾT IN VÉ)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        // BẢNG DANH SÁCH CÁC ĐƠN ĐẶT ONLINE
        String[] columns = {"Mã Đặt Vé", "Tên Phim", "Khách Hàng", "Số Điện Thoại", "Suất Chiếu", "Phòng", "Ghế", "Tổng Tiền", "Trạng Thái In", "Thời Gian Đặt"};
        modelBookings = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tableBookings = new JTable(modelBookings);
        tableBookings.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableBookings.setRowHeight(30);
        tableBookings.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerCol = new DefaultTableCellRenderer();
        centerCol.setHorizontalAlignment(JLabel.CENTER);

        tableBookings.getColumnModel().getColumn(0).setPreferredWidth(95);
        tableBookings.getColumnModel().getColumn(0).setCellRenderer(centerCol);

        tableBookings.getColumnModel().getColumn(1).setPreferredWidth(190);

        tableBookings.getColumnModel().getColumn(2).setPreferredWidth(130);

        tableBookings.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableBookings.getColumnModel().getColumn(3).setCellRenderer(centerCol);

        tableBookings.getColumnModel().getColumn(4).setPreferredWidth(110);
        tableBookings.getColumnModel().getColumn(4).setCellRenderer(centerCol);

        tableBookings.getColumnModel().getColumn(5).setPreferredWidth(75);
        tableBookings.getColumnModel().getColumn(5).setCellRenderer(centerCol);

        tableBookings.getColumnModel().getColumn(6).setPreferredWidth(85);
        tableBookings.getColumnModel().getColumn(6).setCellRenderer(centerCol);

        tableBookings.getColumnModel().getColumn(7).setPreferredWidth(110);
        tableBookings.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.RIGHT);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(220, 38, 38));
                return lbl;
            }
        });

        tableBookings.getColumnModel().getColumn(8).setPreferredWidth(120);
        tableBookings.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String status = value != null ? value.toString() : "";
                if (status.contains("CHƯA IN")) {
                    lbl.setForeground(new Color(217, 119, 6)); // Màu cam cảnh báo
                } else {
                    lbl.setForeground(new Color(22, 163, 74)); // Màu xanh lá thành công
                }
                return lbl;
            }
        });

        tableBookings.getColumnModel().getColumn(9).setPreferredWidth(130);
        tableBookings.getColumnModel().getColumn(9).setCellRenderer(centerCol);

        tableBookings.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tableBookings.getSelectedRow();
                if (row >= 0) {
                    int modelRow = tableBookings.convertRowIndexToModel(row);
                    String id = (String) modelBookings.getValueAt(modelRow, 0);
                    Invoice inv = dataStore.getInvoiceMap().get(id);
                    if (inv != null) {
                        displayBookingDetail(inv);
                    }
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(tableBookings);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Danh Sách Đơn Đặt Vé Trực Tuyến (Chọn một đơn để in vé)"));
        splitPane.setTopComponent(scrollTable);

        // KHUNG CHI TIẾT ĐƠN HÀNG & THAO TÁC IN VÉ
        JPanel detailContainer = createDetailPanel();
        splitPane.setBottomComponent(detailContainer);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Chi Tiết Vé & Xác Nhận In Vé Cứng Vào Cửa"),
                new EmptyBorder(10, 16, 12, 16)
        ));

        JPanel gridInfo = new JPanel(new GridLayout(4, 2, 15, 6));
        gridInfo.setOpaque(false);

        lblDetailBookingId = new JLabel("Mã đặt vé: --");
        lblDetailBookingId.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblDetailMovie = new JLabel("Phim: --");
        lblDetailMovie.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailMovie.setForeground(new Color(2, 132, 199));

        lblDetailCustomer = new JLabel("Khách hàng: --");
        lblDetailShowtime = new JLabel("Suất chiếu: --");

        lblDetailSeats = new JLabel("Ghế ngồi: --");
        lblDetailSeats.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailSeats.setForeground(new Color(220, 38, 38));

        lblDetailRoom = new JLabel("Phòng chiếu: --");

        lblDetailFnB = new JLabel("Bắp nước: Không có");
        lblDetailAmount = new JLabel("Tổng tiền đã thanh toán: --");
        lblDetailAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailAmount.setForeground(new Color(22, 163, 74));

        gridInfo.add(lblDetailBookingId);
        gridInfo.add(lblDetailMovie);
        gridInfo.add(lblDetailCustomer);
        gridInfo.add(lblDetailShowtime);
        gridInfo.add(lblDetailSeats);
        gridInfo.add(lblDetailRoom);
        gridInfo.add(lblDetailFnB);
        gridInfo.add(lblDetailAmount);

        panel.add(gridInfo, BorderLayout.CENTER);

        // Cột bên phải: Huy hiệu trạng thái và các nút in
        JPanel rightActions = new JPanel();
        rightActions.setLayout(new BoxLayout(rightActions, BoxLayout.Y_AXIS));
        rightActions.setOpaque(false);
        rightActions.setPreferredSize(new Dimension(280, 0));

        lblDetailPrintStatus = new JLabel("Vui lòng chọn một đơn đặt vé", JLabel.CENTER);
        lblDetailPrintStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailPrintStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblDetailPrintStatus.setForeground(Color.GRAY);

        btnPrintTicket = new JButton("🖨️ XÁC NHẬN & IN VÉ CỨNG");
        btnPrintTicket.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrintTicket.setBackground(new Color(22, 163, 74));
        btnPrintTicket.setForeground(Color.WHITE);
        btnPrintTicket.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 8,16,8,16;");
        btnPrintTicket.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPrintTicket.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPrintTicket.setEnabled(false);
        btnPrintTicket.addActionListener(e -> handlePrintTicket());

        btnOpenTicketFile = new JButton("📂 Mở File Vé In Nhiệt (.txt)");
        btnOpenTicketFile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenTicketFile.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnOpenTicketFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenTicketFile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btnOpenTicketFile.setEnabled(false);
        btnOpenTicketFile.addActionListener(e -> handleOpenTicketFile());

        rightActions.add(Box.createVerticalStrut(8));
        rightActions.add(lblDetailPrintStatus);
        rightActions.add(Box.createVerticalStrut(12));
        rightActions.add(btnPrintTicket);
        rightActions.add(Box.createVerticalStrut(8));
        rightActions.add(btnOpenTicketFile);

        panel.add(rightActions, BorderLayout.EAST);
        return panel;
    }

    public void reloadOnlineBookings() {
        modelBookings.setRowCount(0);

        String kw = txtSearch.getText().trim().toLowerCase();
        String filterStatus = (String) cbFilterStatus.getSelectedItem();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        DateTimeFormatter dtfCreated = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Invoice> list = new ArrayList<>(dataStore.getInvoiceMap().values());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        int count = 0;
        for (Invoice inv : list) {
            // Chỉ hiển thị các đơn đặt Online hoặc các đơn cần lấy vé
            boolean isOnline = "ONLINE".equalsIgnoreCase(inv.getBookingChannel());
            if (!isOnline) {
                // Vẫn cho phép tra cứu nếu gõ đúng mã đơn hoặc SĐT
                if (kw.isEmpty()) continue;
            }

            if (!kw.isEmpty()) {
                boolean matchId = inv.getInvoiceId() != null && inv.getInvoiceId().toLowerCase().contains(kw);
                boolean matchPhone = inv.getCustomerPhone() != null && inv.getCustomerPhone().contains(kw);
                boolean matchName = inv.getCustomerName() != null && inv.getCustomerName().toLowerCase().contains(kw);
                if (!matchId && !matchPhone && !matchName) continue;
            }

            if (filterStatus != null) {
                if (filterStatus.contains("Chưa in") && inv.isTicketPrinted()) continue;
                if (filterStatus.contains("Đã in") && !inv.isTicketPrinted()) continue;
            }

            count++;
            List<String> seats = new ArrayList<>();
            if (inv.getTickets() != null) {
                for (Ticket t : inv.getTickets()) seats.add(t.getSeatCode());
            }

            String statusStr = inv.isTicketPrinted() ? "✅ ĐÃ IN VÉ" : "⏳ CHƯA IN VÉ";
            String showtimeStr = inv.getShowStartTime() != null ? inv.getShowStartTime().format(dtf) : "N/A";
            String createdStr = inv.getCreatedAt() != null ? inv.getCreatedAt().format(dtfCreated) : "N/A";

            modelBookings.addRow(new Object[]{
                    inv.getInvoiceId(),
                    inv.getMovieTitle(),
                    inv.getCustomerName(),
                    inv.getCustomerPhone(),
                    showtimeStr,
                    inv.getRoomName(),
                    String.join(", ", seats),
                    String.format("%,.0f VNĐ", inv.getFinalAmount()),
                    statusStr,
                    createdStr
            });
        }

        if (count == 0) {
            clearDetailPanel();
        } else if (tableBookings.getSelectedRow() < 0 && tableBookings.getRowCount() > 0) {
            tableBookings.setRowSelectionInterval(0, 0);
        }
    }

    private void displayBookingDetail(Invoice inv) {
        this.selectedInvoice = inv;
        if (inv == null) {
            clearDetailPanel();
            return;
        }

        lblDetailBookingId.setText("Mã đặt vé: " + inv.getInvoiceId());
        lblDetailMovie.setText("Phim: " + inv.getMovieTitle());
        lblDetailCustomer.setText("Khách hàng: " + inv.getCustomerName() + " (" + inv.getCustomerPhone() + ")");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        lblDetailShowtime.setText("Suất chiếu: " + (inv.getShowStartTime() != null ? inv.getShowStartTime().format(dtf) : "N/A"));
        lblDetailRoom.setText("Phòng chiếu: " + inv.getRoomName());

        List<String> seats = new ArrayList<>();
        if (inv.getTickets() != null) {
            for (Ticket t : inv.getTickets()) seats.add(t.getSeatCode());
        }
        lblDetailSeats.setText("Ghế ngồi (" + seats.size() + " ghế): " + String.join(", ", seats));

        if (inv.getFoods() != null && !inv.getFoods().isEmpty()) {
            List<String> foodStrs = new ArrayList<>();
            for (OrderItem f : inv.getFoods()) {
                foodStrs.add(f.getFoodName() + " x" + f.getQuantity());
            }
            lblDetailFnB.setText("Bắp nước: " + String.join("; ", foodStrs));
        } else {
            lblDetailFnB.setText("Bắp nước: Không kèm bắp nước");
        }

        lblDetailAmount.setText(String.format("Tổng tiền đã trả: %,.0f VNĐ (%s)",
                inv.getFinalAmount(), inv.getBookingChannel() != null ? inv.getBookingChannel() : "ONLINE"));

        DateTimeFormatter dtfPrint = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        if (inv.isTicketPrinted()) {
            lblDetailPrintStatus.setText("TRẠNG THÁI: ĐÃ IN VÉ CỨNG");
            lblDetailPrintStatus.setForeground(new Color(22, 163, 74));
            btnPrintTicket.setText("In Lại Vé (Cảnh báo)");
            btnPrintTicket.setBackground(new Color(241, 245, 249));
            btnPrintTicket.setForeground(new Color(71, 85, 105));
        } else {
            lblDetailPrintStatus.setText("TRẠNG THÁI: CHỜ IN VÉ TẠI QUẦY");
            lblDetailPrintStatus.setForeground(new Color(217, 119, 6));
            btnPrintTicket.setText("🖨️ XÁC NHẬN & IN VÉ CỨNG");
            btnPrintTicket.setBackground(new Color(22, 163, 74));
            btnPrintTicket.setForeground(Color.WHITE);
        }

        btnPrintTicket.setEnabled(true);
        btnOpenTicketFile.setEnabled(true);
    }

    private void clearDetailPanel() {
        selectedInvoice = null;
        lblDetailBookingId.setText("Mã đặt vé: --");
        lblDetailMovie.setText("Phim: --");
        lblDetailCustomer.setText("Khách hàng: --");
        lblDetailShowtime.setText("Suất chiếu: --");
        lblDetailSeats.setText("Ghế ngồi: --");
        lblDetailRoom.setText("Phòng chiếu: --");
        lblDetailFnB.setText("Bắp nước: --");
        lblDetailAmount.setText("Tổng tiền đã thanh toán: --");
        lblDetailPrintStatus.setText("Vui lòng chọn một đơn đặt vé");
        lblDetailPrintStatus.setForeground(Color.GRAY);
        btnPrintTicket.setEnabled(false);
        btnOpenTicketFile.setEnabled(false);
    }

    private void handlePrintTicket() {
        if (selectedInvoice == null) return;

        if (selectedInvoice.isTicketPrinted()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String printedTimeStr = selectedInvoice.getPrintedAt() != null ? selectedInvoice.getPrintedAt().format(dtf) : "trước đó";
            int confirm = JOptionPane.showConfirmDialog(this,
                    "CẢNH BÁO: Vé cho đơn " + selectedInvoice.getInvoiceId() + " ĐÃ ĐƯỢC IN lúc " + printedTimeStr + "!\n"
                            + "Việc in lại có thể gây trùng lặp vé vào rạp. Bạn có chắc chắn muốn in lại không?",
                    "Cảnh báo in trùng vé", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        selectedInvoice.setTicketPrinted(true);
        selectedInvoice.setPrintedAt(LocalDateTime.now());
        dataStore.saveAllData();

        File ticketFile = ReceiptPrinter.printOnlineTicket(selectedInvoice);

        JOptionPane.showMessageDialog(this,
                "IN VÉ CỨNG THÀNH CÔNG!\n"
                        + "• Mã đặt vé : " + selectedInvoice.getInvoiceId() + "\n"
                        + "• Phim      : " + selectedInvoice.getMovieTitle() + "\n"
                        + "• Số vé     : " + (selectedInvoice.getTickets() != null ? selectedInvoice.getTickets().size() : 0) + " vé\n"
                        + "• File phiếu: " + (ticketFile != null ? ticketFile.getName() : "receipts/"),
                "Xuất Vé Thành Công", JOptionPane.INFORMATION_MESSAGE);

        reloadOnlineBookings();
        displayBookingDetail(selectedInvoice);
    }

    private void handleOpenTicketFile() {
        if (selectedInvoice == null) return;
        File file = new File("receipts", "Ticket_Online_" + selectedInvoice.getInvoiceId() + ".txt");
        if (!file.exists()) {
            file = ReceiptPrinter.printOnlineTicket(selectedInvoice);
        }
        if (file != null && file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở file: " + ex.getMessage());
            }
        }
    }

    /**
     * HỘP THOẠI MÔ PHỎNG KHÁCH ĐẶT VÉ ONLINE (DÀNH CHO TEST & TRÌNH DIỄN BẢO VỆ ĐỒ ÁN)
     */
    private void openSimulateOnlineBookingDialog() {
        JDialog dlg = new JDialog(parentFrame, "MÔ PHỎNG KHÁCH HÀNG ĐẶT VÉ QUA APP / WEB ONLINE", true);
        dlg.setSize(480, 480);
        dlg.setLocationRelativeTo(parentFrame);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 12));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        JComboBox<String> cbMovie = new JComboBox<>();
        for (Movie m : dataStore.getMovieMap().values()) {
            cbMovie.addItem(String.format("[%s] %s", m.getId(), m.getTitle()));
        }

        JTextField txtCustName = new JTextField("Lê Hoàng Long");
        JTextField txtCustPhone = new JTextField("0934567890");
        JTextField txtSeatChoice = new JTextField("E03, E04");
        JComboBox<String> cbFoodChoice = new JComboBox<>(new String[]{
                "Không kèm bắp nước",
                "Combo Solo (1 Bắp L + 1 Nước L)",
                "Combo Couple (1 Bắp L + 2 Nước L)"
        });

        form.add(new JLabel("Chọn phim:"));
        form.add(cbMovie);
        form.add(new JLabel("Họ tên khách:"));
        form.add(txtCustName);
        form.add(new JLabel("Số điện thoại:"));
        form.add(txtCustPhone);
        form.add(new JLabel("Ghế ngồi (cách nhau dấu phẩy):"));
        form.add(txtSeatChoice);
        form.add(new JLabel("Combo bắp nước online:"));
        form.add(cbFoodChoice);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnConfirm = new JButton("Xác Nhận Đặt Vé Online");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConfirm.setBackground(new Color(37, 99, 235));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnConfirm.addActionListener(e -> {
            String name = txtCustName.getText().trim();
            String phone = txtCustPhone.getText().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập tên và SĐT khách!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selMovie = (String) cbMovie.getSelectedItem();
            String movieId = selMovie.substring(selMovie.indexOf('[') + 1, selMovie.indexOf(']'));
            Movie movie = dataStore.getMovieMap().get(movieId);

            // Tìm một suất chiếu của phim này
            ShowTime targetSt = null;
            for (ShowTime st : dataStore.getShowTimeMap().values()) {
                if (st.getMovieId().equals(movieId)) {
                    targetSt = st;
                    break;
                }
            }

            if (targetSt == null) {
                JOptionPane.showMessageDialog(dlg, "Phim này hiện chưa có suất chiếu nào!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] seatArray = txtSeatChoice.getText().trim().split("[,\\s]+");
            List<Ticket> tickets = new ArrayList<>();
            double ticketTotal = 0;

            for (String rawCode : seatArray) {
                String code = rawCode.trim().toUpperCase();
                if (code.isEmpty()) continue;
                Seat seat = targetSt.getSeats().get(code);
                if (seat != null) {
                    seat.setStatus(SeatStatus.BOOKED); // Đã thanh toán online nên giữ chỗ cứng
                    double price = targetSt.getBasePrice();
                    tickets.add(new Ticket("TK" + (System.currentTimeMillis() % 100000), targetSt.getId(), code, price));
                    ticketTotal += price;
                }
            }

            if (tickets.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập ít nhất 1 mã ghế hợp lệ (VD: E03, E04)!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<OrderItem> foods = new ArrayList<>();
            int foodIdx = cbFoodChoice.getSelectedIndex();
            double foodTotal = 0;
            if (foodIdx == 1) {
                foods.add(new OrderItem("F04", "Combo Solo (1 Bắp + 1 Nước)", 1, 85000.0));
                foodTotal += 85000.0;
            } else if (foodIdx == 2) {
                foods.add(new OrderItem("F05", "Combo Couple (1 Bắp + 2 Nước)", 1, 115000.0));
                foodTotal += 115000.0;
            }

            double finalTotal = ticketTotal + foodTotal;
            String newInvoiceId = "OL" + (System.currentTimeMillis() % 1000000);

            Invoice onlineInv = new Invoice(
                    newInvoiceId,
                    "App-PTIT-Online",
                    name,
                    phone,
                    targetSt.getId(),
                    movie != null ? movie.getTitle() : movieId,
                    "Phòng Chiếu 01",
                    targetSt.getStartTime(),
                    tickets,
                    foods,
                    null,
                    finalTotal,
                    0,
                    finalTotal,
                    LocalDateTime.now(),
                    "ONLINE",
                    false, // CHƯA IN VÉ
                    null
            );

            dataStore.getInvoiceMap().put(newInvoiceId, onlineInv);
            dataStore.saveAllData();

            dlg.dispose();
            reloadOnlineBookings();
            txtSearch.setText(newInvoiceId);
            reloadOnlineBookings();

            JOptionPane.showMessageDialog(parentFrame,
                    "ĐÃ MÔ PHỎNG ĐẶT VÉ ONLINE THÀNH CÔNG!\n"
                            + "• Mã đặt vé   : " + newInvoiceId + "\n"
                            + "• Khách hàng  : " + name + " (" + phone + ")\n"
                            + "• Phim        : " + (movie != null ? movie.getTitle() : movieId) + "\n"
                            + "• Số tiền trả : " + String.format("%,.0f VNĐ", finalTotal) + "\n"
                            + "• Trạng thái  : ⏳ CHƯA IN VÉ (Đang chờ khách đến nhận tại quầy)\n\n"
                            + "Bây giờ bạn có thể bấm nút '🖨️ XÁC NHẬN & IN VÉ CỨNG' để in vé cho khách!",
                    "Đặt Vé Online Thành Công", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.addActionListener(e -> dlg.dispose());

        btnBar.add(btnCancel);
        btnBar.add(btnConfirm);
        dlg.add(btnBar, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
