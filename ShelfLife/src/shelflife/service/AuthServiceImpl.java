package shelflife.service;

import shelflife.model.Role;
import shelflife.model.User;
import shelflife.storage.DataStore;
import shelflife.util.PasswordUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe implementation of AuthService using salted SHA-256 password verification.
 */
public class AuthServiceImpl implements AuthService {

    private final DataStore dataStore;
    private final List<User> userCache = new CopyOnWriteArrayList<>();
    private volatile User currentUser = null;

    public AuthServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            userCache.clear();
            List<User> loaded = dataStore.loadUsers();
            if (loaded != null) {
                userCache.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    @Override
    public synchronized User login(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        String normalizedUser = username.trim().toLowerCase();
        for (User u : userCache) {
            if (u.getUsername().equalsIgnoreCase(normalizedUser)) {
                if (!u.isActive()) {
                    throw new SecurityException("User account is deactivated. Contact Administrator.");
                }
                if (PasswordUtil.verifyPassword(password, u.getSalt(), u.getPasswordHash())) {
                    upgradeHashIfNeeded(u, password);
                    this.currentUser = u;
                    return u;
                } else {
                    throw new SecurityException("Invalid username or password.");
                }
            }
        }
        throw new SecurityException("Invalid username or password.");
    }

    /** Re-hashes an account that still uses the old SHA-256 format; failure must never block the login. */
    private void upgradeHashIfNeeded(User u, String password) {
        if (!PasswordUtil.needsUpgrade(u.getPasswordHash())) {
            return;
        }
        try {
            String newSalt = PasswordUtil.generateSalt();
            u.setSalt(newSalt);
            u.setPasswordHash(PasswordUtil.hashPassword(password, newSalt));
            dataStore.saveUsers(new ArrayList<>(userCache));
        } catch (Exception e) {
            System.out.println("Warning: could not upgrade password hash for " + u.getUsername() + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized void logout() {
        this.currentUser = null;
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    @Override
    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    @Override
    public synchronized boolean changePassword(String username, String oldPassword, String newPassword) throws Exception {
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new IllegalArgumentException("New password must be at least 4 characters.");
        }
        String normalizedUser = username.trim().toLowerCase();
        for (User u : userCache) {
            if (u.getUsername().equalsIgnoreCase(normalizedUser)) {
                if (!PasswordUtil.verifyPassword(oldPassword, u.getSalt(), u.getPasswordHash())) {
                    throw new SecurityException("Incorrect current password.");
                }
                String newSalt = PasswordUtil.generateSalt();
                String newHash = PasswordUtil.hashPassword(newPassword, newSalt);
                u.setSalt(newSalt);
                u.setPasswordHash(newHash);
                u.setMustChangePassword(false);

                dataStore.saveUsers(new ArrayList<>(userCache));
                return true;
            }
        }
        throw new IllegalArgumentException("User not found: " + username);
    }

    @Override
    public List<User> getAllUsers() {
        return Collections.unmodifiableList(new ArrayList<>(userCache));
    }

    @Override
    public synchronized void createUser(User user, String rawPassword) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        for (User u : userCache) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + user.getUsername());
            }
        }
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            user.setUserId("USER-" + (System.currentTimeMillis() % 100000));
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(user.getUsername());
        }
        if (user.getRole() == null) {
            user.setRole(Role.CASHIER);
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);
        user.setSalt(salt);
        user.setPasswordHash(hash);
        userCache.add(user);
        dataStore.saveUsers(new ArrayList<>(userCache));
    }

    @Override
    public synchronized void updateUser(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        for (int i = 0; i < userCache.size(); i++) {
            if (userCache.get(i).getUserId().equalsIgnoreCase(user.getUserId())) {
                userCache.set(i, user);
                dataStore.saveUsers(new ArrayList<>(userCache));
                return;
            }
        }
        throw new IllegalArgumentException("User not found: " + user.getUserId());
    }

    @Override
    public synchronized void registerUser(User user) throws Exception {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        // Check for duplicate username
        for (User u : userCache) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + user.getUsername());
            }
        }
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            user.setUserId("USER-" + (System.currentTimeMillis() % 100000));
        }
        userCache.add(user);
        dataStore.saveUsers(new ArrayList<>(userCache));
    }

    @Override
    public User getUserByUsername(String username) {
        if (username == null) return null;
        for (User u : userCache) {
            if (username.equalsIgnoreCase(u.getUsername())) return u;
        }
        return null;
    }
}
