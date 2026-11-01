package shelflife.service;

import shelflife.model.ActivityLogEntry;

import java.util.List;

/**
 * Service contract for system activity logging and user audit trails.
 */
public interface ActivityLogService {

    default void log(String action, String details) {
        log(action, details, "");
    }

    void log(String action, String details, String recordId);

    void log(String username, String action, String details, String recordId);

    List<ActivityLogEntry> getAllLogs();

    List<ActivityLogEntry> getLogsByUser(String username);

    void reload();
}
