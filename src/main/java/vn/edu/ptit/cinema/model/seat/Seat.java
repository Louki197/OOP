package vn.edu.ptit.cinema.model.seat;

import java.io.Serializable;

/**
 * Model biểu diễn thông tin một chiếc ghế cụ thể trong phòng chiếu
 */
public class Seat implements Serializable {
    private String code;          // Ví dụ: A01, B05, H10
    private int rowIndex;         // Chỉ số hàng (0, 1, 2...)
    private int colIndex;         // Chỉ số cột (0, 1, 2...)
    private SeatType type;        // STANDARD, VIP, SWEETBOX
    private SeatStatus status;    // AVAILABLE, PENDING, BOOKED
    private long holdTimestamp;   // Thời điểm được giữ chỗ (Epoch millis) để tính timeout 5 phút
    private String heldByUserId;  // Tài khoản nhân viên đang giữ ghế

    public Seat() {
    }

    public Seat(String code, int rowIndex, int colIndex, SeatType type) {
        this.code = code;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
        this.holdTimestamp = 0;
        this.heldByUserId = null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
    }

    public SeatType getType() {
        return type;
    }

    public void setType(SeatType type) {
        this.type = type;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public long getHoldTimestamp() {
        return holdTimestamp;
    }

    public void setHoldTimestamp(long holdTimestamp) {
        this.holdTimestamp = holdTimestamp;
    }

    public String getHeldByUserId() {
        return heldByUserId;
    }

    public void setHeldByUserId(String heldByUserId) {
        this.heldByUserId = heldByUserId;
    }
}
