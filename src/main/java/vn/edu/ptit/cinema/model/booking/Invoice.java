package vn.edu.ptit.cinema.model.booking;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn Hóa đơn bán vé & F&B
 */
@SuppressWarnings("serial")
public class Invoice implements Serializable {
    private static final long serialVersionUID = 1L;
    private String invoiceId;
    private String staffUsername;
    private String customerName;
    private String customerPhone;
    private String showTimeId;
    private String movieTitle;
    private String roomName;
    private LocalDateTime showStartTime;

    private List<Ticket> tickets = new ArrayList<>();
    private List<OrderItem> foods = new ArrayList<>();

    private String voucherCode;
    private double originalAmount;
    private double discountAmount;
    private double finalAmount;
    private LocalDateTime createdAt;
    private String paymentMethod;
    private double cashReceived;
    private double changeAmount;

    public void setPaymentDetails(String method, double received, double change) {
        paymentMethod = method;
        cashReceived = received;
        changeAmount = change;
    }

    public String getPaymentMethod() { return paymentMethod; }
    public double getCashReceived() { return cashReceived; }
    public double getChangeAmount() { return changeAmount; }

    public Invoice() {
    }

    public Invoice(String invoiceId, String staffUsername, String customerName, String customerPhone,
                   String showTimeId, String movieTitle, String roomName, LocalDateTime showStartTime,
                   List<Ticket> tickets, List<OrderItem> foods, String voucherCode,
                   double originalAmount, double discountAmount, double finalAmount, LocalDateTime createdAt) {
        this.invoiceId = invoiceId;
        this.staffUsername = staffUsername;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.showTimeId = showTimeId;
        this.movieTitle = movieTitle;
        this.roomName = roomName;
        this.showStartTime = showStartTime;
        this.tickets = tickets != null ? tickets : new ArrayList<>();
        this.foods = foods != null ? foods : new ArrayList<>();
        this.voucherCode = voucherCode;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public void setStaffUsername(String staffUsername) {
        this.staffUsername = staffUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getShowTimeId() {
        return showTimeId;
    }

    public void setShowTimeId(String showTimeId) {
        this.showTimeId = showTimeId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public LocalDateTime getShowStartTime() {
        return showStartTime;
    }

    public void setShowStartTime(LocalDateTime showStartTime) {
        this.showStartTime = showStartTime;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<OrderItem> getFoods() {
        return foods;
    }

    public void setFoods(List<OrderItem> foods) {
        this.foods = foods;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
