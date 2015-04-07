package me.vickychijwani.spectre.network;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.model.SettingsList;
import me.vickychijwani.spectre.model.UserList;
import me.vickychijwani.spectre.util.AppUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class NetworkService {

    public static final String TAG = "NetworkService";

    private GhostApiService mApi = null;
    private AuthToken mAuthToken = null;
    private GsonConverter mGsonConverter;
    private RequestInterceptor mAuthInterceptor;

    public NetworkService() {
        Crashlytics.log(Log.DEBUG, TAG, "Initializing NetworkService...");
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        mGsonConverter = new GsonConverter(gson);
        mAuthInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (mAuthToken != null) {
                    request.addHeader("Authorization", mAuthToken.getAuthHeader());
                }
            }
        };
    }

    public void start() {
        getBus().register(this);
    }

    public void stop() {
        getBus().unregister(this);
    }

    @Subscribe
    public void onLoginStartEvent(final LoginStartEvent event) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppUtils.pathJoin(event.blogUrl, "ghost/api/v0.1"))
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mAuthInterceptor)
                .setLogLevel(RestAdapter.LogLevel.HEADERS)
                .build();

        final AuthReqBody credentials = new AuthReqBody();
        credentials.username = event.username;
        credentials.password = event.password;

        mApi = restAdapter.create(GhostApiService.class);
        mApi.getAuthToken(credentials, new Callback<AuthToken>() {
            @Override
            public void success(AuthToken authToken, Response response) {
                mAuthToken = authToken;
                Log.d(TAG, "Got new access token = " + mAuthToken.access_token);
                getBus().post(new LoginDoneEvent(event.blogUrl, event.username, event.password));
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new LoginErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onLoadUserEvent(LoadUserEvent event) {
        mApi.getCurrentUser(new Callback<UserList>() {
            @Override
            public void success(UserList userList, Response response) {
                getBus().post(new UserLoadedEvent(userList.users.get(0)));
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onLoadBlogSettingsEvent(LoadBlogSettingsEvent event) {
        mApi.getSettings(new Callback<SettingsList>() {
            @Override
            public void success(SettingsList settingsList, Response response) {
                getBus().post(new BlogSettingsLoadedEvent(settingsList.settings));
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onLoadPostsEvent(LoadPostsEvent event) {
        mApi.getPosts(new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                getBus().post(new PostsLoadedEvent(postList.posts));
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onSavePostEvent(SavePostEvent event) {
        mApi.updatePost(event.post.id, PostList.from(event.post), new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                getBus().post(new PostSavedEvent());
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
            }
        });
    }

    // private methods
    private Bus getBus() {
        return BusProvider.getBus();
    }

}
