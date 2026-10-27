package vn.edu.ptit.cinema.model.account;

import java.io.Serializable;

/**
 * Model biểu diễn Khách hàng thành viên tại quầy vé (Loyalty Membership)
 * Phục vụ tích điểm thưởng, nâng hạng hội viên và trừ tiền vé trực tiếp.
 * 
 * QUY CHUẨN NÂNG HẠNG TỰ ĐỘNG THEO TỔNG CHI TIÊU (TOTAL SPENT):
 * 1. STANDARD : Chi tiêu < 1.000.000 VNĐ (Tích điểm 3%)
 * 2. VIP      : Chi tiêu từ 1.000.000 VNĐ đến dưới 3.000.000 VNĐ (Tích điểm 5%)
 * 3. DIAMOND  : Chi tiêu từ 3.000.000 VNĐ trở lên (Tích điểm 7%)
 */
public class Customer implements Serializable {
    private String phone;            // Khóa chính nhận diện khách (SĐT Việt Nam)
    private String fullName;         // Họ và tên khách hàng
    private double loyaltyPoints;    // Điểm thưởng tích lũy (1 điểm = 1 VNĐ)
    private String membershipTier;   // Hạng thành viên: STANDARD, VIP, DIAMOND
    private double totalSpent;       // Tổng số tiền đã chi tiêu tại rạp

    public Customer() {
    }

    public Customer(String phone, String fullName, double loyaltyPoints, String membershipTier, double totalSpent) {
        this.phone = phone;
        this.fullName = fullName;
        this.loyaltyPoints = loyaltyPoints;
        this.totalSpent = totalSpent;
        this.membershipTier = (membershipTier != null && !membershipTier.trim().isEmpty())
                ? membershipTier.toUpperCase()
                : calculateTier(totalSpent);
        checkAndUpdateTier();
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
        checkAndUpdateTier();
    }

    /**
     * Tự động tính toán Hạng hội viên dựa trên Tổng chi tiêu tích lũy (Total Spent):
     * - STANDARD : Chi tiêu < 1.000.000 VNĐ (Tích 3%)
     * - VIP      : Chi tiêu từ 1.000.000 VNĐ đến dưới 3.000.000 VNĐ (Tích 5%)
     * - DIAMOND  : Chi tiêu từ 3.000.000 VNĐ trở lên (Tích 7%)
     */
    public static String calculateTier(double totalSpent) {
        if (totalSpent >= 3000000.0) {
            return "DIAMOND";
        } else if (totalSpent >= 1000000.0) {
            return "VIP";
        }
        return "STANDARD";
    }

    /**
     * Kiểm tra và tự động cập nhật hạng nếu tổng chi tiêu đạt mốc mới.
     * @return true nếu có sự thay đổi hạng
     */
    public boolean checkAndUpdateTier() {
        String newTier = calculateTier(this.totalSpent);
        if (this.membershipTier == null || !newTier.equalsIgnoreCase(this.membershipTier)) {
            this.membershipTier = newTier;
            return true;
        }
        return false;
    }

    /**
     * Lấy chuỗi thông báo tiến độ nâng hạng tiếp theo
     */
    public String getNextTierProgress() {
        if (this.totalSpent >= 3000000.0) {
            return "Đã đạt hạng cao nhất (DIAMOND)";
        } else if (this.totalSpent >= 1000000.0) {
            double rem = 3000000.0 - this.totalSpent;
            return String.format("Còn %,.0f VNĐ lên DIAMOND", rem);
        } else {
            double rem = 1000000.0 - this.totalSpent;
            return String.format("Còn %,.0f VNĐ lên VIP", rem);
        }
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
