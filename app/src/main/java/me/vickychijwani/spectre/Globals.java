package me.vickychijwani.spectre;

import me.vickychijwani.spectre.network.GhostApiService;

// TODO get rid of this shit!
public class Globals {

    private static Globals sInstance = null;

    public static Globals getInstance() {
        if (sInstance == null) {
            sInstance = new Globals();
        }

        return sInstance;
    }

    public GhostApiService api;

}
