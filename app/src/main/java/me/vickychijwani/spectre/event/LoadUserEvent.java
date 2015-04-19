package me.vickychijwani.spectre.event;

public class LoadUserEvent {

    public boolean forceNetworkCall;

    public LoadUserEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

}
