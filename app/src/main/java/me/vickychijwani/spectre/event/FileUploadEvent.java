package me.vickychijwani.spectre.event;

public class FileUploadEvent {

    public final String path;
    public final String mimeType;

    public FileUploadEvent(String path, String mimeType) {
        this.path = path;
        this.mimeType = mimeType;
    }

}
