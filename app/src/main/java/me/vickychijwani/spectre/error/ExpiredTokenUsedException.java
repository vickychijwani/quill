package me.vickychijwani.spectre.error;

public final class ExpiredTokenUsedException extends RuntimeException {

    /**
     * @param message - a custom message
     */
    public ExpiredTokenUsedException(String message) {
        super("EXPIRED TOKEN USED: " + message);
    }

}
