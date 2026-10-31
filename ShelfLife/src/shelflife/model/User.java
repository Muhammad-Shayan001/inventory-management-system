package shelflife.model;

import java.util.Objects;

/**
 * System user entity for authentication, role-based access control,
 * and audit trail tracking.
 */
public class User {
    private String userId;
    private String username;
    private String passwordHash;
    private String salt;
    private Role role;
    private String fullName;
    private boolean active;
    private boolean mustChangePassword;

    public User() {
        this.active = true;
        this.mustChangePassword = false;
        this.role = Role.CASHIER;
    }

    public User(String userId, String username, String passwordHash, String salt, Role role, String fullName, boolean active, boolean mustChangePassword) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role != null ? role : Role.CASHIER;
        this.fullName = fullName;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId != null ? userId.trim().toUpperCase() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.CASHIER;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : "";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " - " + role.getDisplayName() + ")";
    }
}
