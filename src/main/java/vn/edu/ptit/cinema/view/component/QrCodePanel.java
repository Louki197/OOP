package vn.edu.ptit.cinema.view.component;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * [CUSTOM COMPONENT - VIETQR PAYMENT CARD]
 * Mô phỏng thẻ chuyển khoản VietQR Napas 247 thực tế với ma trận QR Code sinh động.
 */
@SuppressWarnings("serial")
public class QrCodePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final String invoiceId;
    private final double amount;
    private final boolean[][] qrMatrix;

    public QrCodePanel(String invoiceId, double amount) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.qrMatrix = generateSimulatedQrMatrix(invoiceId);
        setPreferredSize(new Dimension(280, 310));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
    }

    private boolean[][] generateSimulatedQrMatrix(String seedStr) {
        int size = 25;
        boolean[][] matrix = new boolean[size][size];
        Random rand = new Random(seedStr.hashCode());

        // 3 ô vuông định vị góc QR chuẩn quốc tế (Position Finder Patterns)
        drawFinderPattern(matrix, 0, 0);
        drawFinderPattern(matrix, 0, size - 7);
        drawFinderPattern(matrix, size - 7, 0);

        // Sinh dữ liệu ngẫu nhiên cho các ô còn lại
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (isInsideFinder(r, c, size)) continue;
                matrix[r][c] = rand.nextBoolean();
            }
        }
        return matrix;
    }

    private void drawFinderPattern(boolean[][] m, int r0, int c0) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                    m[r0 + r][c0 + c] = true;
                }
            }
        }
    }

    private boolean isInsideFinder(int r, int c, int size) {
        if (r < 8 && c < 8) return true;
        if (r < 8 && c >= size - 8) return true;
        return r >= size - 8 && c < 8;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();

        // 1. Header VietQR Banner
        g2.setColor(new Color(239, 68, 68)); // Đỏ VietQR / Napas
        g2.fillRect(0, 0, w, 36);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("VietQR  |  NAPAS 24/7", 15, 23);

        // 2. Vẽ ma trận QR Code
        int qrSize = 160;
        int startX = (w - qrSize) / 2;
        int startY = 48;
        int matrixLen = qrMatrix.length;
        double cellSize = (double) qrSize / matrixLen;

        g2.setColor(Color.WHITE);
        g2.fillRect(startX - 6, startY - 6, qrSize + 12, qrSize + 12);
        g2.setColor(new Color(203, 213, 225));
        g2.drawRect(startX - 6, startY - 6, qrSize + 12, qrSize + 12);

        g2.setColor(new Color(15, 23, 42)); // Đen xanh mã QR
        for (int r = 0; r < matrixLen; r++) {
            for (int c = 0; c < matrixLen; c++) {
                if (qrMatrix[r][c]) {
                    int x = (int) Math.round(startX + c * cellSize);
                    int y = (int) Math.round(startY + r * cellSize);
                    int sz = (int) Math.ceil(cellSize);
                    g2.fillRect(x, y, sz, sz);
                }
            }
        }

        // 3. Thông tin tài khoản thụ hưởng
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(30, 41, 59));
        drawCenteredString(g2, "RẠP CHIẾU PHIM PTIT COMPLEX", w, startY + qrSize + 22);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(100, 116, 139));
        drawCenteredString(g2, "STK: 1028888888 - VCB HÀ ĐÔNG", w, startY + qrSize + 38);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(220, 38, 38));
        drawCenteredString(g2, String.format("%,.0f VNĐ", amount), w, startY + qrSize + 56);

        g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        g2.setColor(new Color(71, 85, 105));
        drawCenteredString(g2, "Nội dung: " + invoiceId, w, startY + qrSize + 70);

        g2.dispose();
    }

    private void drawCenteredString(Graphics2D g2, String text, int panelWidth, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (panelWidth - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}
