package TeamMate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AppLogger {
    private static final LinkedList<String> LOGS = new LinkedList<>();
    private static final int MAX_LOGS = 1000;
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void add(String level, String s) {
        String entry = String.format("%s [%s] %s", TF.format(LocalDateTime.now()), level, s);
        synchronized (LOGS) {
            LOGS.addFirst(entry);
            if (LOGS.size() > MAX_LOGS) LOGS.removeLast();
        }
    }

    public static void info(String s) { add("INFO", s); }
    public static void warning(String s) { add("WARN", s); }
    public static void error(String s, Exception e) {
        add("ERROR", s + (e == null ? "" : " -> " + e.getMessage()));
        if (e != null) e.printStackTrace(System.err);
    }
    public static void debug(String s) { add("DEBUG", s); }

    public static List<String> getRecentLogs() {
        synchronized (LOGS) { return Collections.unmodifiableList(LOGS); }
    }
}
