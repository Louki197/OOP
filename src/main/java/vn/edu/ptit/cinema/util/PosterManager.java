package vn.edu.ptit.cinema.util;

import vn.edu.ptit.cinema.model.movie.Movie;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trình quản lý hình ảnh Poster & Bộ nhớ đệm RAM (In-Memory Image Cache)
 * Giải quyết dứt điểm vấn đề giật lag EDT khi vẽ danh sách phim và đảm bảo tỷ lệ chuẩn 2:3.
 */
public class PosterManager {

    // Bộ nhớ đệm ảnh poster đã scale chuẩn kích thước trong RAM
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    // Đánh dấu các file ảnh không tồn tại để không thử đọc lại từ đĩa trên mỗi frame
    private static final Map<String, Boolean> MISSING_FILES = new ConcurrentHashMap<>();
    // Thread pool chạy tải trước ảnh poster trên luồng nền (Asynchronous Preload)
    private static final ExecutorService PRELOAD_EXECUTOR = Executors.newFixedThreadPool(2);

    /**
     * Lấy ảnh poster đã crop & scale sẵn tỷ lệ đẹp từ RAM cache.
     * Hoàn toàn 0ms độ trễ trên EDT nếu đã có trong cache.
     */
    public static BufferedImage getPoster(String path, int targetW, int targetH) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String key = path.trim() + "@" + targetW + "x" + targetH;
        BufferedImage cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (MISSING_FILES.containsKey(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            MISSING_FILES.put(path, Boolean.TRUE);
            return null;
        }

        try {
            BufferedImage orig = ImageIO.read(file);
            if (orig == null) {
                MISSING_FILES.put(path, Boolean.TRUE);
                return null;
            }

            BufferedImage scaled = scaleCenterCrop(orig, targetW, targetH);
            CACHE.put(key, scaled);
            return scaled;
        } catch (Exception ex) {
            MISSING_FILES.put(path, Boolean.TRUE);
            return null;
        }
    }

    /**
     * Center-crop (object-fit: cover) chuẩn tỷ lệ 2:3, không bao giờ bị bẹp hay méo hình.
     */
    public static BufferedImage scaleCenterCrop(BufferedImage src, int targetW, int targetH) {
        if (targetW <= 0) targetW = 200;
        if (targetH <= 0) targetH = 300;

        int srcW = src.getWidth();
        int srcH = src.getHeight();

        double scale = Math.max((double) targetW / srcW, (double) targetH / srcH);
        int scaledW = (int) Math.round(srcW * scale);
        int scaledH = (int) Math.round(srcH * scale);

        int cropX = (scaledW - targetW) / 2;
        int cropY = (scaledH - targetH) / 2;

        BufferedImage output = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(src, -cropX, -cropY, scaledW, scaledH, null);
        g2.dispose();
        return output;
    }

    /**
     * Xóa cache khi người dùng đổi poster phim trong giao diện quản lý
     */
    public static void invalidate(String path) {
        if (path == null) return;
        MISSING_FILES.remove(path);
        CACHE.keySet().removeIf(k -> k.startsWith(path + "@") || k.equals(path));
    }

    public static void clear() {
        CACHE.clear();
        MISSING_FILES.clear();
    }

    /**
     * Tải trước toàn bộ ảnh poster trên luồng nền (Asynchronous Preload)
     */
    public static void preloadAll(Collection<Movie> movies, int targetW, int targetH) {
        if (movies == null) return;
        PRELOAD_EXECUTOR.submit(() -> {
            for (Movie m : movies) {
                if (m.getPosterPath() != null && !m.getPosterPath().trim().isEmpty()) {
                    getPoster(m.getPosterPath(), targetW, targetH);
                }
            }
        });
    }

    /**
     * Vẽ Poster hoặc Placeholder điện ảnh sang trọng đỉnh cao
     */
    public static void drawPosterOrPlaceholder(Graphics2D g2, Movie m, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        RoundRectangle2D roundedShape = new RoundRectangle2D.Double(0, 0, w, h, 16, 16);
        Shape oldClip = g2.getClip();
        g2.setClip(roundedShape);

        BufferedImage img = (m != null) ? getPoster(m.getPosterPath(), w, h) : null;
        if (img != null) {
            g2.drawImage(img, 0, 0, w, h, null);
        } else {
            drawCinematicPlaceholder(g2, m, w, h);
        }

        g2.setClip(oldClip);

        // Viền ngoài bo góc sang trọng
        g2.setColor(new Color(226, 232, 240));
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 16, 16));
    }

    /**
     * Vẽ Placeholder đậm chất điện ảnh: dải phim 35mm, clapperboard, vàng gold, font chữ sang trọng
     */
    private static void drawCinematicPlaceholder(Graphics2D g2, Movie m, int w, int h) {
        // Gradient nền tối điện ảnh sang trọng
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(15, 23, 42),
                0, h, new Color(30, 41, 59)
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Hiệu ứng dải đục lỗ phim 35mm ở 2 bên mép (Film Sprocket Perforations)
        g2.setColor(new Color(255, 255, 255, 22));
        for (int y = 14; y < h - 14; y += 18) {
            g2.fillRoundRect(6, y, 8, 10, 3, 3);
            g2.fillRoundRect(w - 14, y, 8, 10, 3, 3);
        }

        // Vẽ biểu tượng Clapperboard (Bảng quay phim điện ảnh) ở giữa
        int clapW = Math.min(80, w - 40);
        int clapH = 54;
        int clapX = (w - clapW) / 2;
        int clapY = h / 2 - 62;

        // Thân clapperboard
        g2.setColor(new Color(51, 65, 85));
        g2.fillRoundRect(clapX, clapY + 14, clapW, clapH - 14, 6, 6);

        // Nắp clapperboard với sọc nghiêng trắng đen
        g2.setColor(new Color(30, 41, 59));
        g2.fillRoundRect(clapX, clapY, clapW, 16, 4, 4);

        g2.setColor(new Color(241, 245, 249, 190));
        for (int sx = clapX + 8; sx < clapX + clapW - 8; sx += 14) {
            int[] xs = {sx, sx + 6, sx + 2, sx - 4};
            int[] ys = {clapY + 1, clapY + 1, clapY + 15, clapY + 15};
            g2.fillPolygon(xs, ys, 4);
        }

        // Dòng chữ PTIT CINEMA màu vàng ánh kim
        g2.setColor(new Color(245, 158, 11)); // Amber Gold
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        FontMetrics fmGold = g2.getFontMetrics();
        String brand = "★ PTIT CINEMA ★";
        g2.drawString(brand, (w - fmGold.stringWidth(brand)) / 2, clapY + clapH + 24);

        // Tên phim (Tự động ngắt dòng nếu dài)
        String title = (m != null && m.getTitle() != null && !m.getTitle().trim().isEmpty())
                ? m.getTitle().toUpperCase()
                : "PHIM ĐANG CẬP NHẬT";

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        FontMetrics fmTitle = g2.getFontMetrics();

        int startY = clapY + clapH + 52;
        if (fmTitle.stringWidth(title) > w - 36) {
            // Tách thành 2 dòng
            String[] words = title.split("\\s+");
            StringBuilder line1 = new StringBuilder();
            StringBuilder line2 = new StringBuilder();
            for (String word : words) {
                if (fmTitle.stringWidth(line1 + " " + word) < w - 40 && line2.length() == 0) {
                    if (line1.length() > 0) line1.append(" ");
                    line1.append(word);
                } else {
                    if (line2.length() > 0) line2.append(" ");
                    line2.append(word);
                }
            }
            g2.drawString(line1.toString(), (w - fmTitle.stringWidth(line1.toString())) / 2, startY);
            if (line2.length() > 0) {
                String l2 = line2.toString();
                if (fmTitle.stringWidth(l2) > w - 36) {
                    l2 = l2.substring(0, Math.min(l2.length(), 14)) + "...";
                }
                g2.drawString(l2, (w - fmTitle.stringWidth(l2)) / 2, startY + 18);
            }
        } else {
            g2.drawString(title, (w - fmTitle.stringWidth(title)) / 2, startY);
        }

        // Thẻ thông báo "COMING SOON" dưới cùng
        int tagW = 120;
        int tagH = 22;
        int tagX = (w - tagW) / 2;
        int tagY = h - 36;
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRoundRect(tagX, tagY, tagW, tagH, 11, 11);
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fmTag = g2.getFontMetrics();
        String tagText = "COMING SOON";
        g2.drawString(tagText, tagX + (tagW - fmTag.stringWidth(tagText)) / 2, tagY + 15);
    }
}
