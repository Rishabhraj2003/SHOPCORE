package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.enums.Role;
import com.shopcore.enums.UserStatus;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.exception.UnauthorizedException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.User;
import com.shopcore.repository.UserRepository;
import com.shopcore.security.PasswordManager;
import com.shopcore.security.SessionManager;
import com.shopcore.util.IdGenerator;
import com.shopcore.util.InputValidator;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepo;
    private final SessionManager session = SessionManager.getInstance();
    private final AuditLogger audit = AuditLogger.getInstance();

    public AuthService(UserRepository userRepo) { this.userRepo = userRepo; }

    public User register(String name, String email, String password, String phone, String address) {
        return createAccount(name, email, password, phone, address, Role.USER);
    }

    public User registerAdmin(String name, String email, String password, String phone, String address) {
        return createAccount(name, email, password, phone, address, Role.ADMIN);
    }

    // synchronized: two people registering the same email at the same instant must not both succeed
    private synchronized User createAccount(String name, String email, String password, String phone,
                                            String address, Role role) {
        if (!InputValidator.isNotBlank(name)) throw new ShopCoreException("Name is required");
        if (!InputValidator.isValidEmail(email)) throw new ShopCoreException("Enter a valid email address");
        if (!InputValidator.isValidPhone(phone)) throw new ShopCoreException("Phone must be a valid 10-digit mobile number");
        if (!InputValidator.isStrongPassword(password))
            throw new ShopCoreException("Password needs at least 8 characters, including a letter and a digit");
        if (!InputValidator.isNotBlank(address)) throw new ShopCoreException("Address is required");

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepo.findByEmail(normalizedEmail).isPresent())
            throw new ShopCoreException("An account with this email already exists");

        String salt = PasswordManager.generateSalt();
        String hash = PasswordManager.hash(password, salt);
        User user = new User(IdGenerator.next(role == Role.ADMIN ? "A" : "U"), name.trim(), normalizedEmail,
                hash, salt, phone.trim(), address.trim(), role);
        userRepo.save(user);
        audit.log(user.getUserId(), role == Role.ADMIN ? AuditAction.ADMIN_CREATED : AuditAction.REGISTER,
                user.getEmail());
        return user;
    }

    public User login(String email, String password) {
        Optional<User> found = userRepo.findByEmail(email == null ? "" : email);
        // same message for "no such user" and "wrong password" so attackers cannot probe for valid emails
        if (found.isEmpty() || !PasswordManager.verify(password, found.get().getSalt(), found.get().getPasswordHash())) {
            audit.log(null, AuditAction.LOGIN_FAILED, email, null, "FAILED");
            throw new UnauthorizedException("Invalid email or password");
        }
        User user = found.get();
        if (user.getStatus() == UserStatus.BLOCKED) {
            audit.log(user.getUserId(), AuditAction.LOGIN_FAILED, user.getEmail(), null, "FAILED: BLOCKED");
            throw new UnauthorizedException("Your account has been blocked. Please contact support.");
        }
        session.login(user);
        audit.log(user.getUserId(), AuditAction.LOGIN, user.getEmail());
        return user;
    }

    public void logout() {
        User user = session.getCurrentUser();
        audit.log(user.getUserId(), AuditAction.LOGOUT, user.getEmail());
        session.logout();
    }

    /** True when at least one admin account exists (used by the first-run setup). */
    public boolean adminExists() {
        return userRepo.findAll().stream().anyMatch(User::isAdmin);
    }
}
