package me.vickychijwani.spectre.error;

public final class LoginFailedException extends RuntimeException {

    /**
     * @param throwable - the cause of this login failure
     */
    public LoginFailedException(Throwable throwable) {
        super("LOGIN FAILED: see previous exception for details", throwable);
    }

    /**
     * @param message a custom String message
     */
    public LoginFailedException(String message) {
        super("LOGIN FAILED: " + message);
    }

}
