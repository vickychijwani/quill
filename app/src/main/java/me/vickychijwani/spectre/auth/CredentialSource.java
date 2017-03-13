package me.vickychijwani.spectre.auth;

import io.reactivex.Observable;
import me.vickychijwani.spectre.util.Pair;

public interface CredentialSource {

    Observable<String> getGhostAuthCode(GhostAuth.Params params);

    Observable<Pair<String, String>> getEmailAndPassword();

}
