package me.vickychijwani.spectre.event;

import android.support.annotation.NonNull;

public class GhostVersionLoadedEvent {

    public final String version;

    public GhostVersionLoadedEvent(@NonNull String version) {
        this.version = version;
    }

}
