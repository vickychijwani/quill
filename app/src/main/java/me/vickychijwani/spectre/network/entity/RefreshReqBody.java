package me.vickychijwani.spectre.network.entity;

import android.support.annotation.Nullable;

@SuppressWarnings("unused")
public class RefreshReqBody {

    public final String grantType = "refresh_token";
    public final String refreshToken;
    public final String clientId = "ghost-admin";
    @Nullable
    public final String clientSecret;   // nullable since dynamic client secret was
                                        // only added in Ghost 0.7.x

    public RefreshReqBody(String refreshToken, @Nullable String clientSecret) {
        this.refreshToken = refreshToken;
        this.clientSecret = clientSecret;
    }

}
