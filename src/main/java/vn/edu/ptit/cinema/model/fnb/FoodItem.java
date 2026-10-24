package vn.edu.ptit.cinema.model.fnb;

import java.io.Serializable;

/**
 * Model biểu diễn mặt hàng Bắp - Nước (F&B) hoặc Combo
 */
public class FoodItem implements Serializable {
    private String id;
    private String name;
    private double price;
    private String description;
    private String category; // COMBO, POPCORN, DRINK, SNACK

    public FoodItem() {
    }

    public FoodItem(String id, String name, double price, String description, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
