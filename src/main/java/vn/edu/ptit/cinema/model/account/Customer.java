package vn.edu.ptit.cinema.model.account;

import java.io.Serializable;

/**
 * Model biểu diễn Khách hàng thành viên tại quầy vé (Loyalty Membership)
 * Phục vụ tích điểm thưởng, nâng hạng hội viên và trừ tiền vé trực tiếp.
 */
public class Customer implements Serializable {
    private String phone;            // Khóa chính nhận diện khách (SĐT Việt Nam)
    private String fullName;         // Họ và tên khách hàng
    private double loyaltyPoints;    // Điểm thưởng tích lũy (1 điểm = 1 VNĐ)
    private String membershipTier;   // Hạng thành viên: STANDARD (3%), VIP (5%), DIAMOND (7%)
    private double totalSpent;       // Tổng số tiền đã chi tiêu tại rạp

    public Customer() {
    }

    public Customer(String phone, String fullName, double loyaltyPoints, String membershipTier, double totalSpent) {
        this.phone = phone;
        this.fullName = fullName;
        this.loyaltyPoints = loyaltyPoints;
        this.membershipTier = membershipTier;
        this.totalSpent = totalSpent;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(double loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(String membershipTier) {
        this.membershipTier = membershipTier;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    /**
     * Tỷ lệ tích điểm thưởng dựa trên hạng thành viên
     */
    public double getPointRate() {
        if ("DIAMOND".equalsIgnoreCase(membershipTier)) {
            return 0.07; // 7%
        } else if ("VIP".equalsIgnoreCase(membershipTier)) {
            return 0.05; // 5%
        }
        return 0.03; // STANDARD: 3%
    }
}
