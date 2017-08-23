package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AuthReqBody {

    public final String grantType;
    public final String clientId;
    public final String clientSecret;

    // @SerializedName is required since the field naming policy is set to lower_case_with_underscores
    @SerializedName("authorizationCode")
    public final String authorizationCode;
    public final String provider;
    @SerializedName("redirectUri")
    public final String redirectUri;

    // non-null if grantType == "password"
    @SerializedName("username")
    public final String email;
    public final String password;

    private AuthReqBody(String grantType, String clientId, String clientSecret,
                        String authorizationCode, String provider, String redirectUri,
                        String email, String password) {
        this.grantType = grantType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationCode = authorizationCode;
        this.provider = provider;
        this.redirectUri = redirectUri;
        this.email = email;
        this.password = password;
    }

    public static AuthReqBody fromPassword(@NonNull String clientSecret, @NonNull String email,
                                           @NonNull String password) {
        return new AuthReqBody(
                "password", "ghost-admin", clientSecret,
                null, null, null,
                email, password);
    }

    public static AuthReqBody fromAuthCode(@NonNull String clientSecret, @NonNull String authCode,
                                           @NonNull String redirectUri) {
        return new AuthReqBody(
                "authorization_code", "ghost-admin", clientSecret,
                authCode, "ghost-oauth2", redirectUri,
                null, null);
    }

    public boolean isGrantTypePassword() {
        return "password".equals(grantType);
    }

}
