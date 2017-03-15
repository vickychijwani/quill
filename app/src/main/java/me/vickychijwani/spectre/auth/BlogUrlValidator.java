package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;

import io.reactivex.Observable;

interface BlogUrlValidator {

    /**
     * Check if the given URL points to a valid Ghost blog, and if yes, call
     * the returned Observable's onNext with the valid URL, else call onError.
     *
     * @param blogUrl - a URL to validate (this is guaranteed to be a valid URL)
     * @return an Observable representing the success/failure of the validation
     */
    Observable<String> validate(@NonNull String blogUrl);

}
