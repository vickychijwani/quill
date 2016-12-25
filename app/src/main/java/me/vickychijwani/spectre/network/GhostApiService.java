package me.vickychijwani.spectre.network;

import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.network.entity.PostList;
import me.vickychijwani.spectre.network.entity.PostStubList;
import me.vickychijwani.spectre.network.entity.RefreshReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.SettingsList;
import me.vickychijwani.spectre.network.entity.UserList;
import retrofit.Callback;
import retrofit.ResponseCallback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

interface GhostApiService {

    // auth
    @GET("/ghost/")
    void getLoginPage(ResponseCallback cb);

    @POST("/authentication/token/")
    void getAuthToken(@Body AuthReqBody credentials, Callback<AuthToken> cb);

    @POST("/authentication/token/")
    void refreshAuthToken(@Body RefreshReqBody credentials, Callback<AuthToken> cb);

    @POST("/authentication/revoke/")
    void revokeAuthToken(@Body RevokeReqBody revoke, JSONObjectCallback cb);

    // users
    @GET("/users/me/?include=roles&status=all")
    void getCurrentUser(@Header("If-None-Match") String etag, Callback<UserList> cb);

    // posts
    // FIXME (issue #81) only allowing N posts right now to avoid too much data transfer
    @GET("/posts/?status=all&staticPages=all&include=tags")
    void getPosts(@Header("If-None-Match") String etag, @Query("limit") int numPosts,
                  Callback<PostList> cb);

    @GET("/posts/{id}/?status=all&include=tags")
    void getPost(@Path("id") int id, Callback<PostList> cb);

    @POST("/posts/?include=tags")
    void createPost(@Body PostStubList posts, Callback<PostList> cb);

    @PUT("/posts/{id}/?include=tags")
    void updatePost(@Path("id") int id, @Body PostStubList posts, Callback<PostList> cb);

    @DELETE("/posts/{id}/")
    void deletePost(@Path("id") int id, ResponseCallback cb);

    // settings / configuration
    @GET("/settings/?type=blog")
    void getSettings(@Header("If-None-Match") String etag, Callback<SettingsList> cb);

    @GET("/configuration/")
    void getConfiguration(@Header("If-None-Match") String etag, Callback<ConfigurationList> cb);

    @GET("/configuration/about/")
    void getVersion(JSONObjectCallback cb);

    // file upload
    @Multipart
    @POST("/uploads/")
    void uploadFile(@Part("uploadimage") TypedFile file, Callback<String> cb);

}
