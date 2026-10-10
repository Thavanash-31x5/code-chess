package app.registration;

public class DuplicateEmailException extends Exception {
    public DuplicateEmailException(String email, Throwable cause) {
        super("An account already exists for email: " + email, cause);
    }
}