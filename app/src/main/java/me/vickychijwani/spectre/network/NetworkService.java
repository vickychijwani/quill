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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import me.vickychijwani.spectre.BuildConfig;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.ApiCallEvent;
import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.event.ConfigurationLoadedEvent;
import me.vickychijwani.spectre.event.CreatePostEvent;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.FileUploadErrorEvent;
import me.vickychijwani.spectre.event.FileUploadEvent;
import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.ForceCancelRefreshEvent;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadConfigurationEvent;
import me.vickychijwani.spectre.event.LoadPostEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.PasswordChangedEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostLoadedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.event.SyncPostsEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.model.ConfigurationList;
import me.vickychijwani.spectre.model.ConfigurationParam;
import me.vickychijwani.spectre.model.ETag;
import me.vickychijwani.spectre.model.PendingAction;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.model.PostStubList;
import me.vickychijwani.spectre.model.RefreshReqBody;
import me.vickychijwani.spectre.model.RevokeReqBody;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.model.SettingsList;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.model.User;
import me.vickychijwani.spectre.model.UserList;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.DateTimeUtils;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.PostUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.ResponseCallback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.functions.Action1;

public class NetworkService {

    private static final String TAG = "NetworkService";

    private Context mAppContext = null;     // Application context, not Activity context!
    private Realm mRealm = null;
    private GhostApiService mApi = null;
    private AuthToken mAuthToken = null;
    private OkHttpClient mOkHttpClient = null;
    private final GsonConverter mGsonConverter;
    private final RequestInterceptor mAuthInterceptor;

    private boolean mbAuthRequestOnGoing = false;
    private RetrofitError mRefreshError = null;
    private final ArrayDeque<ApiCallEvent> mApiEventQueue = new ArrayDeque<>();
    private final ArrayDeque<ApiCallEvent> mRefreshEventsQueue = new ArrayDeque<>();
    private final ArrayDeque<Object> mPostUploadQueue = new ArrayDeque<>();

    public NetworkService() {
        Crashlytics.log(Log.DEBUG, TAG, "Initializing NetworkService...");
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
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
        mAppContext = context.getApplicationContext();
        mRealm = Realm.getInstance(mAppContext);
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
                // if the response is 401 Unauthorized, we can recover from it by asking for the
                // password again
                if (!event.initiatedByUser && error.getResponse() != null &&
                        error.getResponse().getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    clearSavedPassword();
                    getBus().post(new PasswordChangedEvent());
                } else {
                    getBus().post(new LoginErrorEvent(error));
                }
                flushApiEventQueue(true);
            }
        });
    }

    @Subscribe
    public void onRefreshDataEvent(RefreshDataEvent event) {
        // do nothing if a refresh is already in progress
        // optimization disabled because sometimes (rarely) the queue doesn't get emptied correctly
        // e.g., logout followed by login
//        if (! mRefreshEventsQueue.isEmpty()) {
//            refreshSucceeded(null);
//            return;
//        }

        mRefreshEventsQueue.clear();
        mRefreshError = null;           // clear last error if any
        mRefreshEventsQueue.addAll(Arrays.asList(
                new LoadUserEvent(true),
                new LoadBlogSettingsEvent(true),
                new LoadConfigurationEvent(true),
                new SyncPostsEvent(true)
        ));

        for (ApiCallEvent refreshEvent : mRefreshEventsQueue) {
            if (event.loadCachedData) {
                refreshEvent.loadCachedData();
            }
            getBus().post(refreshEvent);
        }
    }

    @Subscribe
    public void onForceCancelRefreshEvent(ForceCancelRefreshEvent event) {
        // sometimes (rarely) the DataRefreshedEvent is not sent because an ApiCallEvent
        // doesn't get cleared from the queue, this is to guard against that
        if (! mRefreshEventsQueue.isEmpty()) {
            mRefreshEventsQueue.clear();
            mRefreshError = null;           // clear last error if any
        }
    }

    @Subscribe
    public void onLoadUserEvent(final LoadUserEvent event) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            RealmResults<User> users = mRealm.allObjects(User.class);
            if (users.size() > 0) {
                getBus().post(new UserLoadedEvent(users.first()));
                refreshSucceeded(event);
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
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                RealmResults<User> users = mRealm.allObjects(User.class);
                if (users.size() > 0) {
                    getBus().post(new UserLoadedEvent(users.first()));
                }
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new ApiErrorEvent(error));
                    refreshFailed(event, error);
                } else {
                    refreshSucceeded(event);
                }
            }
        });
    }

    @Subscribe
    public void onLoadBlogSettingsEvent(final LoadBlogSettingsEvent event) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            RealmResults<Setting> settings = mRealm.allObjects(Setting.class);
            if (settings.size() > 0) {
                getBus().post(new BlogSettingsLoadedEvent(settings));
                refreshSucceeded(event);
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
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                RealmResults<Setting> settings = mRealm.allObjects(Setting.class);
                if (settings.size() > 0) {
                    getBus().post(new BlogSettingsLoadedEvent(settings));
                }
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new ApiErrorEvent(error));
                    refreshFailed(event, error);
                } else {
                    refreshSucceeded(event);
                }
            }
        });
    }

    @Subscribe
    public void onLoadConfigurationEvent(final LoadConfigurationEvent event) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            RealmResults<ConfigurationParam> params = mRealm.allObjects(ConfigurationParam.class);
            if (params.size() > 0) {
                getBus().post(new ConfigurationLoadedEvent(params));
                refreshSucceeded(event);
                return;
            }
            // no configuration params found in db, force a network call!
        }

        if (! validateAccessToken(event)) return;
        mApi.getConfiguration(new Callback<ConfigurationList>() {
            @Override
            public void success(ConfigurationList configurationList, Response response) {
                createOrUpdateModel(configurationList.configuration);
                getBus().post(new ConfigurationLoadedEvent(configurationList.configuration));
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                RealmResults<ConfigurationParam> params = mRealm.allObjects(ConfigurationParam.class);
                if (params.size() > 0) {
                    getBus().post(new ConfigurationLoadedEvent(params));
                }
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new ApiErrorEvent(error));
                    refreshFailed(event, error);
                } else {
                    refreshSucceeded(event);
                }
            }
        });
    }

    @Subscribe
    public void onLoadPostsEvent(final LoadPostsEvent event) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            List<Post> posts = getPostsSorted();
            // if there are no posts, there could be 2 cases:
            // 1. there are actually no posts
            // 2. we just haven't fetched any posts from the server yet (Realm returns an empty list in this case too)
            if (posts.size() > 0) {
                getBus().post(new PostsLoadedEvent(posts));
                refreshSucceeded(event);
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
                // FIXME time complexity is quadratic in the number of posts!
                Observable.from(mRealm.allObjects(Post.class))
                        .filter(cached -> postList.indexOf(cached.getUuid()) == -1)
                        .toList()
                        .forEach(NetworkService.this::deleteModels);

                // skip posts with local-only edits (e.g., auto-saved edits to published posts)
                RealmResults<Post> localOnlyEdits = mRealm.where(Post.class)
                        .equalTo("pendingActions.type", PendingAction.EDIT_LOCAL)
                        .findAll();
                Observable.from(localOnlyEdits)
                        .map(post -> postList.indexOf(post.getUuid()))
                        .filter(idx -> idx > -1)
                        .forEach(postList::remove);

                // make sure drafts have a publishedAt of FAR_FUTURE so they're sorted to the top
                Observable.from(postList.posts)
                        .filter(post -> post.getPublishedAt() == null)
                        .forEach(post -> post.setPublishedAt(DateTimeUtils.FAR_FUTURE));

                // now create / update received posts
                createOrUpdateModel(postList.posts);
                getBus().post(new PostsLoadedEvent(getPostsSorted()));
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                getBus().post(new PostsLoadedEvent(getPostsSorted()));
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new ApiErrorEvent(error));
                    refreshFailed(event, error);
                } else {
                    refreshSucceeded(event);
                }
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
            String error = "No post with uuid = " + event.uuid + " found!";
            Crashlytics.logException(new IllegalArgumentException(error));
            Log.e(TAG, error);
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
        if (event.loadCachedData) {
            LoadPostsEvent loadPostsEvent = new LoadPostsEvent(false);
            mRefreshEventsQueue.add(loadPostsEvent);
            getBus().post(loadPostsEvent);
            refreshSucceeded(event);
            return;
        }

        final RealmResults<Post> localNewPosts = mRealm.where(Post.class)
                .equalTo("pendingActions.type", PendingAction.CREATE)
                .findAllSorted("uuid", false);
        final RealmResults<Post> localEditedPosts = mRealm.where(Post.class)
                .equalTo("pendingActions.type", PendingAction.EDIT)
                .findAll();

        // nothing to upload
        if (localNewPosts.isEmpty() && localEditedPosts.isEmpty() && event.forceNetworkCall) {
            LoadPostsEvent loadPostsEvent = new LoadPostsEvent(true);
            mRefreshEventsQueue.add(loadPostsEvent);
            getBus().post(loadPostsEvent);
            refreshSucceeded(event);
            return;
        }

        // keep track of new posts uploaded successfully, so the local copies can be deleted
        List<Post> localNewPostsUploaded = new ArrayList<>();

        final Action1<RetrofitError> syncFinishedCB = (retrofitError) -> {
            // delete local copies of only those new posts that were successfully uploaded
            deleteModels(localNewPostsUploaded);
            // if forceNetworkCall is true, first load from the db, AND only then from the network,
            // to avoid a crash because local posts have been deleted above but are still being
            // displayed, so we need to refresh the UI first
            getBus().post(new PostsLoadedEvent(getPostsSorted()));
            if (event.forceNetworkCall) {
                LoadPostsEvent loadPostsEvent = new LoadPostsEvent(true);
                mRefreshEventsQueue.add(loadPostsEvent);
                getBus().post(loadPostsEvent);
            }
            refreshDone(event, retrofitError);
        };

        mPostUploadQueue.addAll(localNewPosts);
        mPostUploadQueue.addAll(localEditedPosts);

        // ugly hack (suggested by the IDE) because this must be declared "final"
        final RetrofitError[] uploadErrorOccurred = {null};

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
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }

                @Override
                public void failure(RetrofitError error) {
                    uploadErrorOccurred[0] = error;
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
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
                    getBus().post(new PostSyncedEvent(localPost.getUuid()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }

                @Override
                public void failure(RetrofitError error) {
                    uploadErrorOccurred[0] = error;
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }
            });
        }
    }

    @Subscribe
    public void onSavePostEvent(SavePostEvent event) {
        Post post = event.post;
        //noinspection StatementWithEmptyBody
        if (PostUtils.hasPendingAction(post, PendingAction.CREATE)) {
            // no-op; if the post is yet to be created, we DO NOT change the PendingAction on it
        } else if (Post.DRAFT.equals(post.getStatus())) {
            PostUtils.addPendingAction(post, PendingAction.EDIT);
        } else if (Post.PUBLISHED.equals(post.getStatus()) && event.isAutoSave) {
            post.getPendingActions().clear();
            PostUtils.addPendingAction(post, PendingAction.EDIT_LOCAL);
        } else {
            // user hit "publish changes" explicitly, on a published post, so mark it for uploading
            post.getPendingActions().clear();
            PostUtils.addPendingAction(post, PendingAction.EDIT);
        }

        // save tags to Realm first
        for (Tag tag : post.getTags()) {
            if (tag.getUuid() == null) {
                tag.setUuid(getTempUniqueId(Tag.class));
                createOrUpdateModel(tag);
            }
        }

        post.setUpdatedAt(new Date());              // mark as updated, to promote in sorted order
        createOrUpdateModel(post);                  // save the local post to db

        getBus().post(new PostSavedEvent(post));
        getBus().post(new SyncPostsEvent(false));
    }

    @Subscribe
    public void onFileUploadEvent(FileUploadEvent event) {
        if (! validateAccessToken(event)) return;
        TypedFile typedFile = new TypedFile(event.mimeType, new File(event.path));
        mApi.uploadFile(typedFile, new Callback<String>() {
            @Override
            public void success(String url, Response response) {
                getBus().post(new FileUploadedEvent(url));
            }

            @Override
            public void failure(RetrofitError error) {
                getBus().post(new FileUploadErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        // revoke access and refresh tokens
        RevokeReqBody revokeReqs[] = new RevokeReqBody[] {
                new RevokeReqBody(RevokeReqBody.TOKEN_TYPE_ACCESS, mAuthToken.getAccessToken()),
                new RevokeReqBody(RevokeReqBody.TOKEN_TYPE_REFRESH, mAuthToken.getRefreshToken())
        };
        for (RevokeReqBody reqBody : revokeReqs) {
            mApi.revokeAuthToken(reqBody, new ResponseCallback() {
                @Override
                public void success(Response response) {
                    try {
                        InputStream istream = response.getBody().in();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
                        StringBuilder out = new StringBuilder();
                        String newLine = System.getProperty("line.separator");
                        String line;
                        while ((line = reader.readLine()) != null) {
                            out.append(line);
                            out.append(newLine);
                        }
                        JSONObject json = new JSONObject(out.toString());
                        if (json.has("error")) {
                            Crashlytics.log(Log.ERROR, TAG, "Failed to revoke "
                                    + reqBody.tokenTypeHint + ": " + json.getString("error"));
                        }
                    } catch (IOException e) {
                        // no-op
                    } catch (JSONException e) {
                        // no-op
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Crashlytics.log(Log.ERROR, TAG, "Failed to revoke " + reqBody.tokenTypeHint);
                }
            });
        }
        // clear all persisted blog data to avoid primary key conflicts
        mRealm.close();
        Realm.deleteRealmFile(mAppContext);
        mRealm = Realm.getInstance(mAppContext);
        AppState.getInstance(SpectreApplication.getInstance())
                .setBoolean(AppState.Key.LOGGED_IN, false);
        // reset state, to be sure
        mApiEventQueue.clear();
        mRefreshEventsQueue.clear();
        mbAuthRequestOnGoing = false;
        mRefreshError = null;
    }


    // private methods
    private boolean validateAccessToken(@NonNull ApiCallEvent event) {
        boolean valid = ! hasAccessTokenExpired();
        if (! valid) {
            refreshAccessToken(event);
        }
        return valid;
    }

    private void refreshAccessToken(@Nullable final ApiCallEvent eventToDefer) {
        if (eventToDefer != null) {
            mApiEventQueue.addLast(eventToDefer);
        }
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
                    flushApiEventQueue(true);
                }
            }
        });
    }

    private void flushApiEventQueue(boolean loadCachedData) {
        Bus bus = getBus();
        while (! mApiEventQueue.isEmpty()) {
            ApiCallEvent event = mApiEventQueue.remove();
            if (loadCachedData) event.loadCachedData();
            bus.post(event);
        }
    }

    private void refreshSucceeded(@Nullable ApiCallEvent sourceEvent) {
        refreshDone(sourceEvent, null);
    }

    private void refreshFailed(@Nullable ApiCallEvent sourceEvent, @NonNull RetrofitError error) {
        refreshDone(sourceEvent, error);
    }

    private void refreshDone(@Nullable ApiCallEvent sourceEvent, @Nullable RetrofitError error) {
        mRefreshEventsQueue.removeFirstOccurrence(sourceEvent);
        if (error != null) {
            mRefreshError = error;      // turn on error flag if *any* request fails
        }
        if (mRefreshEventsQueue.isEmpty()) {
            getBus().post(new DataRefreshedEvent(mRefreshError));
            mRefreshError = null;       // clear last error if any
        }
    }

    private void clearSavedPassword() {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        prefs.clear(UserPrefs.Key.PASSWORD);
        AppState appState = AppState.getInstance(SpectreApplication.getInstance());
        appState.clear(AppState.Key.LOGGED_IN);
    }

    private void postLoginStartEvent() {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        String username = prefs.getString(UserPrefs.Key.USERNAME);
        String password = prefs.getString(UserPrefs.Key.PASSWORD);
        getBus().post(new LoginStartEvent(blogUrl, username, password, false));
    }

    private void onNewAuthToken(AuthToken authToken) {
        Log.d(TAG, "Got new access token = " + authToken.getAccessToken());
        mbAuthRequestOnGoing = false;
        authToken.setCreatedAt(DateTimeUtils.getEpochSeconds());
        mAuthToken = createOrUpdateModel(authToken);
        AppState.getInstance(SpectreApplication.getInstance())
                .setBoolean(AppState.Key.LOGGED_IN, true);
        flushApiEventQueue(false);
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
        RestAdapter.LogLevel logLevel = BuildConfig.DEBUG
                ? RestAdapter.LogLevel.HEADERS
                : RestAdapter.LogLevel.NONE;
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppUtils.pathJoin(blogUrl, "ghost/api/v0.1"))
                .setClient(new OkClient(mOkHttpClient))
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mAuthInterceptor)
                .setLogLevel(logLevel)
                .build();
        return restAdapter.create(GhostApiService.class);
    }

    private List<Post> getPostsSorted() {
        // FIXME time complexity O(n) for copying + O(n log n) for sorting!
        RealmResults<Post> posts = mRealm.allObjects(Post.class);
        List<Post> postsCopy = new ArrayList<>(posts);
        Collections.sort(postsCopy, PostUtils.COMPARATOR_MAIN_LIST);
        return postsCopy;
    }

    /**
     * Generates a temporary primary key until the actual id is generated by the server. <b>Be
     * careful when calling this in a loop, if you don't save the object before calling it again,
     * you'll get the same id twice!</b>
     */
    @NonNull
    private <T extends RealmObject> String getTempUniqueId(Class<T> clazz) {
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
        // TODO add error handling
        // TODO use Realm#executeTransaction(Realm.Transaction) instead of this
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
