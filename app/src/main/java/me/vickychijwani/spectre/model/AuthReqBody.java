package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AuthReqBody {

    public final String grantType;
    public final String username;
    public final String password;
    public final String clientId;

    public AuthReqBody(@NonNull String username, @NonNull String password) {
        this.grantType = "password";
        this.username = username;
        this.password = password;
        this.clientId = "ghost-admin";
    }

}
