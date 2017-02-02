package me.vickychijwani.spectre.event;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FileUploadErrorEvent {

    public final Throwable error;

    public FileUploadErrorEvent(Throwable error) {
        this.error = error;
    }

}
