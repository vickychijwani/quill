package me.vickychijwani.spectre.network;

import com.google.gson.JsonElement;

import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.UserList;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;

// FIXME duplicate of GhostApiService, make this DRY after moving to Retrofit2 since it
// FIXME can be used as sync/async without changing the interface
interface SynchronousGhostApiService {

    // auth
    // FIXME this is bogus: the login page is NOT under /ghost/api/v0.1, it's at the root
    // FIXME it still works because Ghost includes the client secret on the 404 page too!
    @GET("/ghost/")
    Response getLoginPage();

    @POST("/authentication/token/")
    AuthToken getAuthToken(@Body AuthReqBody credentials);

    @POST("/authentication/revoke/")
    JsonElement revokeAuthToken(@Header("Authorization") String authHeader,
                                @Body RevokeReqBody revoke);

    // users
    @GET("/users/me/?include=roles&status=all")
    UserList getCurrentUser(@Header("Authorization") String authHeader,
                            @Header("If-None-Match") String etag);

}
