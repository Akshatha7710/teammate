package TeamMate;

/**
 * Custom exception for errors occurring within the TeamMateDB (in-memory database).
 * Extends Exception to enforce checked exception handling for database operations.
 */
public class TeamMateDBException extends Exception {

    /**
     * Constructs exception with a message.
     * @param message The specific error detail.
     */
    public TeamMateDBException(String message) {
        super(message);
    }

    /**
     * Constructs exception with a message and a root cause.
     * Useful for wrapping other exceptions (e.g., IOException during file operations).
     * @param message The specific error detail.
     * @param cause The underlying cause.
     */
    public TeamMateDBException(String message, Throwable cause) {
        super(message, cause);
    }
}