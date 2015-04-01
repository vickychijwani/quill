package me.vickychijwani.spectre.model;

public class AuthToken {

    public String token_type;
    public String access_token;
    public String refresh_token;
    public int expires_in;

    public String getAuthHeader() {
        return token_type + " " + access_token;
    }

}
