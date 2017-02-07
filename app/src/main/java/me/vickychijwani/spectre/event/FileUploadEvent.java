package me.vickychijwani.spectre.event;

import java.io.InputStream;

public class FileUploadEvent implements ApiCallEvent {

    public final InputStream inputStream;
    public final String mimeType;

    public FileUploadEvent(InputStream inputStream, String mimeType) {
        this.inputStream = inputStream;
        this.mimeType = mimeType;
    }

    @Override
    public void loadCachedData() {
        // no-op; this event cannot be handled with cached data
    }

}
