package com.shopcore.security;

import com.shopcore.enums.Role;
import com.shopcore.exception.UnauthorizedException;
import com.shopcore.model.User;

/** Singleton that remembers who is logged in for the console session. */
public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private volatile User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    public void login(User user) { this.currentUser = user; }
    public void logout() { this.currentUser = null; }
    public boolean isLoggedIn() { return currentUser != null; }
    public boolean isAdmin() { return currentUser != null && currentUser.getRole() == Role.ADMIN; }

    public User getCurrentUser() {
        if (currentUser == null) throw new UnauthorizedException("Please login first");
        return currentUser;
    }

    public void requireAdmin() {
        if (!isAdmin()) throw new UnauthorizedException("Admin access required");
    }
}
