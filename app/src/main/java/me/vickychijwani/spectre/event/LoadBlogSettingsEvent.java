package me.vickychijwani.spectre.event;

public class LoadBlogSettingsEvent {

    public boolean forceNetworkCall;

    public LoadBlogSettingsEvent(boolean forceNetworkCall) {
        this.forceNetworkCall = forceNetworkCall;
    }

}
