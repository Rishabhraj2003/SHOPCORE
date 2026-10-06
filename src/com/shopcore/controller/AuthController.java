package com.shopcore.controller;

import com.shopcore.model.User;
import com.shopcore.security.SessionManager;
import com.shopcore.service.AuthService;
import com.shopcore.service.UserService;
import com.shopcore.util.ConsoleUtil;

public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final SessionManager session = SessionManager.getInstance();

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /** Shows the login screen once. Returns false when the user chooses Exit. */
    public boolean showAuthMenu() {
        ConsoleUtil.header("WELCOME TO SHOPCORE");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        int choice = ConsoleUtil.readInt("Choose", 1, 3);
        switch (choice) {
            case 1: ConsoleUtil.safely(this::login); return true;
            case 2: ConsoleUtil.safely(this::register); return true;
            default: return false;
        }
    }

    private void login() {
        String email = ConsoleUtil.readRequired("Email");
        String password = ConsoleUtil.readPassword("Password");
        User user = authService.login(email, password);
        ConsoleUtil.success("Logged in as " + user.getName() + " (" + user.getRole() + ")");
    }

    private void register() {
        ConsoleUtil.header("REGISTER");
        String name = ConsoleUtil.readRequired("Full name");
        String email = ConsoleUtil.readRequired("Email");
        String phone = ConsoleUtil.readRequired("Phone (10 digits)");
        String address = ConsoleUtil.readRequired("Address");
        String password = ConsoleUtil.readPassword("Password (min 8 chars, letter + digit)");
        User user = authService.register(name, email, password, phone, address);
        ConsoleUtil.success("Account created. Your user id is " + user.getUserId() + ". You can login now.");
    }

    public void logout() {
        authService.logout();
        ConsoleUtil.success("Logged out.");
    }

    public void profileMenu() {
        while (true) {
            User user = userService.getById(session.getCurrentUser().getUserId());
            ConsoleUtil.header("MY PROFILE");
            System.out.println("User ID : " + user.getUserId());
            System.out.println("Name    : " + user.getName());
            System.out.println("Email   : " + user.getEmail());
            System.out.println("Phone   : " + user.getPhone());
            System.out.println("Address : " + user.getAddress());
            System.out.println();
            System.out.println("1. Update name / phone / address");
            System.out.println("2. Change password");
            System.out.println("3. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 3);
            if (choice == 3) return;
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    String name = ConsoleUtil.readLine("New name (Enter to keep)");
                    String phone = ConsoleUtil.readLine("New phone (Enter to keep)");
                    String address = ConsoleUtil.readLine("New address (Enter to keep)");
                    userService.updateProfile(user.getUserId(), name, phone, address);
                    ConsoleUtil.success("Profile updated.");
                } else {
                    String oldPw = ConsoleUtil.readPassword("Current password");
                    String newPw = ConsoleUtil.readPassword("New password");
                    userService.changePassword(user.getUserId(), oldPw, newPw);
                    ConsoleUtil.success("Password changed.");
                }
            });
        }
    }
}
