package me.vickychijwani.spectre.event;

public class SyncPostsEvent implements ApiCallEvent {

    public final boolean forceNetworkCall;
    public boolean loadCachedData = false;

    public SyncPostsEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

    @Override
    public void loadCachedData() {
        loadCachedData = true;
    }

}
