package me.vickychijwani.spectre.event;

public class FileUploadEvent implements ApiCallEvent {

    public final String path;
    public final String mimeType;

    public FileUploadEvent(String path, String mimeType) {
        this.path = path;
        this.mimeType = mimeType;
    }

    @Override
    public void loadCachedData() {
        // no-op; this event cannot be handled with cached data
    }

}
