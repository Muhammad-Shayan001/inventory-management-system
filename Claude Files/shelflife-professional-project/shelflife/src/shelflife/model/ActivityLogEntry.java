package shelflife.model;

import java.time.LocalDateTime;

/**
 * Audit log entry for tracking system operations, user accountability,
 * and data modifications.
 */
public class ActivityLogEntry {
    private String logId;
    private LocalDateTime timestamp;
    private String username;
    private String action;
    private String details;
    private String recordId;

    public ActivityLogEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLogEntry(String logId, LocalDateTime timestamp, String username, String action, String details, String recordId) {
        this.logId = logId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.username = username != null ? username : "SYSTEM";
        this.action = action != null ? action : "UNKNOWN";
        this.details = details != null ? details : "";
        this.recordId = recordId != null ? recordId : "-";
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
}
