package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;

/**
 * An object that can be listened to or unlistened from.
 */
public interface Listenable<T> {

    /**
     * Start listening to events from this Listenable.
     * @param listener - the listener to be notified
     */
    void listen(@NonNull T listener);

    /**
     * Stop listening to further events. This removes all references this
     * Listenable holds to the listener. The operation should be idempotent.
     */
    void unlisten(@NonNull T listener);

}
