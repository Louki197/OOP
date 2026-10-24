package vn.edu.ptit.cinema.model.booking;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Model biểu diễn mã giảm giá (Voucher)
 */
public class Voucher implements Serializable {
    private String code;
    private double discountPercent;    // Phần trăm giảm (ví dụ: 10 = 10%)
    private double maxDiscountAmount;  // Giảm tối đa (ví dụ: 50,000 VND)
    private double minOrderAmount;      // Đơn tối thiểu áp dụng
    private LocalDate expiryDate;
    private boolean active;

    public Voucher() {
    }

    public Voucher(String code, double discountPercent, double maxDiscountAmount,
                   double minOrderAmount, LocalDate expiryDate, boolean active) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.expiryDate = expiryDate;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
