package com.shopcore.repository;

import com.shopcore.model.User;

import java.util.Optional;

public class UserRepository extends FileRepository<User, String> {
    public UserRepository() { super("users.dat", User::getUserId); }

    public Optional<User> findByEmail(String email) {
        return store.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email.trim())).findFirst();
    }
}
