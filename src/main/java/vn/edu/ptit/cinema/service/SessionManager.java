package vn.edu.ptit.cinema.service;

import vn.edu.ptit.cinema.model.account.User;

/**
 * [SINGLETON PATTERN - SESSION MANAGER]
 * Quản lý phiên làm việc của người dùng đang đăng nhập vào hệ thống.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == vn.edu.ptit.cinema.model.account.Role.ADMIN;
    }
}
