package me.vickychijwani.spectre.network.entity;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings({"WeakerAccess"})
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
    public final String clientSecret;

    private RevokeReqBody(@TokenType String tokenTypeHint, String token, String clientSecret) {
        this.tokenTypeHint = tokenTypeHint;
        this.token = token;
        this.clientId = "ghost-admin";
        this.clientSecret = clientSecret;
    }

    public static RevokeReqBody fromAccessToken(String token, String clientSecret) {
        return new RevokeReqBody(TOKEN_TYPE_ACCESS, token, clientSecret);
    }

    public static RevokeReqBody fromRefreshToken(String token, String clientSecret) {
        return new RevokeReqBody(TOKEN_TYPE_REFRESH, token, clientSecret);
    }

}
