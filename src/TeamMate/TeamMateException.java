package TeamMate;

/**
 * General-purpose custom exception for application-level business logic errors in TeamMate.
 * Extends Exception to enforce checked exception handling for critical validation points.
 */
public class TeamMateException extends Exception {

    /**
     * Constructs exception with a detail message.
     * @param message The specific error detail.
     */
    public TeamMateException(String message) {
        super(message);
    }

    /**
     * Constructs exception with a detail message and a root cause.
     * @param message The specific error detail.
     * @param cause The underlying exception that triggered this error.
     */
    public TeamMateException(String message, Throwable cause) {
        super(message, cause);
    }
}