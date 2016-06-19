package me.vickychijwani.spectre.error;

public final class ExpiredTokenUsedException extends RuntimeException {

    /**
     * @param throwable - the cause of this exception
     */
    public ExpiredTokenUsedException(Throwable throwable) {
        super("EXPIRED TOKEN USED: see previous exception for details", throwable);
    }

}
