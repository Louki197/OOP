package vn.edu.ptit.cinema.view.component;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;

/**
 * [CUSTOM COMPONENT - 3D CURVED SCREEN]
 * Màn hình chiếu cong 3D hướng về phía khán phòng,
 * tích hợp hiệu ứng ánh sáng phát xạ (Ambient Glow Gradient) chuẩn IMAX.
 * Tinh chỉnh chiều cao 38px để vừa khít màn hình, không cần cuộn chuột.
 */
public class CurvedScreenPanel extends JPanel {

    public CurvedScreenPanel() {
        setPreferredSize(new Dimension(0, 38));
        setMinimumSize(new Dimension(0, 38));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        double startX = w * 0.08;
        double endX = w * 0.92;
        double ctrlX = w * 0.50;
        double startY = 6;
        double ctrlY = 18;

        // 1. Vẽ dải ánh sáng phát xạ (Ambient Light Glow) lan tỏa từ màn hình xuống khán phòng
        Path2D glowPath = new Path2D.Double();
        glowPath.moveTo(startX, startY);
        glowPath.quadTo(ctrlX, ctrlY, endX, startY);
        glowPath.lineTo(endX + 20, h);
        glowPath.lineTo(startX - 20, h);
        glowPath.closePath();

        GradientPaint glowGradient = new GradientPaint(
                (float) ctrlX, (float) ctrlY, new Color(56, 189, 248, 55), // Xanh Cyan sáng
                (float) ctrlX, h, new Color(56, 189, 248, 0)               // Trong suốt dần
        );
        g2.setPaint(glowGradient);
        g2.fill(glowPath);

        // 2. Vẽ viền Màn hình cong kim loại phản quang (Curved Screen Arc)
        QuadCurve2D screenCurve = new QuadCurve2D.Double(startX, startY, ctrlX, ctrlY, endX, startY);

        // Đổ bóng màn chiếu (Shadow)
        g2.setColor(new Color(15, 23, 42, 35));
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new QuadCurve2D.Double(startX, startY + 1, ctrlX, ctrlY + 1, endX, startY + 1));

        // Màn chiếu chính màu Cyan Neon rực rỡ
        GradientPaint screenPaint = new GradientPaint(
                (float) startX, (float) startY, new Color(14, 165, 233),
                (float) ctrlX, (float) ctrlY, new Color(240, 249, 255),
                true
        );
        g2.setPaint(screenPaint);
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(screenCurve);

        // 3. Chữ hiển thị "MÀN HÌNH CHIẾU • SCREEN"
        String title = "MÀN HÌNH CHIẾU  •  SCREEN";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(title);
        int textX = (w - textW) / 2;
        int textY = (int) ctrlY + 15;

        // Bóng chữ
        g2.setColor(new Color(15, 23, 42, 40));
        g2.drawString(title, textX + 1, textY + 1);

        // Chữ chính màu xám xanh Slate sang trọng
        g2.setColor(new Color(51, 65, 85));
        g2.drawString(title, textX, textY);

        g2.dispose();
    }
}
