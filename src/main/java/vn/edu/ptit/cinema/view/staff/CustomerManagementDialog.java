package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.account.Customer;
import vn.edu.ptit.cinema.service.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collection;

/**
 * [HỆ THỐNG QUẢN LÝ THẺ TÍCH ĐIỂM & ĐĂNG KÝ HỘI VIÊN]
 * Cho phép nhân viên quầy và quản trị viên:
 * 1. Tra cứu danh sách toàn bộ hội viên rạp phim.
 * 2. Đăng ký & cấp thẻ tích điểm mới cho khách hàng với số điện thoại tùy chọn.
 * 3. Tặng điểm chào mừng tự động và lưu trữ vĩnh viễn vào JSON.
 */
public class CustomerManagementDialog extends JDialog {

    private final DataStore dataStore = DataStore.getInstance();
    private JTable tableCustomers;
    private DefaultTableModel modelCustomers;
    private JTextField txtSearch;

    public CustomerManagementDialog(Frame parent) {
        super(parent, "QUẢN LÝ THẺ TÍCH ĐIỂM & THÀNH VIÊN HỘI VIÊN (LOYALTY MEMBERSHIP)", true);
        initUI();
        reloadCustomerTable();
    }

    private void initUI() {
        setSize(820, 560);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(248, 250, 252));

        // 1. Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTitle = new JLabel("DANH SÁCH THẺ TÍCH ĐIỂM & HỘI VIÊN PTIT CINEMA");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Hội viên được tích lũy 5% - 10% giá trị hóa đơn vé & bắp nước để trừ tiền cho các lần xem tiếp theo");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // 2. Toolbar & Table Container
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Thanh công cụ: Tìm kiếm + Nút Cấp thẻ mới
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Nhập số điện thoại, họ tên...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { reloadCustomerTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { reloadCustomerTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { reloadCustomerTable(); }
        });
        searchBox.add(new JLabel("Tìm kiếm:"));
        searchBox.add(txtSearch);

        JButton btnAddNew = new JButton("+ CẤP THẺ TÍCH ĐIỂM MỚI");
        btnAddNew.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddNew.setBackground(new Color(22, 163, 74));
        btnAddNew.setForeground(Color.WHITE);
        btnAddNew.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 6,14,6,14;");
        btnAddNew.addActionListener(e -> showRegisterCustomerDialog());

        toolbar.add(searchBox, BorderLayout.WEST);
        toolbar.add(btnAddNew, BorderLayout.EAST);
        body.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu khách hàng
        String[] columns = {"STT", "Số Điện Thoại", "Họ Và Tên", "Hạng Thẻ", "Điểm Tích Lũy (VNĐ)", "Tổng Chi Tiêu (VNĐ)"};
        modelCustomers = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tableCustomers = new JTable(modelCustomers);
        tableCustomers.setRowHeight(32);
        tableCustomers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableCustomers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableCustomers.getTableHeader().setBackground(new Color(241, 245, 249));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableCustomers.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableCustomers.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableCustomers.getColumnModel().getColumn(1).setPreferredWidth(120);
        tableCustomers.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableCustomers.getColumnModel().getColumn(2).setPreferredWidth(180);
        tableCustomers.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableCustomers.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableCustomers.getColumnModel().getColumn(4).setPreferredWidth(140);
        tableCustomers.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tableCustomers.getColumnModel().getColumn(5).setPreferredWidth(140);
        tableCustomers.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollTable = new JScrollPane(tableCustomers);
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        body.add(scrollTable, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        // 3. Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnClose = new JButton("Đóng lại");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 6,18,6,18;");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }

    private void reloadCustomerTable() {
        modelCustomers.setRowCount(0);
        String keyword = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";

        int stt = 1;
        for (Customer c : dataStore.getCustomerMap().values()) {
            if (!keyword.isEmpty()) {
                boolean matchPhone = c.getPhone() != null && c.getPhone().contains(keyword);
                boolean matchName = c.getFullName() != null && c.getFullName().toLowerCase().contains(keyword);
                if (!matchPhone && !matchName) continue;
            }

            modelCustomers.addRow(new Object[]{
                    stt++,
                    c.getPhone(),
                    c.getFullName(),
                    "⭐ " + c.getMembershipTier(),
                    String.format("%,.0f VNĐ", c.getLoyaltyPoints()),
                    String.format("%,.0f VNĐ", c.getTotalSpent())
            });
        }
    }

    /**
     * HỘP THOẠI ĐĂNG KÝ THẺ TÍCH ĐIỂM MỚI
     */
    public void showRegisterCustomerDialog() {
        JDialog dlg = new JDialog(this, "ĐĂNG KÝ THẺ TÍCH ĐIỂM HỘI VIÊN MỚI", true);
        dlg.setSize(440, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(15, 15));
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel pnlForm = new JPanel(new GridLayout(4, 2, 10, 14));
        pnlForm.setBorder(new EmptyBorder(20, 25, 10, 25));
        pnlForm.setOpaque(false);

        JTextField txtPhone = new JTextField();
        txtPhone.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ví dụ: 0987654321");
        JTextField txtName = new JTextField();
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập họ và tên khách hàng");
        JComboBox<String> cbTier = new JComboBox<>(new String[]{"STANDARD (Tích 5%)", "VIP (Tích 10%)"});
        JTextField txtBonus = new JTextField("10000"); // Tặng 10k mặc định

        pnlForm.add(new JLabel("Số Điện Thoại (*):"));
        pnlForm.add(txtPhone);
        pnlForm.add(new JLabel("Họ Và Tên (*):"));
        pnlForm.add(txtName);
        pnlForm.add(new JLabel("Hạng Thẻ Ban Đầu:"));
        pnlForm.add(cbTier);
        pnlForm.add(new JLabel("Điểm Tặng Khởi Tạo (VNĐ):"));
        pnlForm.add(txtBonus);

        dlg.add(pnlForm, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        pnlBtns.setBackground(new Color(248, 250, 252));
        pnlBtns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = new JButton("Xác Nhận Cấp Thẻ");
        btnSave.setBackground(new Color(22, 163, 74));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.addActionListener(e -> {
            String phone = txtPhone.getText().trim();
            String name = txtName.getText().trim();
            if (!phone.matches("^0[0-9]{9,10}$")) {
                JOptionPane.showMessageDialog(dlg, "Số điện thoại không hợp lệ! Vui lòng nhập 10 chữ số bắt đầu bằng 0.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập họ tên khách hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (dataStore.getCustomerMap().containsKey(phone)) {
                JOptionPane.showMessageDialog(dlg, "Số điện thoại " + phone + " đã được đăng ký thẻ trước đó!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double bonus = 10000.0;
            try {
                bonus = Double.parseDouble(txtBonus.getText().trim());
            } catch (NumberFormatException ignored) {}

            String tier = cbTier.getSelectedIndex() == 1 ? "VIP" : "STANDARD";
            Customer newCust = new Customer(phone, name, bonus, tier, 0.0);
            dataStore.getCustomerMap().put(phone, newCust);
            dataStore.saveAllData();

            JOptionPane.showMessageDialog(dlg, String.format("CẤP THẺ HỘI VIÊN THÀNH CÔNG!\nKhách hàng: %s\nSĐT: %s\nĐiểm tặng chào mừng: %,.0f VNĐ", name, phone, bonus), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dlg.dispose();
            reloadCustomerTable();
        });

        pnlBtns.add(btnCancel);
        pnlBtns.add(btnSave);
        dlg.add(pnlBtns, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
