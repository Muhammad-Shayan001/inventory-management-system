package shelflife.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diagnostic local error and audit logger.
 * Writes diagnostic entries to `logs/app_error.log`.
 * Automatically sanitizes sensitive data (passwords, PINs, hashes).
 */
public final class AppLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "logs/app_error.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object LOCK = new Object();

    private AppLogger() {}

    public static void info(String message) {
        log("INFO", message, null);
    }

    public static void warn(String message) {
        log("WARN", message, null);
    }

    public static void error(String message) {
        log("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    private static void log(String level, String message, Throwable throwable) {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(LOG_FILE);
            String timestamp = LocalDateTime.now().format(FMT);
            String thread = Thread.currentThread().getName();
            String sanitizedMessage = sanitize(message);

            synchronized (LOCK) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                    pw.println(String.format("[%s] [%s] [%s] %s", timestamp, level, thread, sanitizedMessage));
                    if (throwable != null) {
                        throwable.printStackTrace(pw);
                    }
                    pw.flush();
                }
            }
        } catch (Exception ex) {
            // Failsafe: Never break application execution if logging encounters an issue
            System.err.println("AppLogger fallback: " + message);
        }
    }

    /**
     * Sanitizes sensitive fields like passwords, PINs, and hashes from log strings.
     */
    public static String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)(password|pin|hash|secret|token)=\\S+", "$1=***REDACTED***")
                   .replaceAll("(?i)(password|pin|hash|secret|token)\\s*:\\s*\\S+", "$1: ***REDACTED***");
    }
}
