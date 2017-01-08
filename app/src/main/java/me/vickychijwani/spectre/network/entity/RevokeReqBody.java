package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import me.vickychijwani.spectre.model.entity.AuthToken;

@SuppressWarnings({"WeakerAccess", "unused"})
public class RevokeReqBody {

    private static final String TOKEN_TYPE_ACCESS = "access_token";
    private static final String TOKEN_TYPE_REFRESH = "refresh_token";

    @StringDef({TOKEN_TYPE_ACCESS, TOKEN_TYPE_REFRESH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TokenType {}

    @TokenType
    public final String tokenTypeHint;
    public final String token;
    public final String clientId;
    @Nullable public final String clientSecret;

    /* use static factory method from() instead */
    private RevokeReqBody(@TokenType String tokenTypeHint, String token, @Nullable String clientSecret) {
        this.tokenTypeHint = tokenTypeHint;
        this.token = token;
        this.clientId = "ghost-admin";
        this.clientSecret = clientSecret;
    }

    // client secret may be null for older Ghost version (< 0.7.x)
    public static RevokeReqBody[] from(@NonNull AuthToken authToken, @Nullable String clientSecret) {
        return new RevokeReqBody[] {
                new RevokeReqBody(TOKEN_TYPE_REFRESH, authToken.getRefreshToken(), clientSecret),
                new RevokeReqBody(TOKEN_TYPE_ACCESS, authToken.getAccessToken(), clientSecret)
        };
    }

}
