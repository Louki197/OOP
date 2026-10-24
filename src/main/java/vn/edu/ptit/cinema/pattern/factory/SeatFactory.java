package vn.edu.ptit.cinema.pattern.factory;

import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatType;

/**
 * [FACTORY PATTERN - SEAT FACTORY]
 * Tạo lập đối tượng ghế ngồi chuẩn hóa
 */
public class SeatFactory {

    public static Seat createSeat(String code, int row, int col, SeatType type) {
        if (code == null || code.trim().isEmpty()) {
            char rowChar = (char) ('A' + row);
            code = String.format("%c%02d", rowChar, col + 1);
        }
        return new Seat(code, row, col, type);
    }
}
