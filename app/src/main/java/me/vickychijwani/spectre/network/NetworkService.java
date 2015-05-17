package me.vickychijwani.spectre.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import me.vickychijwani.spectre.event.CreatePostEvent;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadPostEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostLoadedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.event.SyncPostsEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.ETag;
import me.vickychijwani.spectre.model.PendingAction;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.model.PostStubList;
import me.vickychijwani.spectre.model.RefreshReqBody;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.model.SettingsList;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.model.User;
import me.vickychijwani.spectre.model.UserList;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.DateTimeUtils;
import me.vickychijwani.spectre.util.PostUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import rx.Observable;

public class NetworkService {

    public static final String TAG = "NetworkService";

    private Realm mRealm = null;
    private GhostApiService mApi = null;
    private AuthToken mAuthToken = null;
    private OkHttpClient mOkHttpClient = null;
    private GsonConverter mGsonConverter;
    private RequestInterceptor mAuthInterceptor;

    private boolean mbAuthRequestOnGoing = false;
    private ArrayDeque<Object> mApiEventQueue = new ArrayDeque<>();
    private ArrayDeque<Object> mRefreshEventsQueue = new ArrayDeque<>();
    private ArrayDeque<Object> mPostUploadQueue = new ArrayDeque<>();

    public NetworkService() {
        Crashlytics.log(Log.DEBUG, TAG, "Initializing NetworkService...");
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy())
                .create();
        mGsonConverter = new GsonConverter(gson);
        mAuthInterceptor = (request) -> {
            if (mAuthToken != null && mAuthToken.isValid() && ! hasAccessTokenExpired()) {
                request.addHeader("Authorization", mAuthToken.getTokenType() + " " +
                        mAuthToken.getAccessToken());
            }
        };
    }

    public void start(Context context, OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
        getBus().register(this);
        mRealm = Realm.getInstance(context);
        if (AppState.getInstance(context).getBoolean(AppState.Key.LOGGED_IN)) {
            mAuthToken = mRealm.allObjects(AuthToken.class).first();
            String blogUrl = UserPrefs.getInstance(context).getString(UserPrefs.Key.BLOG_URL);
            mApi = buildApiService(blogUrl);
        }
    }

    // I don't know how to call this from the Application class!
    @SuppressWarnings("unused")
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
        if (! mRefreshEventsQueue.isEmpty()) {
            refreshDone(null);
            return;
        }

        Bus bus = getBus();
        mRefreshEventsQueue.addAll(Arrays.asList(
                new LoadUserEvent(true),
                new LoadBlogSettingsEvent(true),
                new SyncPostsEvent(true)
        ));
        Observable.from(mRefreshEventsQueue)
                .forEach(bus::post);
    }

    @Subscribe
    public void onLoadUserEvent(final LoadUserEvent event) {
        if (! event.forceNetworkCall) {
            RealmResults<User> users = mRealm.allObjects(User.class);
            if (users.size() > 0) {
                getBus().post(new UserLoadedEvent(users.first()));
                refreshDone(event);
                return;
            }
            // else no users found in db, force a network call!
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
                refreshDone(event);
                return;
            }
            // no settings found in db, force a network call!
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
            RealmResults<Post> posts = getPostsSorted();
            // if there are no posts, there could be 2 cases:
            // 1. there are actually no posts
            // 2. we just haven't fetched any posts from the server yet (Realm returns an empty list in this case too)
            if (posts.size() > 0) {
                getBus().post(new PostsLoadedEvent(posts));
                refreshDone(event);
                return;
            }
        }

        if (! validateAccessToken(event)) return;
        RealmResults<ETag> etags = mRealm.allObjects(ETag.class);
        String etagStr = "";
        if (etags.size() > 0) {
            etagStr = etags.first().getTag();
            if (etagStr == null) etagStr = "";
        }
        mApi.getPosts(etagStr, new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                // update the stored etag
                Observable.from(response.getHeaders())
                        .takeFirst(h -> "ETag".equals(h.getName()))
                        .forEach(h -> createOrUpdateModel(new ETag(h.getValue())));

                // delete posts that are no longer present on the server
                // this assumes that postList.posts is a list of ALL posts on the server
                RealmResults<Post> cachedPosts = mRealm
                        .where(Post.class)
                        .equalTo("status", Post.DRAFT)
                        .or().equalTo("status", Post.PUBLISHED)
                        .findAll();
                // FIXME time complexity is quadratic in the number of posts!
                Observable.from(cachedPosts)
                        .filter(cached -> !postList.contains(cached.getUuid()))
                        .toList()
                        .forEach(NetworkService.this::deleteModels);

                // make sure drafts have a publishedAt of FAR_FUTURE so they're sorted to the top
                Observable.from(postList.posts)
                        .filter(post -> post.getPublishedAt() == null)
                        .forEach(post -> post.setPublishedAt(DateTimeUtils.FAR_FUTURE));

                // now create / update received posts
                createOrUpdateModel(postList.posts);
                getBus().post(new PostsLoadedEvent(getPostsSorted()));
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
    public void onLoadPostEvent(LoadPostEvent event) {
        Post post = mRealm.where(Post.class).equalTo("uuid", event.uuid).findFirst();
        if (post != null) {
            Post postCopy = new Post(post);   // copy isn't tied to db, so it can be mutated freely
            getBus().post(new PostLoadedEvent(postCopy));
        } else {
            Log.wtf(TAG, "No post with uuid = " + event.uuid + " found!");
        }
    }

    @Subscribe
    public void onCreatePostEvent(final CreatePostEvent event) {
        Post newPost = new Post();
        PostUtils.addPendingAction(newPost, PendingAction.CREATE);
        newPost.setUuid(getTempUniqueId(Post.class));
        createOrUpdateModel(newPost);                    // save the local post to db
        getBus().post(new PostCreatedEvent(newPost));
        getBus().post(new SyncPostsEvent(false));
    }

    @Subscribe
    public void onSyncPostsEvent(final SyncPostsEvent event) {
        final RealmResults<Post> localNewPosts = mRealm.where(Post.class)
                .equalTo("pendingActions.type", PendingAction.CREATE)
                .findAllSorted("uuid", false);
        final RealmResults<Post> localEditedPosts = mRealm.where(Post.class)
                .equalTo("pendingActions.type", PendingAction.EDIT)
                .findAll();

        // nothing to upload
        if (localNewPosts.isEmpty() && localEditedPosts.isEmpty() && event.refreshPosts) {
            LoadPostsEvent loadPostsEvent = new LoadPostsEvent(true);
            mRefreshEventsQueue.add(loadPostsEvent);
            getBus().post(loadPostsEvent);
            refreshDone(event);
            return;
        }

        // keep track of new posts uploaded successfully, so the local copies can be deleted
        List<Post> localNewPostsUploaded = new ArrayList<>();

        final Runnable syncFinishedCB = () -> {
            // delete local copies of only those new posts that were successfully uploaded
            deleteModels(localNewPostsUploaded);
            // if refreshPosts is true, first load from the db, AND only then from the network,
            // to avoid a crash because local posts have been deleted above but are still being
            // displayed, so we need to refresh the UI first
            getBus().post(new PostsLoadedEvent(getPostsSorted()));
            if (event.refreshPosts) {
                LoadPostsEvent loadPostsEvent = new LoadPostsEvent(true);
                mRefreshEventsQueue.add(loadPostsEvent);
                getBus().post(loadPostsEvent);
            }
            refreshDone(event);
        };

        mPostUploadQueue.addAll(localNewPosts);
        mPostUploadQueue.addAll(localEditedPosts);

        // the loop variable is *local* to the loop block, so it can be captured in a closure easily
        // this is unlike JavaScript, in which the same loop variable is mutated
        for (final Post localPost : localNewPosts) {
            if (! validateAccessToken(event)) return;
            mApi.createPost(PostStubList.from(localPost), new Callback<PostList>() {
                @Override
                public void success(PostList postList, Response response) {
                    createOrUpdateModel(postList.posts, () -> {
                        localPost.getPendingActions().clear();
                    });
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    localNewPostsUploaded.add(localPost);
                    // FIXME this is a new post! how do subscribers know which post changed?
                    getBus().post(new PostReplacedEvent(postList.posts.get(0)));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.run();
                }

                @Override
                public void failure(RetrofitError error) {
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.run();
                }
            });
        }

        // the loop variable is *local* to the loop block, so it can be captured in a closure easily
        // this is unlike JavaScript, in which the same loop variable is mutated
        for (final Post localPost : localEditedPosts) {
            if (! validateAccessToken(event)) return;
            PostStubList postStubList = PostStubList.from(localPost);
            mApi.updatePost(localPost.getId(), postStubList, new Callback<PostList>() {
                @Override
                public void success(PostList postList, Response response) {
                    createOrUpdateModel(postList.posts, () -> {
                        localPost.getPendingActions().clear();
                    });
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.run();
                }

                @Override
                public void failure(RetrofitError error) {
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.run();
                }
            });
        }
    }

    @Subscribe
    public void onSavePostEvent(SavePostEvent event) {
        PostUtils.addPendingAction(event.post, PendingAction.EDIT);
        for (Tag tag : event.post.getTags()) {
            if (tag.getUuid() == null) {
                tag.setUuid(getTempUniqueId(Tag.class));
                createOrUpdateModel(tag);
            }
        }
        createOrUpdateModel(event.post);                    // save the local post to db
        getBus().post(new PostSavedEvent());
        getBus().post(new SyncPostsEvent(false));
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        // clear all persisted blog data to avoid primary key conflicts
        mRealm.close();
        Realm.deleteRealmFile(event.context);
        mRealm = Realm.getInstance(event.context);
        AppState.getInstance(SpectreApplication.getInstance())
                .setBoolean(AppState.Key.LOGGED_IN, false);
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

        final RefreshReqBody credentials = new RefreshReqBody(mAuthToken.getRefreshToken());
        mbAuthRequestOnGoing = true;
        mApi.refreshAuthToken(credentials, new Callback<AuthToken>() {
            @Override
            public void success(AuthToken authToken, Response response) {
                // since this is a *refreshed* auth token, there is no refresh token in it, so add
                // it manually
                authToken.setRefreshToken(credentials.refreshToken);
                onNewAuthToken(authToken);
            }

            @Override
            public void failure(RetrofitError error) {
                mbAuthRequestOnGoing = false;
                // if the response is 401 Unauthorized, we can recover from it by logging in anew
                // but this should never happen because we first check if the refresh token is valid
                if (error.getResponse() != null &&
                        error.getResponse().getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
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

    private void refreshDone(@Nullable Object sourceEvent) {
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
        Log.d(TAG, "Got new access token = " + authToken.getAccessToken());
        mbAuthRequestOnGoing = false;
        authToken.setCreatedAt(DateTimeUtils.getEpochSeconds());
        mAuthToken = createOrUpdateModel(authToken);
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
                .setClient(new OkClient(mOkHttpClient))
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mAuthInterceptor)
                .setLogLevel(RestAdapter.LogLevel.HEADERS)
                .build();
        return restAdapter.create(GhostApiService.class);
    }

    private RealmResults<Post> getPostsSorted() {
        RealmResults<Post> posts = mRealm.allObjects(Post.class);
        posts.sort(new String[]{ "publishedAt", "updatedAt" }, new boolean[]{ false, false });
        return posts;
    }

    /**
     * Generates a temporary primary key until the actual id is generated by the server. <b>Be
     * careful when calling this in a loop, if you don't save the object before calling it again,
     * you'll get the same id twice!</b>
     */
    @NonNull
    public <T extends RealmObject> String getTempUniqueId(Class<T> clazz) {
        int tempId = Integer.MAX_VALUE;
        while (mRealm.where(clazz).equalTo("uuid", String.valueOf(tempId)).findAll().size() > 0) {
            --tempId;
        }
        return String.valueOf(tempId);
    }

    private <T extends RealmObject> T createOrUpdateModel(T object) {
        return createOrUpdateModel(object, null);
    }

    private <T extends RealmObject> T createOrUpdateModel(T object, @Nullable Runnable transaction) {
        mRealm.beginTransaction();
        T realmObject = mRealm.copyToRealmOrUpdate(object);
        if (transaction != null) {
            transaction.run();
        }
        mRealm.commitTransaction();
        return realmObject;
    }

    private <T extends RealmObject> List<T> createOrUpdateModel(Iterable<T> objects) {
        return createOrUpdateModel(objects, null);
    }

    private <T extends RealmObject> List<T> createOrUpdateModel(Iterable<T> objects,
                                                                @Nullable Runnable transaction) {
        mRealm.beginTransaction();
        List<T> realmObjects = mRealm.copyToRealmOrUpdate(objects);
        if (transaction != null) {
            transaction.run();
        }
        mRealm.commitTransaction();
        return realmObjects;
    }

    private <T extends RealmObject> void deleteModels(List<T> realmObjects) {
        mRealm.beginTransaction();
        Observable.from(realmObjects).forEach(T::removeFromRealm);
        mRealm.commitTransaction();
    }

    private Bus getBus() {
        return BusProvider.getBus();
    }

}
