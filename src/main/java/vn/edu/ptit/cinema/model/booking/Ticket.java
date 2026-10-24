package vn.edu.ptit.cinema.model.booking;

import java.io.Serializable;

/**
 * Model vé xem phim
 */
public class Ticket implements Serializable {
    private String ticketId;
    private String showTimeId;
    private String seatCode;
    private double price;

    public Ticket() {
    }

    public Ticket(String ticketId, String showTimeId, String seatCode, double price) {
        this.ticketId = ticketId;
        this.showTimeId = showTimeId;
        this.seatCode = seatCode;
        this.price = price;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getShowTimeId() {
        return showTimeId;
    }

    public void setShowTimeId(String showTimeId) {
        this.showTimeId = showTimeId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
