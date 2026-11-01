package shelflife.service;

import shelflife.model.ActivityLogEntry;
import shelflife.storage.DataStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe audit trail implementation appending operational logs to CSV persistence.
 */
public class ActivityLogServiceImpl implements ActivityLogService {

    private final DataStore dataStore;
    private final AuthService authService;
    private final List<ActivityLogEntry> logCache = new CopyOnWriteArrayList<>();

    public ActivityLogServiceImpl(DataStore dataStore, AuthService authService) {
        if (dataStore == null) throw new IllegalArgumentException("DataStore cannot be null.");
        this.dataStore = dataStore;
        this.authService = authService;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            logCache.clear();
            List<ActivityLogEntry> loaded = dataStore.loadActivityLogs();
            if (loaded != null) {
                logCache.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Error loading activity logs: " + e.getMessage());
        }
    }

    @Override
    public void log(String action, String details, String recordId) {
        String username = (authService != null && authService.getCurrentUser() != null) ?
                authService.getCurrentUser().getUsername() : "SYSTEM";
        log(username, action, details, recordId);
    }

    @Override
    public synchronized void log(String username, String action, String details, String recordId) {
        String logId = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ActivityLogEntry entry = new ActivityLogEntry(logId, LocalDateTime.now(), username, action, details, recordId);
        logCache.add(entry);
        try {
            dataStore.saveActivityLogs(new ArrayList<>(logCache));
        } catch (Exception e) {
            System.err.println("Failed to persist activity log: " + e.getMessage());
        }
    }

    @Override
    public List<ActivityLogEntry> getAllLogs() {
        List<ActivityLogEntry> copy = new ArrayList<>(logCache);
        Collections.reverse(copy); // most recent first
        return Collections.unmodifiableList(copy);
    }

    @Override
    public List<ActivityLogEntry> getLogsByUser(String username) {
        List<ActivityLogEntry> list = new ArrayList<>();
        if (username == null) return list;
        for (ActivityLogEntry e : logCache) {
            if (e.getUsername().equalsIgnoreCase(username.trim())) {
                list.add(e);
            }
        }
        Collections.reverse(list);
        return list;
    }
}
