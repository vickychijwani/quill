package me.vickychijwani.spectre.auth;

import io.reactivex.Observable;
import me.vickychijwani.spectre.util.Pair;

/**
 * An asynchronous source of credentials that can be used to login to a Ghost blog.
 *
 * Examples of credential sources:
 * - A login screen where the user types in their credentials
 * - A persistent store on disk where credentials have been saved from an earlier login
 */
public interface CredentialSource {

    Observable<String> getGhostAuthCode(GhostAuth.Params params);

    Observable<Pair<String, String>> getEmailAndPassword(PasswordAuth.Params params);

}
