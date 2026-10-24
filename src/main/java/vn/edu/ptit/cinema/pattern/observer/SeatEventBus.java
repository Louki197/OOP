package vn.edu.ptit.cinema.pattern.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * [OBSERVER PATTERN - SUBJECT / EVENT BUS]
 * Quản lý danh sách các Observer và bắn thông báo khi ghế đổi trạng thái
 */
public class SeatEventBus {
    private static final List<SeatUpdateListener> listeners = new CopyOnWriteArrayList<>();

    public static void subscribe(SeatUpdateListener listener) {
        listeners.add(listener);
    }

    public static void unsubscribe(SeatUpdateListener listener) {
        listeners.remove(listener);
    }

    public static void clear() {
        listeners.clear();
    }

    public static void notifySeatChanged(String showTimeId, String seatCode) {
        for (SeatUpdateListener listener : listeners) {
            try {
                listener.onSeatStateChanged(showTimeId, seatCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
