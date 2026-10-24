package vn.edu.ptit.cinema.model.booking;

import java.io.Serializable;

/**
 * Item mặt hàng F&B trong hóa đơn
 */
public class OrderItem implements Serializable {
    private String foodId;
    private String foodName;
    private int quantity;
    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(String foodId, String foodName, int quantity, double unitPrice) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalPrice() {
        return quantity * unitPrice;
    }
}
