package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AuthReqBody {

    public final String grantType;
    public final String username;
    public final String password;
    public final String clientId;
    public final String clientSecret;   // can be null, since dynamic client secret was only added
                                        // in Ghost 0.7.x

    public AuthReqBody(@NonNull String username, @NonNull String password,
                       @Nullable String clientSecret) {
        this.grantType = "password";
        this.username = username;
        this.password = password;
        this.clientId = "ghost-admin";
        this.clientSecret = clientSecret;
    }

}
