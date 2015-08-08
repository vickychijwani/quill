package me.vickychijwani.spectre.event;

public class FileUploadedEvent {

    public final String relativeUrl;

    public FileUploadedEvent(String relativeUrl) {
        this.relativeUrl = relativeUrl;
    }

}
