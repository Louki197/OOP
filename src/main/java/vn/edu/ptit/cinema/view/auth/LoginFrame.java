package vn.edu.ptit.cinema.view.auth;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.account.User;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.service.SessionManager;
import vn.edu.ptit.cinema.view.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * [LOGIN FRAME - JAVA SWING + FLATLAF]
 * Cửa sổ đăng nhập bóng bẩy chuẩn thực tế, tích hợp phân quyền Admin/Staff.
 * Tuân thủ cấu trúc giáo trình OOP (Slide 79 - 82).
 */
@SuppressWarnings("serial")
public class LoginFrame extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;
    private JCheckBox chkShowPassword;

    public LoginFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("ĐĂNG NHẬP HỆ THỐNG - RẠP CHIẾU PHIM PTIT");
        setSize(480, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Bố cục tổng thể BorderLayout (Slide 60 - 65)
        JPanel rootPanel = new JPanel(new BorderLayout(0, 15));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        // 1. Header Logo & Tiêu đề
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblLogo = new JLabel("PTIT CINEMA", JLabel.CENTER);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblLogo.setForeground(new Color(220, 38, 38)); // Màu đỏ cinema đặc trưng

        JLabel lblSub = new JLabel("Hệ Thống Quản Lý Bán Vé Toàn Diện", JLabel.CENTER);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(Color.GRAY);

        headerPanel.add(lblLogo);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(lblSub);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Form Đăng nhập (Dùng BoxLayout Y_AXIS - Slide 75 - 78)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // Tài khoản
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập admin hoặc staff01");
        txtUsername.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 6,10,6,10;");

        // Mật khẩu
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 6,10,6,10; showRevealButton: true;");

        // Gợi ý tài khoản nhanh
        JPanel tipPanel = new JPanel(new GridLayout(2, 1, 3, 3));
        tipPanel.setBorder(BorderFactory.createTitledBorder("Tài khoản mẫu thử nghiệm (DataStore)"));
        JLabel tip1 = new JLabel(" • Quản trị (Admin): admin / admin123");
        JLabel tip2 = new JLabel(" • Nhân viên (Staff): staff01 / 123456");
        tip1.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tip2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tipPanel.add(tip1);
        tipPanel.add(tip2);

        // Hiển thị mật khẩu checkbox
        chkShowPassword = new JCheckBox("Hiện mật khẩu");
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });

        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(lblUser);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(txtUsername);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(lblPass);
        formPanel.add(Box.createVerticalStrut(6));
        formPanel.add(txtPassword);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(chkShowPassword);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(tipPanel);

        rootPanel.add(formPanel, BorderLayout.CENTER);

        // 3. Nút bấm Action (Login & Exit)
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(220, 38, 38));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 8,16,8,16;");
        btnLogin.addActionListener(this);

        btnExit = new JButton("Thoát");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnExit.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 8,16,8,16;");
        btnExit.addActionListener(e -> System.exit(0));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnExit);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Hỗ trợ Enter đăng nhập
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        setContentPane(rootPanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLogin) {
            performLogin();
        }
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiểm tra tài khoản từ DataStore In-Memory
        User user = DataStore.getInstance().getUserMap().get(username);
        if (user != null && user.getPassword().equals(password)) {
            if (!user.isActive()) {
                JOptionPane.showMessageDialog(this, "Tài khoản của bạn đã bị vô hiệu hóa!", "Thông báo", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ghi nhận phiên làm việc vào SessionManager Singleton
            SessionManager.getInstance().login(user);

            JOptionPane.showMessageDialog(this,
                    "Đăng nhập thành công!\nXin chào " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            this.dispose(); // Đóng LoginFrame

            // Mở MainFrame giao diện rạp chiếu phim
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });

        } else {
            JOptionPane.showMessageDialog(this,
                    "Tên đăng nhập hoặc mật khẩu không chính xác!",
                    "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }
}
