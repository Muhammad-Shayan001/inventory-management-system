package shelflife.service;

/**
 * Exception thrown when a user attempts an operation without sufficient privileges.
 */
public class AccessDeniedException extends RuntimeException {
    private final String resource;
    private final String action;

    public AccessDeniedException(String resource, String action) {
        super("Access denied to " + resource + " for action: " + action);
        this.resource = resource;
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}