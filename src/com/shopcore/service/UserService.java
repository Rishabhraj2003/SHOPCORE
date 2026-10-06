package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.enums.UserStatus;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.exception.UserNotFoundException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.User;
import com.shopcore.repository.UserRepository;
import com.shopcore.security.PasswordManager;
import com.shopcore.util.InputValidator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private final UserRepository userRepo;
    private final AuditLogger audit = AuditLogger.getInstance();

    public UserService(UserRepository userRepo) { this.userRepo = userRepo; }

    public User getById(String userId) {
        return userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    public List<User> getAllUsers() {
        return userRepo.findAll().stream().sorted(Comparator.comparing(User::getUserId)).collect(Collectors.toList());
    }

    public User updateProfile(String userId, String name, String phone, String address) {
        User user = getById(userId);
        if (InputValidator.isNotBlank(name)) user.setName(name.trim());
        if (InputValidator.isNotBlank(phone)) {
            if (!InputValidator.isValidPhone(phone)) throw new ShopCoreException("Phone must be a valid 10-digit mobile number");
            user.setPhone(phone.trim());
        }
        if (InputValidator.isNotBlank(address)) user.setAddress(address.trim());
        userRepo.save(user);
        audit.log(userId, AuditAction.PROFILE_UPDATED, userId);
        return user;
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (!PasswordManager.verify(oldPassword, user.getSalt(), user.getPasswordHash()))
            throw new ShopCoreException("Current password is incorrect");
        if (!InputValidator.isStrongPassword(newPassword))
            throw new ShopCoreException("Password needs at least 8 characters, including a letter and a digit");
        String salt = PasswordManager.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordManager.hash(newPassword, salt));
        userRepo.save(user);
        audit.log(userId, AuditAction.PASSWORD_CHANGED, userId);
    }

    public void setStatus(String adminId, String targetUserId, UserStatus status) {
        User target = getById(targetUserId);
        if (target.isAdmin()) throw new ShopCoreException("Admin accounts cannot be blocked");
        target.setStatus(status);
        userRepo.save(target);
        audit.log(adminId, status == UserStatus.BLOCKED ? AuditAction.USER_BLOCKED : AuditAction.USER_UNBLOCKED, targetUserId);
    }
}
