package vn.edu.ptit.cinema.model.cinema;

import vn.edu.ptit.cinema.model.seat.Seat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model biểu diễn Suất chiếu phim kèm sơ đồ ghế thời gian thực
 */
public class ShowTime implements Serializable {
    private String id;
    private String movieId;
    private String roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double basePrice;

    // Sơ đồ ghế của suất chiếu (Key: Mã ghế ví dụ "A01", Value: Đối tượng Seat)
    // Dùng ConcurrentHashMap để thread Auto-Save và countdown đọc/ghi an toàn
    private Map<String, Seat> seats = new ConcurrentHashMap<>();

    public ShowTime() {
    }

    public ShowTime(String id, String movieId, String roomId, LocalDateTime startTime,
                    LocalDateTime endTime, double basePrice, Map<String, Seat> seats) {
        this.id = id;
        this.movieId = movieId;
        this.roomId = roomId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        if (seats != null) {
            this.seats = new ConcurrentHashMap<>(seats);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public Map<String, Seat> getSeats() {
        return seats;
    }

    public void setSeats(Map<String, Seat> seats) {
        this.seats = seats != null ? new ConcurrentHashMap<>(seats) : new ConcurrentHashMap<>();
    }
}
