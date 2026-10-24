package vn.edu.ptit.cinema.pattern.strategy;

import vn.edu.ptit.cinema.model.seat.Seat;

import java.time.LocalDateTime;

/**
 * [STRATEGY PATTERN - PRICING STRATEGY]
 * Giao diện định nghĩa thuật toán tính giá vé linh hoạt theo thời điểm và đối tượng
 */
public interface PricingStrategy {
    /**
     * @param basePrice Giá gốc suất chiếu
     * @param seat Ghế được chọn (có hệ số ghế VIP / Sweetbox)
     * @param showTime Thời gian chiếu
     * @return Đơn giá cuối cùng của vé
     */
    double calculatePrice(double basePrice, Seat seat, LocalDateTime showTime);
    
    String getStrategyName();
}
