package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.account.Customer;
import vn.edu.ptit.cinema.model.booking.Invoice;
import vn.edu.ptit.cinema.model.booking.OrderItem;
import vn.edu.ptit.cinema.model.booking.Ticket;
import vn.edu.ptit.cinema.model.booking.Voucher;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.pattern.strategy.PricingStrategies;
import vn.edu.ptit.cinema.pattern.strategy.PricingStrategy;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.service.SessionManager;
import vn.edu.ptit.cinema.util.ReceiptPrinter;
import vn.edu.ptit.cinema.view.component.QrCodePanel;

import vn.edu.ptit.cinema.service.BookingRules;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * [STEP 3: PAYMENT DIALOG - XÁC NHẬN THANH TOÁN VÀ IN HÓA ĐƠN]
 * Hỗ trợ chuyển đổi linh hoạt: Tiền mặt tại quầy hoặc Quét mã VietQR ngân hàng.
 * Tích hợp hệ thống Khách hàng thành viên (Member Loyalty) tích điểm và trừ tiền vé.
 */
@SuppressWarnings("serial")
public class PaymentDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final ShowTime showTime;
    private final Set<String> seatCodes;
    private final List<OrderItem> orderFoods;
    private final String tempInvoiceId;

    private JTextField txtCustomerName;
    private JTextField txtCustomerPhone;
    private JLabel lblCustomerTierBadge;
    private JCheckBox chkUseLoyaltyPoints;
    private JPanel chkPointsRow;
    private JButton btnRegisterMember;
    private JButton btnLookupMember;
    private Customer currentCustomer = null;
    private double pointsDiscountAmount = 0;

    private JTextField txtVoucher;
    private JButton btnApplyVoucher;
    private JButton btnRemoveVoucher;
    private JLabel lblVoucherStatus;
    private JCheckBox chkStudentDiscount;

    private JRadioButton rbCash;
    private JRadioButton rbVietQR;
    private JPanel qrCardContainer;
    private JTextField txtCashReceived;
    private JLabel lblChange;
    private JButton btnPayAndPrint;
    private javax.swing.Timer expiryTimer;

    private JLabel lblTicketSummary;
    private JLabel lblFoodSummary;
    private JLabel lblVoucherDiscount;
    private JLabel lblPointsDiscountSummary;
    private JLabel lblFinalAmount;

    private Voucher appliedVoucher = null;
    private boolean paymentSuccessful = false;
    private double currentFinalTotal = 0;

    public PaymentDialog(Frame parent, ShowTime showTime, Set<String> seatCodes, List<OrderItem> orderFoods) {
        super(parent, "XÁC NHẬN THANH TOÁN VÉ & DỊCH VỤ", true);
        this.showTime = showTime;
        this.seatCodes = new java.util.LinkedHashSet<>(seatCodes);
        this.orderFoods = orderFoods != null ? orderFoods : new ArrayList<>();
        this.tempInvoiceId = "INV-" + java.util.UUID.randomUUID();
        initUI();
    }

    private void initUI() {
        setSize(820, 750);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout(15, 15));

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(15, 25, 12, 25));
        headerPanel.setBackground(new Color(248, 250, 252));

        JLabel lblTitle = new JLabel("THANH TOÁN HÓA ĐƠN & XUẤT VÉ");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(22, 163, 74));

        Movie movie = DataStore.getInstance().getMovieMap().get(showTime.getMovieId());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        String subText = String.format("Phim: %s | Suất: %s | Ghế: %s",
                movie != null ? movie.getTitle() : "Phim",
                showTime.getStartTime().format(dtf),
                String.join(", ", seatCodes));
        vn.edu.ptit.cinema.model.cinema.Room room = DataStore.getInstance().getRoomMap().get(showTime.getRoomId());
        JLabel lblSub = new JLabel("<html>" + subText + "<br>Phòng: " + (room == null ? showTime.getRoomId() : room.getName()) + "</html>");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.DARK_GRAY);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Body chia làm 2 cột: Cột trái (Thông tin & Khuyến mãi), Cột phải (Phương thức & QR Code)
        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        contentGrid.setBorder(new EmptyBorder(5, 25, 5, 25));

        // --- CỘT TRÁI: KHÁCH HÀNG & CHI PHÍ ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Block 1: Thông tin khách & Thẻ thành viên
        JPanel custBox = new JPanel();
        custBox.setLayout(new BoxLayout(custBox, BoxLayout.Y_AXIS));
        custBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("1. Khách hàng & Thành viên (Member)"),
                new EmptyBorder(6, 10, 8, 10)
        ));

        // Dòng SĐT + Nút Tra cứu
        JPanel phoneRow = new JPanel(new BorderLayout(6, 0));
        phoneRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        phoneRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblPhoneTitle = new JLabel("Số ĐT: ");
        lblPhoneTitle.setPreferredSize(new Dimension(55, 28));
        txtCustomerPhone = new JTextField("0912345678");
        btnLookupMember = new JButton("Tra cứu");
        btnLookupMember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnLookupMember.addActionListener(e -> lookupCustomerByPhone());

        phoneRow.add(lblPhoneTitle, BorderLayout.WEST);
        phoneRow.add(txtCustomerPhone, BorderLayout.CENTER);
        phoneRow.add(btnLookupMember, BorderLayout.EAST);

        // Dòng Tên khách
        JPanel nameRow = new JPanel(new BorderLayout(6, 0));
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblNameTitle = new JLabel("Họ tên: ");
        lblNameTitle.setPreferredSize(new Dimension(55, 28));
        txtCustomerName = new JTextField("Nguyễn Văn Tuấn");
        nameRow.add(lblNameTitle, BorderLayout.WEST);
        nameRow.add(txtCustomerName, BorderLayout.CENTER);

        // Dòng Badge thành viên & Nút Đăng ký nhanh
        JPanel memberBadgeRow = new JPanel(new BorderLayout(6, 0));
        memberBadgeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        memberBadgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCustomerTierBadge = new JLabel("Thành viên VIP (Tích lũy: 50.000 VNĐ)");
        lblCustomerTierBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCustomerTierBadge.setForeground(new Color(22, 163, 74));

        btnRegisterMember = new JButton("+ Cấp thẻ mới");
        btnRegisterMember.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRegisterMember.setBackground(new Color(241, 245, 249));
        btnRegisterMember.setForeground(new Color(22, 163, 74));
        btnRegisterMember.putClientProperty(FlatClientProperties.STYLE, "arc: 6;");
        btnRegisterMember.setVisible(true);
        btnRegisterMember.addActionListener(e -> handleQuickRegisterCustomer());

        memberBadgeRow.add(lblCustomerTierBadge, BorderLayout.CENTER);
        memberBadgeRow.add(btnRegisterMember, BorderLayout.EAST);

        // Checkbox dùng điểm tích lũy - Căn chuẩn lề trái
        chkUseLoyaltyPoints = new JCheckBox("Dùng điểm tích lũy trừ tiền vé");
        chkUseLoyaltyPoints.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkUseLoyaltyPoints.setForeground(new Color(37, 99, 235));
        chkUseLoyaltyPoints.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkUseLoyaltyPoints.addActionListener(e -> recalculateTotal());

        chkPointsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chkPointsRow.setOpaque(false);
        chkPointsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkPointsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        chkPointsRow.add(chkUseLoyaltyPoints);

        custBox.add(phoneRow);
        custBox.add(Box.createVerticalStrut(6));
        custBox.add(nameRow);
        custBox.add(Box.createVerticalStrut(6));
        custBox.add(memberBadgeRow);
        custBox.add(Box.createVerticalStrut(4));
        custBox.add(chkPointsRow);

        // Tự động tra cứu khi rời ô SĐT
        txtCustomerPhone.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                lookupCustomerByPhone();
            }
        });

        leftPanel.add(custBox);
        leftPanel.add(Box.createVerticalStrut(10));

        // Block 2: Voucher & Ưu đãi
        JPanel voucherBox = new JPanel();
        voucherBox.setLayout(new BoxLayout(voucherBox, BoxLayout.Y_AXIS));
        voucherBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2. Khuyến mãi & Giảm giá"),
                new EmptyBorder(6, 10, 8, 10)
        ));

        JPanel vInput = new JPanel(new BorderLayout(6, 0));
        vInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        vInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtVoucher = new JTextField("PTIT10");
        txtVoucher.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập PTIT10 hoặc SINHVIEN20");

        btnApplyVoucher = new JButton("Áp dụng");
        btnApplyVoucher.addActionListener(e -> applyVoucher());

        btnRemoveVoucher = new JButton("Hủy");
        btnRemoveVoucher.setVisible(false);
        btnRemoveVoucher.addActionListener(e -> removeVoucher());

        JPanel vBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        vBtnPanel.setOpaque(false);
        vBtnPanel.add(btnApplyVoucher);
        vBtnPanel.add(btnRemoveVoucher);

        vInput.add(txtVoucher, BorderLayout.CENTER);
        vInput.add(vBtnPanel, BorderLayout.EAST);

        lblVoucherStatus = new JLabel(" ");
        lblVoucherStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVoucherStatus.setForeground(new Color(22, 163, 74));
        lblVoucherStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        chkStudentDiscount = new JCheckBox("Áp dụng ưu đãi Sinh viên PTIT (-20%)");
        chkStudentDiscount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkStudentDiscount.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkStudentDiscount.addActionListener(e -> recalculateTotal());

        JPanel chkStudentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chkStudentRow.setOpaque(false);
        chkStudentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkStudentRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        chkStudentRow.add(chkStudentDiscount);

        voucherBox.add(vInput);
        voucherBox.add(Box.createVerticalStrut(4));
        voucherBox.add(lblVoucherStatus);
        voucherBox.add(Box.createVerticalStrut(4));
        voucherBox.add(chkStudentRow);
        leftPanel.add(voucherBox);
        leftPanel.add(Box.createVerticalStrut(10));

        // Block 3: Tóm tắt chi phí
        JPanel summaryBox = new JPanel(new GridLayout(5, 1, 4, 4));
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("3. Tóm tắt chi phí"),
                new EmptyBorder(6, 10, 6, 10)
        ));

        lblTicketSummary = new JLabel("• Tiền vé: 0 VNĐ");
        lblFoodSummary = new JLabel("• Tiền bắp nước (F&B): 0 VNĐ");
        lblVoucherDiscount = new JLabel("• Giảm giá Voucher: -0 VNĐ");
        lblVoucherDiscount.setForeground(new Color(22, 163, 74));
        lblPointsDiscountSummary = new JLabel("• Trừ điểm thành viên: -0 VNĐ");
        lblPointsDiscountSummary.setForeground(new Color(37, 99, 235));
        lblFinalAmount = new JLabel("TỔNG TIỀN: 0 VNĐ");
        lblFinalAmount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFinalAmount.setForeground(new Color(220, 38, 38));

        summaryBox.add(lblTicketSummary);
        summaryBox.add(lblFoodSummary);
        summaryBox.add(lblVoucherDiscount);
        summaryBox.add(lblPointsDiscountSummary);
        summaryBox.add(lblFinalAmount);
        leftPanel.add(summaryBox);

        contentGrid.add(leftPanel);

        // --- CỘT PHẢI: PHƯƠNG THỨC THANH TOÁN (TIỀN MẶT / VIETQR) ---
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder("4. Phương thức thanh toán"));

        // Bộ chọn radio button
        JPanel methodSelect = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        rbCash = new JRadioButton("Tiền mặt tại quầy", true);
        rbVietQR = new JRadioButton("QR mô phỏng", false);
        ButtonGroup bgMethod = new ButtonGroup();
        bgMethod.add(rbCash);
        bgMethod.add(rbVietQR);
        methodSelect.add(rbCash);
        methodSelect.add(rbVietQR);
        rightPanel.add(methodSelect, BorderLayout.NORTH);

        // Khu vực hiển thị mã QR hoặc hướng dẫn tiền mặt
        qrCardContainer = new JPanel(new GridBagLayout());
        qrCardContainer.setBackground(new Color(248, 250, 252));
        rightPanel.add(qrCardContainer, BorderLayout.CENTER);

        rbCash.addActionListener(e -> updatePaymentMethodUI());
        rbVietQR.addActionListener(e -> updatePaymentMethodUI());

        contentGrid.add(rightPanel);
        add(contentGrid, BorderLayout.CENTER);

        // 3. Footer Buttons
        JPanel footerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        footerPanel.setBorder(new EmptyBorder(12, 25, 20, 25));

        JButton btnCancel = new JButton("Quay lại chọn thêm");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 8,16,8,16;");
        btnCancel.addActionListener(e -> dispose());

        btnPayAndPrint = new JButton("Xác nhận & In hóa đơn (.txt)");
        btnPayAndPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPayAndPrint.setBackground(new Color(22, 163, 74));
        btnPayAndPrint.setForeground(Color.WHITE);
        btnPayAndPrint.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 8,16,8,16;");
        btnPayAndPrint.addActionListener(e -> processPayment());

        footerPanel.add(btnCancel);
        footerPanel.add(btnPayAndPrint);
        add(footerPanel, BorderLayout.SOUTH);

        txtCashReceived = new JTextField(14);
        txtCashReceived.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
        });
        lblChange = new JLabel("Nhập tiền khách đưa (VNĐ)");
        lookupCustomerByPhone();
        recalculateTotal();
        updatePaymentMethodUI();
        expiryTimer = new javax.swing.Timer(1000, e -> {
            if (!BookingRules.canSell(showTime, LocalDateTime.now()) || !BookingRules.validHold(showTime, seatCodes, System.currentTimeMillis())) {
                btnPayAndPrint.setEnabled(false);
                expiryTimer.stop();
                JOptionPane.showMessageDialog(this, "Ghế đã hết hạn giữ hoặc suất đã bắt đầu. Vui lòng chọn lại.");
                dispose();
            }
        });
        expiryTimer.start();
    }

    private void lookupCustomerByPhone() {
        String phone = txtCustomerPhone.getText().trim();
        Customer c = DataStore.getInstance().getCustomerMap().get(phone);
        if (c != null) {
            currentCustomer = c;
            txtCustomerName.setText(c.getFullName());
            lblCustomerTierBadge.setText(String.format("Thành viên: %s (Tích lũy: %,.0f VNĐ)", c.getMembershipTier(), c.getLoyaltyPoints()));
            lblCustomerTierBadge.setForeground(new Color(22, 163, 74));
            btnRegisterMember.setVisible(true);
            boolean hasPoints = c.getLoyaltyPoints() > 0;
            chkUseLoyaltyPoints.setVisible(hasPoints);
            if (chkPointsRow != null) chkPointsRow.setVisible(hasPoints);
            chkUseLoyaltyPoints.setText(String.format("Dùng %,.0f điểm tích lũy trừ tiền vé", c.getLoyaltyPoints()));
        } else {
            currentCustomer = null;
            lblCustomerTierBadge.setText("• Khách vãng lai (Chưa đăng ký thẻ)");
            lblCustomerTierBadge.setForeground(Color.GRAY);
            btnRegisterMember.setVisible(true);
            chkUseLoyaltyPoints.setVisible(false);
            if (chkPointsRow != null) chkPointsRow.setVisible(false);
            chkUseLoyaltyPoints.setSelected(false);
        }
        recalculateTotal();
    }

    private void handleQuickRegisterCustomer() {
        CustomerManagementDialog dlg = new CustomerManagementDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dlg.showRegisterCustomerDialog();
        lookupCustomerByPhone();
    }

    private void updatePaymentMethodUI() {
        qrCardContainer.removeAll();
        if (rbVietQR.isSelected()) {
            QrCodePanel qrPanel = new QrCodePanel(tempInvoiceId, currentFinalTotal);
            qrCardContainer.add(qrPanel);
        } else {
            JPanel cashGuide = new JPanel();
            cashGuide.setLayout(new BoxLayout(cashGuide, BoxLayout.Y_AXIS));
            cashGuide.setOpaque(false);

            JLabel lblCashIcon = new JLabel("TIỀN MẶT", JLabel.CENTER);
            lblCashIcon.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblCashIcon.setForeground(new Color(22, 163, 74));
            lblCashIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblCashText = new JLabel("Thanh toán tiền mặt trực tiếp tại quầy", JLabel.CENTER);
            lblCashText.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblCashText.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblCashSub = new JLabel("Vui lòng nhận tiền từ khách trước khi xuất vé.", JLabel.CENTER);
            lblCashSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblCashSub.setForeground(Color.GRAY);
            lblCashSub.setAlignmentX(Component.CENTER_ALIGNMENT);

            cashGuide.add(Box.createVerticalStrut(40));
            cashGuide.add(lblCashIcon);
            cashGuide.add(Box.createVerticalStrut(10));
            cashGuide.add(lblCashText);
            cashGuide.add(Box.createVerticalStrut(5));
            cashGuide.add(lblCashSub);
            cashGuide.add(Box.createVerticalStrut(12));
            cashGuide.add(new JLabel("Tiền khách đưa (chỉ nhập số VNĐ):"));
            txtCashReceived.setMaximumSize(new Dimension(250, 32));
            cashGuide.add(txtCashReceived);
            cashGuide.add(lblChange);
            JButton exact = new JButton("Khách đưa đủ");
            exact.addActionListener(e -> txtCashReceived.setText(String.format(java.util.Locale.ROOT, "%.0f", currentFinalTotal)));
            cashGuide.add(exact);
            updateChange();
            qrCardContainer.add(cashGuide);
        }
        qrCardContainer.revalidate();
        qrCardContainer.repaint();
    }

    private void applyVoucher() {
        String code = txtVoucher.getText().trim().toUpperCase();
        if (!code.matches("^[A-Za-z0-9]{3,20}$")) {
            JOptionPane.showMessageDialog(this, "Mã Voucher không đúng định dạng (3-20 ký tự chữ và số)!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Voucher v = DataStore.getInstance().getVoucherMap().get(code);
        String error = BookingRules.voucherError(v, subtotal(), java.time.LocalDate.now());
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Không thể áp dụng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        appliedVoucher = v;
        txtVoucher.setEditable(false);
        btnApplyVoucher.setEnabled(false);
        btnRemoveVoucher.setVisible(true);
        recalculateTotal();
    }

    private double subtotal() {
        PricingStrategy strategy = PricingStrategies.determineStrategy(showTime.getStartTime(), chkStudentDiscount.isSelected());
        double tickets = seatCodes.stream().map(showTime.getSeats()::get).filter(java.util.Objects::nonNull)
                .mapToDouble(seat -> strategy.calculatePrice(showTime.getBasePrice(), seat, showTime.getStartTime())).sum();
        return tickets + orderFoods.stream().mapToDouble(OrderItem::getTotalPrice).sum();
    }

    private double cashReceived() {
        String text = txtCashReceived.getText().trim();
        if (!text.matches("[0-9]{1,12}")) return -1;
        return Double.parseDouble(text);
    }

    private void updateChange() {
        if (lblChange == null) return;
        double received = cashReceived();
        lblChange.setText(received < 0 ? "Nhập số tiền hợp lệ." : received < currentFinalTotal
                ? String.format("Còn thiếu: %,.0f VNĐ", currentFinalTotal - received)
                : String.format("Tiền thừa: %,.0f VNĐ", received - currentFinalTotal));
    }

    @Override public void dispose() {
        if (expiryTimer != null) expiryTimer.stop();
        super.dispose();
    }

    private void removeVoucher() {
        appliedVoucher = null;
        txtVoucher.setEditable(true);
        txtVoucher.setText("");
        btnApplyVoucher.setText("Áp dụng");
        btnApplyVoucher.setEnabled(true);
        btnRemoveVoucher.setVisible(false);
        lblVoucherStatus.setText(" ");
        recalculateTotal();
    }

    private void recalculateTotal() {
        PricingStrategy strategy = PricingStrategies.determineStrategy(
                showTime.getStartTime(),
                chkStudentDiscount.isSelected()
        );

        double ticketTotal = 0;
        for (String code : seatCodes) {
            Seat seat = showTime.getSeats().get(code);
            ticketTotal += strategy.calculatePrice(showTime.getBasePrice(), seat, showTime.getStartTime());
        }

        double foodTotal = 0;
        for (OrderItem item : orderFoods) {
            foodTotal += item.getTotalPrice();
        }

        double originalTotal = ticketTotal + foodTotal;
        double voucherDiscount = 0;

        if (appliedVoucher != null) {
            String error = BookingRules.voucherError(appliedVoucher, originalTotal, java.time.LocalDate.now());
            lblVoucherStatus.setText(error == null ? "Mã " + appliedVoucher.getCode() + " đang áp dụng" : error);
            lblVoucherStatus.setForeground(error == null ? new Color(22, 163, 74) : Color.RED);
        }
        if (appliedVoucher != null && BookingRules.voucherError(appliedVoucher, originalTotal, java.time.LocalDate.now()) == null) {
            voucherDiscount = (originalTotal * appliedVoucher.getDiscountPercent()) / 100.0;
            if (voucherDiscount > appliedVoucher.getMaxDiscountAmount()) {
                voucherDiscount = appliedVoucher.getMaxDiscountAmount();
            }
        }

        double afterVoucher = Math.max(0, originalTotal - voucherDiscount);
        pointsDiscountAmount = 0;
        if (chkUseLoyaltyPoints != null && chkUseLoyaltyPoints.isSelected() && currentCustomer != null) {
            pointsDiscountAmount = Math.min(currentCustomer.getLoyaltyPoints(), afterVoucher);
        }

        currentFinalTotal = Math.round(Math.max(0, afterVoucher - pointsDiscountAmount));

        lblTicketSummary.setText(String.format("• Tiền vé (%d ghế - %s): %,.0f VNĐ",
                seatCodes.size(), strategy.getStrategyName(), ticketTotal));
        lblFoodSummary.setText(String.format("• Tiền bắp nước (%d món): %,.0f VNĐ",
                orderFoods.size(), foodTotal));
        lblVoucherDiscount.setText(String.format("• Giảm giá Voucher: -%,.0f VNĐ", voucherDiscount));
        lblPointsDiscountSummary.setText(String.format("• Trừ điểm thành viên: -%,.0f VNĐ", pointsDiscountAmount));
        lblFinalAmount.setText(String.format("TỔNG TIỀN: %,.0f VNĐ", currentFinalTotal));

        updateChange();
        if (rbVietQR != null && rbVietQR.isSelected()) {
            updatePaymentMethodUI();
        }
    }

    private void processPayment() {
        synchronized (DataStore.getInstance()) { processPaymentLocked(); }
    }

    private void processPaymentLocked() {
        if (paymentSuccessful) return;
        DataStore store = DataStore.getInstance();
        if (store.getShowTimeMap().get(showTime.getId()) != showTime
                || !BookingRules.canSell(showTime, LocalDateTime.now())
                || !BookingRules.validHold(showTime, seatCodes, System.currentTimeMillis())) {
            JOptionPane.showMessageDialog(this, "Ghế hết hạn hoặc suất không còn hợp lệ. Vui lòng chọn lại.");
            dispose(); return;
        }
        lookupCustomerByPhone();
        recalculateTotal();
        if (appliedVoucher != null) {
            String error = BookingRules.voucherError(appliedVoucher, subtotal(), java.time.LocalDate.now());
            if (error != null) { JOptionPane.showMessageDialog(this, error + " Hãy hủy hoặc đổi mã."); return; }
        }
        if (rbCash.isSelected() && cashReceived() < currentFinalTotal) {
            JOptionPane.showMessageDialog(this, "Tiền khách đưa chưa đủ hoặc không hợp lệ."); return;
        }
        vn.edu.ptit.cinema.model.cinema.Room room = store.getRoomMap().get(showTime.getRoomId());
        if (room == null) { JOptionPane.showMessageDialog(this, "Không tìm thấy phòng chiếu."); return; }
        String name = txtCustomerName.getText().trim();
        String phone = txtCustomerPhone.getText().trim();

        if (name.length() > 50) {
            JOptionPane.showMessageDialog(this, "Tên khách hàng không được vượt quá 50 ký tự!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            txtCustomerName.requestFocus();
            return;
        }

        if (!phone.isEmpty() && !phone.matches("^0[0-9]{9,10}$")) {
            JOptionPane.showMessageDialog(this,
                    "Số điện thoại không hợp lệ!\nVui lòng nhập số điện thoại Việt Nam bắt đầu bằng số 0 (10-11 chữ số).",
                    "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            txtCustomerPhone.requestFocus();
            return;
        }

        if (name.isEmpty()) name = "Khách Lẻ";

        PricingStrategy strategy = PricingStrategies.determineStrategy(
                showTime.getStartTime(),
                chkStudentDiscount.isSelected()
        );

        double oldPoints = currentCustomer == null ? 0 : currentCustomer.getLoyaltyPoints();
        double oldSpent = currentCustomer == null ? 0 : currentCustomer.getTotalSpent();
        String previousTier = currentCustomer == null ? null : currentCustomer.getMembershipTier();
        java.util.Map<String, Long> holds = new java.util.HashMap<>();
        java.util.Map<String, String> holders = new java.util.HashMap<>();
        // Chuyển trạng thái ghế sang BOOKED (Đỏ)
        List<Ticket> tickets = new ArrayList<>();
        for (String code : seatCodes) {
            Seat seat = showTime.getSeats().get(code);
            holds.put(code, seat.getHoldTimestamp());
            holders.put(code, seat.getHeldByUserId());
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHoldTimestamp(0);
            seat.setHeldByUserId(null);

            double price = strategy.calculatePrice(showTime.getBasePrice(), seat, showTime.getStartTime());
            tickets.add(new Ticket("TK-" + java.util.UUID.randomUUID(), showTime.getId(), code, price));
        }

        Movie movie = store.getMovieMap().get(showTime.getMovieId());

        double ticketTotal = tickets.stream().mapToDouble(Ticket::getPrice).sum();
        double foodTotal = orderFoods.stream().mapToDouble(OrderItem::getTotalPrice).sum();
        double originalTotal = ticketTotal + foodTotal;

        double voucherDiscount = 0;
        if (appliedVoucher != null && originalTotal >= appliedVoucher.getMinOrderAmount()) {
            voucherDiscount = (originalTotal * appliedVoucher.getDiscountPercent()) / 100.0;
            if (voucherDiscount > appliedVoucher.getMaxDiscountAmount()) {
                voucherDiscount = appliedVoucher.getMaxDiscountAmount();
            }
        }
        double totalDiscount = voucherDiscount + pointsDiscountAmount;
        double finalAmount = Math.round(Math.max(0, originalTotal - totalDiscount));

        Invoice invoice = new Invoice(
                tempInvoiceId,
                SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUsername() : "staff",
                name,
                phone,
                showTime.getId(),
                movie != null ? movie.getTitle() : "Phim",
                room.getName(),
                showTime.getStartTime(),
                tickets,
                new ArrayList<>(orderFoods),
                appliedVoucher != null ? appliedVoucher.getCode() : null,
                originalTotal,
                totalDiscount,
                finalAmount,
                LocalDateTime.now()
        );

        invoice.setPaymentDetails(rbCash.isSelected() ? "CASH" : "QR_DEMO",
                rbCash.isSelected() ? cashReceived() : 0,
                rbCash.isSelected() ? cashReceived() - finalAmount : 0);

        // Cập nhật điểm thành viên và kiểm tra tự động nâng hạng hội viên theo tổng chi tiêu
        double pointsEarned = 0;
        boolean upgradedTier = false;
        String oldTier = "";
        if (currentCustomer != null) {
            oldTier = currentCustomer.getMembershipTier();
            if (pointsDiscountAmount > 0) {
                currentCustomer.setLoyaltyPoints(Math.max(0, currentCustomer.getLoyaltyPoints() - pointsDiscountAmount));
            }
            pointsEarned = Math.round(finalAmount * currentCustomer.getPointRate());
            currentCustomer.setLoyaltyPoints(currentCustomer.getLoyaltyPoints() + pointsEarned);
            currentCustomer.setTotalSpent(currentCustomer.getTotalSpent() + finalAmount);
            upgradedTier = !oldTier.equals(currentCustomer.getMembershipTier());
        }

        // Lưu vào RAM và flush xuống file JSON ngay lập tức
        store.getInvoiceMap().put(invoice.getInvoiceId(), invoice);
        if (!store.saveAllData()) {
            store.getInvoiceMap().remove(invoice.getInvoiceId());
            for (String code : seatCodes) {
                Seat seat = showTime.getSeats().get(code);
                seat.setStatus(SeatStatus.PENDING);
                seat.setHoldTimestamp(holds.get(code));
                seat.setHeldByUserId(holders.get(code));
            }
            if (currentCustomer != null) {
                currentCustomer.setLoyaltyPoints(oldPoints);
                currentCustomer.setTotalSpent(oldSpent);
                currentCustomer.setMembershipTier(previousTier);
            }
            JOptionPane.showMessageDialog(this, "Lưu giao dịch thất bại. Chưa xuất vé; hãy kiểm tra quyền ghi/thư mục data.");
            return;
        }
        paymentSuccessful = true;
        if (expiryTimer != null) expiryTimer.stop();

        // In vé ra file text mô phỏng máy in nhiệt POS
        File receipt = ReceiptPrinter.printReceipt(invoice);

        String paymentMethodStr = rbVietQR.isSelected() ? "QR mô phỏng" : "Tiền mặt tại quầy";
        String memberMsg = "";
        if (currentCustomer != null) {
            memberMsg = String.format("\n• Thành viên : %s (+%,.0f điểm thưởng - Hạng: %s)",
                    currentCustomer.getFullName(), pointsEarned, currentCustomer.getMembershipTier());
            if (upgradedTier) {
                memberMsg += String.format("\n• CHÚC MỪNG: Quý khách đã được NÂNG HẠNG từ %s lên %s!", oldTier, currentCustomer.getMembershipTier());
            }
        }

        JOptionPane.showMessageDialog(this,
                "GIAO DỊCH THANH TOÁN THÀNH CÔNG!\n"
                        + "• Mã Hóa Đơn : " + tempInvoiceId + "\n"
                        + "• Phương Thức: " + paymentMethodStr + "\n"
                        + "• Tổng Tiền  : " + String.format("%,.0f VNĐ", finalAmount)
                        + memberMsg + "\n"
                        + (receipt != null ? "• Đã in hóa đơn POS nhiệt ra: " + receipt.getName() : "Không in được file. Giao dịch đã lưu; có thể in lại từ lịch sử."),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);

        paymentSuccessful = true;
        dispose();
    }

    public boolean isPaymentSuccessful() {
        return paymentSuccessful;
    }
}
