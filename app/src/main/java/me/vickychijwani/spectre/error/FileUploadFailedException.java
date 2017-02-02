package me.vickychijwani.spectre.error;

public final class FileUploadFailedException extends RuntimeException {

    /**
     * @param throwable - the cause of this file upload failure
     */
    public FileUploadFailedException(Throwable throwable) {
        super("FILE UPLOAD FAILED: see previous exception for details", throwable);
    }

}
