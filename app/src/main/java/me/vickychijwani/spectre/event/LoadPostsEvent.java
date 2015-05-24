package me.vickychijwani.spectre.event;

public class LoadPostsEvent implements ApiCallEvent {

    public final boolean forceNetworkCall;
    public boolean loadCachedData = false;

    public LoadPostsEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

    @Override
    public void loadCachedData() {
        loadCachedData = true;
    }

}
