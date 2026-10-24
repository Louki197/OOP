package vn.edu.ptit.cinema.pattern.strategy;

import vn.edu.ptit.cinema.model.seat.Seat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * [STRATEGY IMPLEMENTATIONS]
 * Các giải thuật tính giá vé theo ngày thường, cuối tuần, sinh viên
 */
public class PricingStrategies {

    // 1. Chiến lược Ngày thường (Thứ 2 - Thứ 6)
    public static class StandardPricing implements PricingStrategy {
        @Override
        public double calculatePrice(double basePrice, Seat seat, LocalDateTime showTime) {
            double multiplier = seat.getType() != null ? seat.getType().getPriceMultiplier() : 1.0;
            return basePrice * multiplier;
        }

        @Override
        public String getStrategyName() {
            return "Giá chuẩn (Ngày thường)";
        }
    }

    // 2. Chiến lược Cuối tuần (Thứ 7, Chủ Nhật phụ thu 20%)
    public static class WeekendPricing implements PricingStrategy {
        @Override
        public double calculatePrice(double basePrice, Seat seat, LocalDateTime showTime) {
            double multiplier = seat.getType() != null ? seat.getType().getPriceMultiplier() : 1.0;
            return basePrice * multiplier * 1.2; // Tăng 20%
        }

        @Override
        public String getStrategyName() {
            return "Cuối tuần (+20%)";
        }
    }

    // 3. Chiến lược Ưu đãi Sinh viên (Giảm 20% trên giá cơ sở)
    public static class StudentPricing implements PricingStrategy {
        @Override
        public double calculatePrice(double basePrice, Seat seat, LocalDateTime showTime) {
            double multiplier = seat.getType() != null ? seat.getType().getPriceMultiplier() : 1.0;
            return (basePrice * 0.8) * multiplier; // Giảm 20%
        }

        @Override
        public String getStrategyName() {
            return "Ưu đãi Sinh viên (-20%)";
        }
    }

    /**
     * Tự động lựa chọn chiến lược phù hợp dựa trên thời gian chiếu
     */
    public static PricingStrategy determineStrategy(LocalDateTime time, boolean isStudent) {
        if (isStudent) {
            return new StudentPricing();
        }
        DayOfWeek day = time.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return new WeekendPricing();
        }
        return new StandardPricing();
    }
}
