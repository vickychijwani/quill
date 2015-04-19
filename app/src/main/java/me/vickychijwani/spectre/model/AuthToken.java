package me.vickychijwani.spectre.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import me.vickychijwani.spectre.util.DateTimeUtils;

@RealmClass
public class AuthToken extends RealmObject {

    @PrimaryKey   // intentional; there should only ever be one auth token
    private String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
    private int expiresIn;

    /**
     * Approximate time (expressed in epoch seconds) at which this object was created. Might be off
     * by several seconds. Obtained using {@link DateTimeUtils#getEpochSeconds()}. May be wildly
     * inaccurate. Filled in by us, not by Retrofit.
     */
    private long createdAt;

    // NOTE: DO NOT ADD / MODIFY METHODS, SEE https://realm.io/docs/java/#faq
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
