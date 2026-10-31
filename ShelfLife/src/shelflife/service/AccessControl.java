package shelflife.service;

import shelflife.model.User;
import shelflife.model.Role;

/**
 * Helper class for role-based access control.
 * Provides methods to check user permissions and restrict access
 * to admin-only features and sensitive data.
 */
public final class AccessControl {

    private AccessControl() {}

    /**
     * Requires that the user has ADMIN role.
     * Throws AccessDeniedException if the user is not an admin or is null.
     */
    public static void requireAdmin(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Admin privileges required", "requireAdmin");
        }
    }

    /**
     * Requires that the user has the specified role.
     * Throws AccessDeniedException if the user does not have the required role.
     */
    public static void requireRole(User user, Role requiredRole) {
        if (user == null || user.getRole() != requiredRole) {
            throw new AccessDeniedException("Role '" + requiredRole.getDisplayName() + "' required", "requireRole");
        }
    }

    /**
     * Checks if the user has ADMIN role.
     * Returns true if user is admin, false otherwise (including null user).
     */
    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Checks if the user has CASHIER role.
     * Returns true if user is cashier, false otherwise (including null user).
     */
    public static boolean isCashier(User user) {
        return user != null && user.getRole() == Role.CASHIER;
    }
}