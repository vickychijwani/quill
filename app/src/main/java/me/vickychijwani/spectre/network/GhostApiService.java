package me.vickychijwani.spectre.network;

import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.model.PostStubList;
import me.vickychijwani.spectre.model.RefreshReqBody;
import me.vickychijwani.spectre.model.SettingsList;
import me.vickychijwani.spectre.model.UserList;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

interface GhostApiService {

    // auth
    @POST("/authentication/token")
    void getAuthToken(@Body AuthReqBody credentials, Callback<AuthToken> cb);

    @POST("/authentication/token")
    void refreshAuthToken(@Body RefreshReqBody credentials, Callback<AuthToken> cb);

    // users
    @GET("/users/me/?include=roles&status=all")
    void getCurrentUser(Callback<UserList> cb);

    // posts
    @GET("/posts/?status=all&staticPages=all&limit=all&include=tags")
    void getPosts(@Header("If-None-Match") String etag, Callback<PostList> cb);

    @POST("/posts/?include=tags")
    void createPost(@Body PostStubList posts, Callback<PostList> cb);

    @PUT("/posts/{id}/?include=tags")
    void updatePost(@Path("id") int id, @Body PostList posts, Callback<PostList> cb);

    // settings
    @GET("/settings/?type=blog")
    void getSettings(Callback<SettingsList> cb);

}
