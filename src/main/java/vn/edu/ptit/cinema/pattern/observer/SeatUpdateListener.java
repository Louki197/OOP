package vn.edu.ptit.cinema.pattern.observer;

/**
 * [OBSERVER PATTERN - LISTENER]
 * Interface cho các thành phần giao diện muốn nhận thông báo khi sơ đồ ghế thay đổi
 */
@FunctionalInterface
public interface SeatUpdateListener {
    void onSeatStateChanged(String showTimeId, String seatCode);
}
