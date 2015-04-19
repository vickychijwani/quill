package me.vickychijwani.spectre.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

import hugo.weaving.DebugLog;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.model.RefreshReqBody;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.model.SettingsList;
import me.vickychijwani.spectre.model.User;
import me.vickychijwani.spectre.model.UserList;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.DateTimeUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class NetworkService {

    public static final String TAG = "NetworkService";

    private Realm mRealm = null;
    private GhostApiService mApi = null;
    private AuthToken mAuthToken = null;
    private GsonConverter mGsonConverter;
    private RequestInterceptor mAuthInterceptor;

    private boolean mbAuthRequestOnGoing = false;
    private ArrayDeque<Object> mApiEventQueue = new ArrayDeque<>();
    private ArrayDeque<Object> mRefreshEventsQueue = new ArrayDeque<>();

    public NetworkService() {
        Crashlytics.log(Log.DEBUG, TAG, "Initializing NetworkService...");
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy())
                .create();
        mGsonConverter = new GsonConverter(gson);
        mAuthInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (mAuthToken != null) {
                    request.addHeader("Authorization", mAuthToken.getTokenType() + " " +
                            mAuthToken.getAccessToken());
                }
            }
        };
    }

    public void start(Context context) {
        getBus().register(this);
        mRealm = Realm.getInstance(context);
        if (AppState.getInstance(context).getBoolean(AppState.Key.LOGGED_IN)) {
            mAuthToken = mRealm.allObjects(AuthToken.class).first();
            String blogUrl = UserPrefs.getInstance(context).getString(UserPrefs.Key.BLOG_URL);
            mApi = buildApiService(blogUrl);
        }
    }

    // I don't know how to call this from the Application class!
    public void stop() {
        getBus().unregister(this);
        mRealm.close();
    }

    @Subscribe
    public void onLoginStartEvent(final LoginStartEvent event) {
        if (mbAuthRequestOnGoing) return;

        AuthReqBody credentials = new AuthReqBody(event.username, event.password);
        mApi = buildApiService(event.blogUrl);
        mbAuthRequestOnGoing = true;
        mApi.getAuthToken(credentials, new Callback<AuthToken>() {
            @Override
            public void success(AuthToken authToken, Response response) {
                onNewAuthToken(authToken);
                getBus().post(new LoginDoneEvent(event.blogUrl, event.username, event.password));
            }

            @Override
            public void failure(RetrofitError error) {
                mbAuthRequestOnGoing = false;
                getBus().post(new LoginErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onRefreshDataEvent(RefreshDataEvent event) {
        // do nothing if a refresh is already in progress
        if (! mRefreshEventsQueue.isEmpty()) return;

        Bus bus = getBus();
        mRefreshEventsQueue.addAll(Arrays.asList(
                new LoadUserEvent(true),
                new LoadBlogSettingsEvent(true),
                new LoadPostsEvent(true)
        ));
        for (Object e : mRefreshEventsQueue) {
            bus.post(e);
        }
    }

    @Subscribe
    public void onLoadUserEvent(final LoadUserEvent event) {
        if (! event.forceNetworkCall) {
            RealmResults<User> users = mRealm.allObjects(User.class);
            if (users.size() > 0) {
                getBus().post(new UserLoadedEvent(users.first()));
                return;
            }
        }

        if (! validateAccessToken(event)) return;
        mApi.getCurrentUser(new Callback<UserList>() {
            @Override
            public void success(UserList userList, Response response) {
                createOrUpdateModel(userList.users);
                getBus().post(new UserLoadedEvent(userList.users.get(0)));
                refreshDone(event);
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
                refreshDone(event);
            }
        });
    }

    @Subscribe
    public void onLoadBlogSettingsEvent(final LoadBlogSettingsEvent event) {
        if (! event.forceNetworkCall) {
            RealmResults<Setting> settings = mRealm.allObjects(Setting.class);
            if (settings.size() > 0) {
                getBus().post(new BlogSettingsLoadedEvent(settings));
                return;
            }
        }

        if (! validateAccessToken(event)) return;
        mApi.getSettings(new Callback<SettingsList>() {
            @Override
            public void success(SettingsList settingsList, Response response) {
                createOrUpdateModel(settingsList.settings);
                getBus().post(new BlogSettingsLoadedEvent(settingsList.settings));
                refreshDone(event);
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
                refreshDone(event);
            }
        });
    }

    @Subscribe
    public void onLoadPostsEvent(final LoadPostsEvent event) {
        if (! event.forceNetworkCall) {
            RealmResults<Post> posts = mRealm.allObjects(Post.class);
            if (posts.size() > 0) {
                posts.sort("updatedAt", false);
                getBus().post(new PostsLoadedEvent(posts));
                return;
            }
        }

        if (! validateAccessToken(event)) return;
        mApi.getPosts(new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                createOrUpdateModel(postList.posts);
                RealmResults<Post> posts = mRealm.allObjects(Post.class);
                if (posts.size() > 0) {
                    posts.sort("updatedAt", false);
                    getBus().post(new PostsLoadedEvent(posts));
                }
                refreshDone(event);
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new ApiErrorEvent(error));
                refreshDone(event);
            }
        });
    }

    @Subscribe
    public void onSavePostEvent(SavePostEvent event) {
        mApi.updatePost(event.post.getId(), PostList.from(event.post), new Callback<PostList>() {
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
    private boolean validateAccessToken(@NonNull Object event) {
        boolean valid = ! hasAccessTokenExpired();
        if (! valid) {
            refreshAccessToken(event);
        }
        return valid;
    }

    @DebugLog
    private void refreshAccessToken(@Nullable final Object eventToDefer) {
        mApiEventQueue.addLast(eventToDefer);
        if (mbAuthRequestOnGoing) return;

        // don't waste bandwidth by trying to use an expired refresh token
        if (hasRefreshTokenExpired()) {
            postLoginStartEvent();
            return;
        }

        RefreshReqBody credentials = new RefreshReqBody(mAuthToken.getRefreshToken());
        mbAuthRequestOnGoing = true;
        mApi.refreshAuthToken(credentials, new Callback<AuthToken>() {
            @Override
            public void success(AuthToken authToken, Response response) {
                onNewAuthToken(authToken);
            }

            @Override
            public void failure(RetrofitError error) {
                mbAuthRequestOnGoing = false;
                // if the response is 401 Unauthorized, we can recover from it by logging in anew
                // but this should never happen because we first check if the refresh token is valid
                if (error.getResponse().getStatus() == 401) {
                    postLoginStartEvent();
                    Log.e(TAG, "Expired refresh token used! You're wasting bandwidth / battery!");
                } else {
                    getBus().post(new LoginErrorEvent(error));
                }
            }
        });
    }

    private void flushApiEventQueue() {
        Bus bus = getBus();
        while (! mApiEventQueue.isEmpty()) {
            bus.post(mApiEventQueue.remove());
        }
    }

    private void refreshDone(Object sourceEvent) {
        mRefreshEventsQueue.removeFirstOccurrence(sourceEvent);
        if (mRefreshEventsQueue.isEmpty()) {
            getBus().post(new DataRefreshedEvent());
        }
    }

    @DebugLog
    private void postLoginStartEvent() {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        String username = prefs.getString(UserPrefs.Key.USERNAME);
        String password = prefs.getString(UserPrefs.Key.PASSWORD);
        getBus().post(new LoginStartEvent(blogUrl, username, password));
    }

    @DebugLog
    private void onNewAuthToken(AuthToken authToken) {
        mbAuthRequestOnGoing = false;
        mAuthToken = authToken;
        mAuthToken.setCreatedAt(DateTimeUtils.getEpochSeconds());
        Log.d(TAG, "Got new access token = " + mAuthToken.getAccessToken());
        createOrUpdateModel(mAuthToken);
        AppState.getInstance(SpectreApplication.getInstance())
                .setBoolean(AppState.Key.LOGGED_IN, true);
        flushApiEventQueue();
    }

    private boolean hasAccessTokenExpired() {
        // consider the token as "expired" 60 seconds earlier, because the createdAt timestamp can
        // be off by several seconds
        return DateTimeUtils.getEpochSeconds() > mAuthToken.getCreatedAt() +
                mAuthToken.getExpiresIn() - 60;
    }

    private boolean hasRefreshTokenExpired() {
        // consider the token as "expired" 60 seconds earlier, because the createdAt timestamp can
        // be off by several seconds
        return DateTimeUtils.getEpochSeconds() > mAuthToken.getCreatedAt() + 86400 - 60;
    }

    private GhostApiService buildApiService(@NonNull String blogUrl) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppUtils.pathJoin(blogUrl, "ghost/api/v0.1"))
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mAuthInterceptor)
                .setLogLevel(RestAdapter.LogLevel.HEADERS)
                .build();
        return restAdapter.create(GhostApiService.class);
    }

    private <T extends RealmObject> T createOrUpdateModel(T object) {
        mRealm.beginTransaction();
        T realmObject = mRealm.copyToRealmOrUpdate(object);
        mRealm.commitTransaction();
        return realmObject;
    }

    private <T extends RealmObject> List<T> createOrUpdateModel(Iterable<T> objects) {
        mRealm.beginTransaction();
        List<T> realmObjects = mRealm.copyToRealmOrUpdate(objects);
        mRealm.commitTransaction();
        return realmObjects;
    }

    private Bus getBus() {
        return BusProvider.getBus();
    }

}
