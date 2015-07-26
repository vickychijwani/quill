package me.vickychijwani.spectre.event;

public class RefreshDataEvent {

    public final boolean isUserInitiated;
    public final boolean loadCachedData;

    public RefreshDataEvent(boolean isUserInitiated, boolean loadCachedData) {
        this.isUserInitiated = isUserInitiated;
        this.loadCachedData = loadCachedData;
    }

}
