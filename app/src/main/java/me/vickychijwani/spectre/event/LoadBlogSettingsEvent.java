package me.vickychijwani.spectre.event;

public class LoadBlogSettingsEvent implements ApiCallEvent {

    public final boolean forceNetworkCall;
    public boolean loadCachedData = false;

    public LoadBlogSettingsEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

    @Override
    public void loadCachedData() {
        loadCachedData = true;
    }

}
