package shelflife.service;

import shelflife.model.Role;
import shelflife.model.User;

import java.util.List;

/**
 * Service contract for user authentication, session state management,
 * and role-based access verification.
 */
public interface AuthService {

    User login(String username, String password) throws Exception;

    void logout();

    User getCurrentUser();

    boolean isLoggedIn();

    boolean hasRole(Role role);

    boolean changePassword(String username, String oldPassword, String newPassword) throws Exception;

    List<User> getAllUsers();

    void createUser(User user, String rawPassword) throws Exception;

    void updateUser(User user) throws Exception;

    /**
     * Persists an already-hashed User object directly.
     * Used when the caller has already generated the salt/hash (e.g. UserManagementDialog).
     */
    default void registerUser(User user) throws Exception {
        updateUser(user);
    }

    /**
     * Finds a user by username (case-insensitive). Returns null if not found.
     */
    default User getUserByUsername(String username) {
        if (username == null) return null;
        for (User u : getAllUsers()) {
            if (username.equalsIgnoreCase(u.getUsername())) return u;
        }
        return null;
    }

    void reload();
}
