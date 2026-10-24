package vn.edu.ptit.cinema.model.account;

/**
 * Phân quyền hệ thống: ADMIN (Quản lý) và STAFF (Nhân viên bán vé)
 */
public enum Role {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên bán vé");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
