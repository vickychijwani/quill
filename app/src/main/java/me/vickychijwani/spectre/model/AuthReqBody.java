package me.vickychijwani.spectre.model;

public class AuthReqBody {

    public final String grant_type = "password";
    public String username;
    public String password;
    public final String client_id = "ghost-admin";

}
