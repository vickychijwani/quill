package me.vickychijwani.spectre.network;

import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.PostList;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;

public interface GhostApiService {

    @POST("/authentication/token")
    void getAuthToken(@Body AuthReqBody credentials, Callback<AuthToken> cb);

    @GET("/posts/?status=all&staticPages=all&page=1&include=tags")
    void getPosts(@Header("Authorization") String authorization, Callback<PostList> cb);

}
