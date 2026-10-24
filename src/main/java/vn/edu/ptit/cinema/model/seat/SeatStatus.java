package vn.edu.ptit.cinema.model.seat;

/**
 * Trạng thái ghế trên sơ đồ rạp:
 * - AVAILABLE: Ghế trống (Xanh lá)
 * - PENDING: Đang được giữ chỗ và đếm ngược 5 phút (Vàng)
 * - BOOKED: Đã thanh toán thành công (Đỏ)
 */
public enum SeatStatus {
    AVAILABLE("Còn trống"),
    PENDING("Đang giữ chỗ"),
    BOOKED("Đã bán");

    private final String description;

    SeatStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
