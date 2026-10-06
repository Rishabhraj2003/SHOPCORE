package com.shopcore.model;

import com.shopcore.enums.Role;
import com.shopcore.enums.UserStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One class for both roles: an admin is simply a User whose role is ADMIN. */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private String name;
    private final String email;
    private String passwordHash;
    private String salt;
    private String phone;
    private String address;
    private final Role role;
    private UserStatus status;
    private final LocalDateTime createdAt;

    public User(String userId, String name, String email, String passwordHash, String salt,
                String phone, String address, Role role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setSalt(String salt) { this.salt = salt; }
    public void setStatus(UserStatus status) { this.status = status; }
}
