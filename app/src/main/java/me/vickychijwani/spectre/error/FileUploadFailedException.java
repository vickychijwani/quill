package me.vickychijwani.spectre.error;

public final class FileUploadFailedException extends RuntimeException {

    /**
     * @param throwable - the cause of this file upload failure
     */
    public FileUploadFailedException(Throwable throwable) {
        super("FILE UPLOAD FAILED: see previous exception for details", throwable);
    }

    /**
     * @param message a custom String message
     */
    public FileUploadFailedException(String message) {
        super("FILE UPLOAD FAILED: " + message);
    }

}
