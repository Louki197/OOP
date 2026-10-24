package vn.edu.ptit.cinema.model.cinema;

import java.io.Serializable;

/**
 * Model biểu diễn thông tin Phòng chiếu
 */
public class Room implements Serializable {
    private String id;
    private String name;
    private int totalRows;
    private int totalCols;

    public Room() {
    }

    public Room(String id, String name, int totalRows, int totalCols) {
        this.id = id;
        this.name = name;
        this.totalRows = totalRows;
        this.totalCols = totalCols;
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

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getTotalCols() {
        return totalCols;
    }

    public void setTotalCols(int totalCols) {
        this.totalCols = totalCols;
    }

    public int getTotalSeats() {
        return totalRows * totalCols;
    }
}
