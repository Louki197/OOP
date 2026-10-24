package vn.edu.ptit.cinema;

import com.formdev.flatlaf.FlatLightLaf;
import vn.edu.ptit.cinema.service.DataStore;
import vn.edu.ptit.cinema.view.auth.LoginFrame;

import javax.swing.*;

/**
 * Điểm khởi chạy ứng dụng (Main Entry Point)
 * Thiết lập Look and Feel hiện đại FlatLaf và mở giao diện Đăng Nhập
 */
public class App {
    public static void main(String[] args) {
        // 1. Kích hoạt Look and Feel FlatLaf hiện đại (bóng bẩy, bo góc)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            // Tinh chỉnh bo góc và kiểu chữ Segoe UI
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
        } catch (Exception ex) {
            System.err.println("Không thể thiết lập FlatLaf: " + ex.getMessage());
        }

        // 2. Khởi tạo kho dữ liệu In-Memory DataStore (Singleton)
        DataStore.getInstance();

        // 3. Khởi chạy giao diện người dùng trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
