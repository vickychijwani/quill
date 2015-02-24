package me.vickychijwani.spectre.network;

import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.PostList;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

public interface GhostApiService {

    @POST("/authentication/token")
    void getAuthToken(@Body AuthReqBody credentials, Callback<AuthToken> cb);

    @GET("/posts/?status=all&staticPages=all&page=1&include=tags")
    void getPosts(@Header("Authorization") String authorization, Callback<PostList> cb);

    @PUT("/posts/{id}/?include=tags")
    void updatePost(@Header("Authorization") String authorization, @Path("id") int id,
                    @Body PostList posts, Callback<PostList> cb);

}
