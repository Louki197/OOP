package vn.edu.ptit.cinema.service;

import vn.edu.ptit.cinema.model.booking.Voucher;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

/** Các quy tắc dùng chung cho bán vé tại một quầy. */
public final class BookingRules {
    private BookingRules() {}

    public static boolean canSell(ShowTime show, LocalDateTime now) {
        return show != null && show.getStartTime() != null && show.getStartTime().isAfter(now);
    }

    public static boolean hasBookedSeats(ShowTime show) {
        return show.getSeats() != null && show.getSeats().values().stream()
                .anyMatch(s -> s.getStatus() == SeatStatus.BOOKED);
    }

    public static boolean validHold(ShowTime show, Collection<String> codes, long now) {
        if (codes.isEmpty() || show == null || show.getSeats() == null) return false;
        return codes.stream().allMatch(code -> {
            Seat seat = show.getSeats().get(code);
            return seat != null && seat.getStatus() == SeatStatus.PENDING
                    && seat.getHoldTimestamp() > 0 && now >= seat.getHoldTimestamp()
                    && now - seat.getHoldTimestamp() < DataStore.SEAT_HOLD_TIMEOUT_MILLIS;
        });
    }

    public static String voucherError(Voucher voucher, double amount, LocalDate today) {
        if (voucher == null) return "Mã voucher không tồn tại.";
        if (!voucher.isActive()) return "Mã voucher đã bị vô hiệu hóa.";
        if (voucher.getExpiryDate() == null || voucher.getExpiryDate().isBefore(today))
            return "Mã voucher đã hết hạn.";
        if (amount < voucher.getMinOrderAmount())
            return String.format("Đơn tối thiểu %,.0f VNĐ để dùng mã này.", voucher.getMinOrderAmount());
        if (!Double.isFinite(voucher.getDiscountPercent()) || voucher.getDiscountPercent() <= 0
                || voucher.getDiscountPercent() > 100 || !Double.isFinite(voucher.getMaxDiscountAmount())
                || voucher.getMaxDiscountAmount() <= 0) return "Cấu hình voucher không hợp lệ.";
        return null;
    }

    public static boolean inPeriod(LocalDate date, LocalDate today, String period) {
        if ("Hôm nay".equals(period)) return today.equals(date);
        if ("7 ngày gần nhất".equals(period))
            return date != null && !date.isBefore(today.minusDays(6)) && !date.isAfter(today);
        return true;
    }
}
