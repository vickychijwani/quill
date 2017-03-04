package me.vickychijwani.spectre.event;

import android.support.annotation.RestrictTo;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusProvider {

    private static Bus sBus = new Bus();

    private BusProvider() {}

    @RestrictTo(RestrictTo.Scope.TESTS)
    public static void setupForTesting() {
        sBus = new Bus(ThreadEnforcer.ANY);
    }

    public static Bus getBus() { return sBus; }

}
