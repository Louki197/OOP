package vn.edu.ptit.cinema.model.seat;

public enum SeatType {
    STANDARD("Ghế Thường", 1.0),
    VIP("Ghế VIP", 1.25),
    SWEETBOX("Ghế Đôi Sweetbox", 2.0);

    private final String displayName;
    private final double priceMultiplier;

    SeatType(String displayName, double priceMultiplier) {
        this.displayName = displayName;
        this.priceMultiplier = priceMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }
}
