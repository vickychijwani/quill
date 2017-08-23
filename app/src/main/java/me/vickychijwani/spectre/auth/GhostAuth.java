package me.vickychijwani.spectre.auth;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class GhostAuth {

    public static String buildAuthRequestUrl(Params params) {
        return Uri.parse(params.authUrl).buildUpon()
                .appendEncodedPath("oauth2/authorize/")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("type", "signin")
                .appendQueryParameter("client_id", params.ghostAuthId)
                .appendQueryParameter("redirect_uri", params.redirectUri)
                .appendQueryParameter("state", "doesnt_matter")
                .build()
                .toString();
    }

    @Nullable
    public static String extractAuthCodeFromUrl(@NonNull String url) {
        return new UrlQuerySanitizer(url).getValue("code");
    }

    public static class Params {

        public final String blogUrl;
        public final String authUrl;
        public final String ghostAuthId;
        public final String redirectUri;

        Params(String blogUrl, String authUrl, String ghostAuthId, String redirectUri) {
            this.blogUrl = blogUrl;
            this.authUrl = authUrl;
            this.ghostAuthId = ghostAuthId;
            this.redirectUri = redirectUri;
        }

    }

}
