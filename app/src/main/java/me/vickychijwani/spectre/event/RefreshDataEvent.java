package me.vickychijwani.spectre.event;

public class RefreshDataEvent {

    public final boolean loadCachedData;

    public RefreshDataEvent(boolean loadCachedData) {
        this.loadCachedData = loadCachedData;
    }

}
