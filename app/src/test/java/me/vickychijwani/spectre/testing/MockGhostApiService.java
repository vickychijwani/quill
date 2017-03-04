package me.vickychijwani.spectre.testing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.HttpURLConnection;

import io.reactivex.Observable;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.model.entity.ConfigurationParam;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.network.entity.PostList;
import me.vickychijwani.spectre.network.entity.PostStubList;
import me.vickychijwani.spectre.network.entity.RefreshReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.SettingsList;
import me.vickychijwani.spectre.network.entity.UserList;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.Calls;

public class MockGhostApiService implements GhostApiService {

    private final BehaviorDelegate<GhostApiService> mDelegate;
    private final boolean mUseGhostAuth;

    public MockGhostApiService(BehaviorDelegate<GhostApiService> delegate,
                               boolean useGhostAuth) {
        mDelegate = delegate;
        mUseGhostAuth = useGhostAuth;
    }

    @Override
    public Call<String> getLoginPage(@Url String url) {
        return null;
    }

    @Override
    public Call<AuthToken> getAuthTokenV0(@Body AuthReqBody credentials) {
        return null;
    }

    @Override
    public Observable<AuthToken> getAuthToken(@Body AuthReqBody credentials) {
        if (mUseGhostAuth || credentials.password.equals("password")) {
            AuthToken token = new AuthToken();
            token.setAccessToken("access-token");
            token.setRefreshToken("refresh-token");
            token.setCreatedAt(System.currentTimeMillis());
            token.setExpiresIn(60 * 1000);
            return mDelegate
                    .returningResponse(token)
                    .getAuthToken(credentials);
        } else {
            // Mock Retrofit doesn't set the correct request URL, it sets just http://localhost
            String tokenUrl = "http://localhost/authentication/token/";
            ResponseBody body = ResponseBody.create(MediaType.parse("application/json"),
                    "{\"errors\":[{\"message\":\"Wrong password\",\"errorType\":\"UnauthorizedError\"}]}");
            final okhttp3.Response rawResponse = new okhttp3.Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url(tokenUrl).build())
                    .code(HttpURLConnection.HTTP_UNAUTHORIZED)
                    .body(body)
                    .build();
            final Response<Object> res = Response.error(body, rawResponse);
            return mDelegate
                    .returning(Calls.response(res))
                    .getAuthToken(credentials);
        }
    }

    @Override
    public Call<AuthToken> refreshAuthToken(@Body RefreshReqBody credentials) {
        return null;
    }

    @Override
    public Call<JsonElement> revokeAuthToken(@Header("Authorization") String authHeader, @Body RevokeReqBody revoke) {
        return null;
    }

    @Override
    public Call<UserList> getCurrentUser(@Header("Authorization") String authHeader, @Header("If-None-Match") String etag) {
        return null;
    }

    @Override
    public Call<PostList> createPost(@Header("Authorization") String authHeader, @Body PostStubList posts) {
        return null;
    }

    @Override
    public Call<PostList> getPosts(@Header("Authorization") String authHeader, @Header("If-None-Match") String etag, @Query("limit") int numPosts) {
        return null;
    }

    @Override
    public Call<PostList> getPost(@Header("Authorization") String authHeader, @Path("id") String id) {
        return null;
    }

    @Override
    public Call<PostList> updatePost(@Header("Authorization") String authHeader, @Path("id") String id, @Body PostStubList posts) {
        return null;
    }

    @Override
    public Call<String> deletePost(@Header("Authorization") String authHeader, @Path("id") String id) {
        return null;
    }

    @Override
    public Call<SettingsList> getSettings(@Header("Authorization") String authHeader, @Header("If-None-Match") String etag) {
        return null;
    }

    @Override
    public Observable<ConfigurationList> getConfiguration() {
        ConfigurationList config;
        if (mUseGhostAuth) {
            config = ConfigurationList.from(
                    new ConfigurationParam("clientSecret", "client-secret"),
                    new ConfigurationParam("ghostAuthId", "ghost-auth-id"),
                    new ConfigurationParam("ghostAuthUrl", "ghost-auth-url"),
                    new ConfigurationParam("blogUrl", "http://blog.com"));
        } else {
            config = ConfigurationList.from(
                    new ConfigurationParam("clientSecret", "client-secret"));
        }
        return mDelegate
                .returningResponse(config)
                .getConfiguration();
    }

    @Override
    public Call<JsonObject> getVersion(@Header("Authorization") String authHeader) {
        return null;
    }

    @Override
    public Call<JsonElement> uploadFile(@Header("Authorization") String authHeader, @Part MultipartBody.Part file) {
        return null;
    }

}
