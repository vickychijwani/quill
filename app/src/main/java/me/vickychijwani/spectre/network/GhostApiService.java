package me.vickychijwani.spectre.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.reactivex.Observable;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.network.entity.PostList;
import me.vickychijwani.spectre.network.entity.PostStubList;
import me.vickychijwani.spectre.network.entity.RefreshReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.SettingsList;
import me.vickychijwani.spectre.network.entity.UserList;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GhostApiService {

    @POST("authentication/token/")
    Observable<AuthToken> getAuthToken(@Body AuthReqBody credentials);

    @POST("authentication/token/")
    Observable<AuthToken> refreshAuthToken(@Body RefreshReqBody credentials);

    @POST("authentication/revoke/")
    Observable<JsonElement> revokeAuthToken(@Header("Authorization") String authHeader,
                                            @Body RevokeReqBody revoke);

    // users
    @GET("users/me/?include=roles&status=all")
    Call<UserList> getCurrentUser(@Header("Authorization") String authHeader,
                                  @Header("If-None-Match") String etag);

    // posts
    @POST("posts/?include=tags&formats=mobiledoc,html")
    Call<PostList> createPost(@Header("Authorization") String authHeader,
                              @Body PostStubList posts);

    // FIXME (issue #81) only allowing N posts right now to avoid too much data transfer
    @GET("posts/?status=all&staticPages=all&include=tags&formats=mobiledoc,html")
    Call<PostList> getPosts(@Header("Authorization") String authHeader,
                            @Header("If-None-Match") String etag, @Query("limit") int numPosts);

    @GET("posts/{id}/?status=all&include=tags&formats=mobiledoc,html")
    Call<PostList> getPost(@Header("Authorization") String authHeader, @Path("id") String id);

    @PUT("posts/{id}/?include=tags&formats=mobiledoc,html")
    Call<PostList> updatePost(@Header("Authorization") String authHeader,
                              @Path("id") String id, @Body PostStubList posts);

    @DELETE("posts/{id}/")
    Call<String> deletePost(@Header("Authorization") String authHeader, @Path("id") String id);

    // settings / configuration
    @GET("settings/?type=blog")
    Call<SettingsList> getSettings(@Header("Authorization") String authHeader,
                                   @Header("If-None-Match") String etag);

    @GET("configuration/")
    Observable<ConfigurationList> getConfiguration();

    @GET("configuration/about/")
    Call<JsonObject> getVersion(@Header("Authorization") String authHeader);

    // file upload
    @Multipart
    @POST("uploads/")
    Call<JsonElement> uploadFile(@Header("Authorization") String authHeader,
                                 @Part MultipartBody.Part file);

}
