package me.vickychijwani.spectre.event;

public class LoadPostsEvent {

    public boolean forceNetworkCall;

    public LoadPostsEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

}
