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

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
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
import me.vickychijwani.spectre.event.GhostVersionLoadedEvent;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadConfigurationEvent;
import me.vickychijwani.spectre.event.LoadGhostVersionEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadTagsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.LogoutStatusEvent;
import me.vickychijwani.spectre.event.PasswordChangedEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.event.SyncPostsEvent;
import me.vickychijwani.spectre.event.TagsLoadedEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.model.entity.ConfigurationParam;
import me.vickychijwani.spectre.model.entity.ETag;
import me.vickychijwani.spectre.model.entity.PendingAction;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Setting;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.model.entity.User;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.network.entity.PostList;
import me.vickychijwani.spectre.network.entity.PostStubList;
import me.vickychijwani.spectre.network.entity.RefreshReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.SettingsList;
import me.vickychijwani.spectre.network.entity.UserList;
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
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

public class NetworkService {

    private static final String TAG = "NetworkService";

    // number of posts to fetch
    private static final int POSTS_FETCH_LIMIT = 30;

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
                .registerTypeAdapter(ConfigurationList.class, new ConfigurationListDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy(), new AnnotationExclusionStrategy())
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
        mRealm = Realm.getDefaultInstance();
        if (AppState.getInstance(context).getBoolean(AppState.Key.LOGGED_IN)) {
            mAuthToken = mRealm.where(AuthToken.class).findFirst();
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
        mbAuthRequestOnGoing = true;
        mApi = buildApiService(event.blogUrl);

        // get dynamic client secret, if the blog supports it
        mApi.getLoginPage(new ResponseCallback() {
            @Override
            public void success(Response response) {
                String html = new String(((TypedByteArray) response.getBody()).getBytes());
                Pattern clientSecretPattern = Pattern.compile("^.*<meta[ ]+name=['\"]env-clientSecret['\"][ ]+content=['\"]([^'\"]+)['\"].*$", Pattern.DOTALL);
                Matcher matcher = clientSecretPattern.matcher(html);
                if (matcher.matches()) {
                    String clientSecret = matcher.group(1);
                    doLogin(event, clientSecret);
                } else {
                    Log.w(TAG, "No client secret found, assuming old Ghost version without client secret support");
                    doLogin(event, null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "No client secret found, assuming old Ghost version without client secret support");
                Log.e(TAG, Log.getStackTraceString(error));
                doLogin(event, null);
            }
        });
    }

    private void doLogin(@NonNull LoginStartEvent event, @Nullable String clientSecret) {
        AuthReqBody credentials = new AuthReqBody(event.username, event.password, clientSecret);
        mApi.getAuthToken(credentials, new Callback<AuthToken>() {
            @Override
            public void success(AuthToken authToken, Response response) {
                onNewAuthToken(authToken);
                getBus().post(new LoginDoneEvent(event.blogUrl, event.username, event.password,
                        event.initiatedByUser));
            }

            @Override
            public void failure(RetrofitError error) {
                mbAuthRequestOnGoing = false;
                // if the response is 401 Unauthorized, we can recover from it by asking for the
                // password again
                if (!event.initiatedByUser && error.getResponse() != null &&
                        error.getResponse().getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // FIXME actually this can also happen if the access token is manually revoked OR
                    // if the expiration time is changed inside Ghost (#92)
                    clearSavedPassword();
                    getBus().post(new PasswordChangedEvent());
                } else {
                    getBus().post(new LoginErrorEvent(error, event.blogUrl, event.initiatedByUser));
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
    public void onLoadGhostVersionEvent(LoadGhostVersionEvent event) {
        if (mAuthToken == null) {
            return; // can't do much, not logged in
        }

        final String UNKNOWN_VERSION = "Unknown";

        if (! event.forceNetworkCall) {
            RealmQuery<ConfigurationParam> query = mRealm.where(ConfigurationParam.class)
                    .equalTo("key", "version", Case.INSENSITIVE);
            if (query.count() > 0) {
                getBus().post(new GhostVersionLoadedEvent(query.findFirst().getValue()));
                return;
            }
        }

        if (! validateAccessToken(event)) return;

        Action0 checkVersionInConfiguration = () -> {
            Action1<List<ConfigurationParam>> successCallback = configParams -> {
                boolean versionFound = false;
                for (ConfigurationParam param : configParams) {
                    if ("version".equals(param.getKey())) {
                        versionFound = true;
                        getBus().post(new GhostVersionLoadedEvent(param.getValue()));
                    }
                }
                if (! versionFound) {
                    getBus().post(new GhostVersionLoadedEvent(UNKNOWN_VERSION));
                }
            };
            Action1<RetrofitError> failureCallback = error -> {
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new GhostVersionLoadedEvent(UNKNOWN_VERSION));
                } else {
                    // fallback to cached data
                    successCallback.call(mRealm.where(ConfigurationParam.class).findAll());
                }
            };
            doLoadConfiguration(new LoadConfigurationEvent(true), successCallback, failureCallback);
        };

        mApi.getVersion(new JSONObjectCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject, Response response) {
                try {
                    String ghostVersion = jsonObject
                            .getJSONArray("configuration")
                            .getJSONObject(0)
                            .getString("version");
                    getBus().post(new GhostVersionLoadedEvent(ghostVersion));
                } catch (JSONException e) {
                    getBus().post(new GhostVersionLoadedEvent(UNKNOWN_VERSION));
                }
            }

            @Override
            public void onFailure(RetrofitError error) {
                if (error.getResponse() != null
                        && error.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                    // this condition means the version is < 0.7.9, because that is when
                    // the new /configuration/about endpoint was introduced
                    // FIXME remove this mess once we stop supporting < 0.7.9
                    checkVersionInConfiguration.call();
                } else {
                    getBus().post(new GhostVersionLoadedEvent(UNKNOWN_VERSION));
                }
            }
        });
    }

    @Subscribe
    public void onLoadUserEvent(final LoadUserEvent event) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            RealmResults<User> users = mRealm.where(User.class).findAll();
            if (users.size() > 0) {
                getBus().post(new UserLoadedEvent(users.first()));
                refreshSucceeded(event);
                return;
            }
            // else no users found in db, force a network call!
        }

        if (! validateAccessToken(event)) return;
        mApi.getCurrentUser(loadEtag(ETag.TYPE_CURRENT_USER), new Callback<UserList>() {
            @Override
            public void success(UserList userList, Response response) {
                storeEtag(response.getHeaders(), ETag.TYPE_CURRENT_USER);
                createOrUpdateModel(userList.users);
                getBus().post(new UserLoadedEvent(userList.users.get(0)));
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                RealmResults<User> users = mRealm.where(User.class).findAll();
                if (users.size() > 0) {
                    getBus().post(new UserLoadedEvent(users.first()));
                }
                // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                // login with the password if that doesn't work either (#92)
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
            RealmResults<Setting> settings = mRealm.where(Setting.class).findAll();
            if (settings.size() > 0) {
                getBus().post(new BlogSettingsLoadedEvent(settings));
                refreshSucceeded(event);
                return;
            }
            // no settings found in db, force a network call!
        }

        if (! validateAccessToken(event)) return;
        mApi.getSettings(loadEtag(ETag.TYPE_BLOG_SETTINGS), new Callback<SettingsList>() {
            @Override
            public void success(SettingsList settingsList, Response response) {
                storeEtag(response.getHeaders(), ETag.TYPE_BLOG_SETTINGS);
                createOrUpdateModel(settingsList.settings);
                getBus().post(new BlogSettingsLoadedEvent(settingsList.settings));
                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                RealmResults<Setting> settings = mRealm.where(Setting.class).findAll();
                if (settings.size() > 0) {
                    getBus().post(new BlogSettingsLoadedEvent(settings));
                }
                // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                // login with the password if that doesn't work either (#92)
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
        Action1<List<ConfigurationParam>> successCallback = configParams -> {
            getBus().post(new ConfigurationLoadedEvent(configParams));
            refreshSucceeded(event);
        };
        Action1<RetrofitError> failureCallback = error -> {
            // fallback to cached data
            RealmResults<ConfigurationParam> params = mRealm.where(ConfigurationParam.class).findAll();
            if (params.size() > 0) {
                getBus().post(new ConfigurationLoadedEvent(params));
            }
            if (NetworkUtils.isRealError(error)) {
                refreshFailed(event, error);
            } else {
                refreshSucceeded(event);
            }
        };
        doLoadConfiguration(event, successCallback, failureCallback);
    }

    public void doLoadConfiguration(final LoadConfigurationEvent event,
                                    @NonNull Action1<List<ConfigurationParam>> successCallback,
                                    @NonNull Action1<RetrofitError> failureCallback) {
        if (event.loadCachedData || ! event.forceNetworkCall) {
            RealmResults<ConfigurationParam> params = mRealm.where(ConfigurationParam.class).findAll();
            if (params.size() > 0) {
                successCallback.call(params);
                return;
            }
            // no configuration params found in db, force a network call!
        }

        if (! validateAccessToken(event)) return;
        mApi.getConfiguration(loadEtag(ETag.TYPE_CONFIGURATION), new Callback<ConfigurationList>() {
            @Override
            public void success(ConfigurationList configurationList, Response response) {
                storeEtag(response.getHeaders(), ETag.TYPE_CONFIGURATION);
                createOrUpdateModel(configurationList.configuration);
                successCallback.call(configurationList.configuration);
            }

            @Override
            public void failure(RetrofitError error) {
                // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                // login with the password if that doesn't work either (#92)
                if (NetworkUtils.isRealError(error)) {
                    getBus().post(new ApiErrorEvent(error));
                }
                failureCallback.call(error);
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
                getBus().post(new PostsLoadedEvent(posts, POSTS_FETCH_LIMIT));
                refreshSucceeded(event);
                return;
            }
        }

        if (! validateAccessToken(event)) return;
        mApi.getPosts(loadEtag(ETag.TYPE_ALL_POSTS), POSTS_FETCH_LIMIT, new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                storeEtag(response.getHeaders(), ETag.TYPE_ALL_POSTS);

                // delete posts that are no longer present on the server
                // this assumes that postList.posts is a list of ALL posts on the server
                // FIXME time complexity is quadratic in the number of posts!
                Observable.from(mRealm.where(Post.class).findAll())
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
                getBus().post(new PostsLoadedEvent(getPostsSorted(), POSTS_FETCH_LIMIT));

                refreshSucceeded(event);
            }

            @Override
            public void failure(RetrofitError error) {
                // fallback to cached data
                getBus().post(new PostsLoadedEvent(getPostsSorted(), POSTS_FETCH_LIMIT));
                if (NetworkUtils.isRealError(error)) {
                    // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                    // login with the password if that doesn't work either (#92)
                    getBus().post(new ApiErrorEvent(error));
                    refreshFailed(event, error);
                } else {
                    refreshSucceeded(event);
                }
            }
        });
    }

    @Subscribe
    public void onCreatePostEvent(final CreatePostEvent event) {
        Post newPost = new Post();
        newPost.addPendingAction(PendingAction.CREATE);
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
                .findAllSorted("uuid", Sort.DESCENDING);
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
            getBus().post(new PostsLoadedEvent(getPostsSorted(), POSTS_FETCH_LIMIT));
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
                        localPost.clearPendingActions();
                    });
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    localNewPostsUploaded.add(localPost);
                    // FIXME this is a new post! how do subscribers know which post changed?
                    getBus().post(new PostReplacedEvent(postList.posts.get(0)));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }

                @Override
                public void failure(RetrofitError error) {
                    // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                    // login with the password if that doesn't work either (#92)

                    uploadErrorOccurred[0] = error;
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted(), POSTS_FETCH_LIMIT));
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
                        localPost.clearPendingActions();
                    });
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new PostSyncedEvent(localPost.getUuid()));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }

                @Override
                public void failure(RetrofitError error) {
                    // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                    // login with the password if that doesn't work either (#92)

                    uploadErrorOccurred[0] = error;
                    mPostUploadQueue.removeFirstOccurrence(localPost);
                    getBus().post(new ApiErrorEvent(error));
                    getBus().post(new PostsLoadedEvent(getPostsSorted(), POSTS_FETCH_LIMIT));
                    if (mPostUploadQueue.isEmpty()) syncFinishedCB.call(uploadErrorOccurred[0]);
                }
            });
        }
    }

    @Subscribe
    public void onSavePostEvent(SavePostEvent event) {
        Post post = event.post;

        // TODO bug in edge-case:
        // TODO 1. user publishes draft, offline => pending actions = { EDIT }
        // TODO 2. then makes some edits offline which are auto-saved => pending actions = { EDIT_LOCAL }
        // TODO the post will not actually be published now!
        // TODO to resolve this we would require some notion of pending actions associated with
        // TODO specific fields of a post rather than the entire post

        //noinspection StatementWithEmptyBody
        if (post.hasPendingAction(PendingAction.CREATE)) {
            // no-op; if the post is yet to be created, we DO NOT change the PendingAction on it
        } else if (Post.DRAFT.equals(post.getStatus())) {
            post.addPendingAction(PendingAction.EDIT);
        } else if (Post.PUBLISHED.equals(post.getStatus()) && event.isAutoSave) {
            post.clearPendingActions();
            post.addPendingAction(PendingAction.EDIT_LOCAL);
        } else {
            // user hit "publish changes" explicitly, on a published post, so mark it for uploading
            post.clearPendingActions();
            post.addPendingAction(PendingAction.EDIT);
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
                // FIXME if status is 401 Unauthorized, try to refresh the access token, OR
                // login with the password if that doesn't work either (#92)

                getBus().post(new FileUploadErrorEvent(error));
            }
        });
    }

    @Subscribe
    public void onLoadTagsEvent(LoadTagsEvent event) {
        RealmResults<Tag> tags = mRealm.where(Tag.class).findAllSorted("name");
        List<Tag> tagsCopy = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            tagsCopy.add(new Tag(tag.getName()));
        }
        getBus().post(new TagsLoadedEvent(tagsCopy));
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        if (!event.forceLogout) {
            // check for pending actions
            RealmResults<PendingAction> pendingActions = mRealm.where(PendingAction.class).findAll();
            if (pendingActions.size() > 0) {
                getBus().post(new LogoutStatusEvent(false, true));
                return;
            }
        }

        // revoke access and refresh tokens
        RevokeReqBody revokeReqs[] = new RevokeReqBody[] {
                new RevokeReqBody(RevokeReqBody.TOKEN_TYPE_ACCESS, mAuthToken.getAccessToken()),
                new RevokeReqBody(RevokeReqBody.TOKEN_TYPE_REFRESH, mAuthToken.getRefreshToken())
        };
        for (RevokeReqBody reqBody : revokeReqs) {
            mApi.revokeAuthToken(reqBody, new JSONObjectCallback() {
                @Override
                public void onSuccess(JSONObject json, Response response) {
                    if (json.has("error")) {
                        Crashlytics.log(Log.ERROR, TAG, "Failed to revoke "
                                + reqBody.tokenTypeHint + ": " + json.optString("error"));
                    }
                }

                @Override
                public void onFailure(RetrofitError error) {
                    Crashlytics.log(Log.ERROR, TAG, "Failed to revoke " + reqBody.tokenTypeHint);
                    Crashlytics.logException(error);
                }
            });
        }
        // clear all persisted blog data to avoid primary key conflicts
        mRealm.close();
        Realm.deleteRealm(mRealm.getConfiguration());
        mRealm = Realm.getDefaultInstance();
        AppState.getInstance(SpectreApplication.getInstance())
                .setBoolean(AppState.Key.LOGGED_IN, false);
        // reset state, to be sure
        mApiEventQueue.clear();
        mRefreshEventsQueue.clear();
        mbAuthRequestOnGoing = false;
        mRefreshError = null;
        getBus().post(new LogoutStatusEvent(true, false));
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
                // this should only happen if the refresh token is manually revoked or the
                // expiration time is changed inside Ghost (#92)
                if (error.getResponse() != null &&
                        error.getResponse().getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    postLoginStartEvent();
                    Log.e(TAG, "Expired refresh token used! You're wasting bandwidth / battery!");
                } else {
                    getBus().post(new LoginErrorEvent(error, null, false));
                    flushApiEventQueue(true);
                }
            }
        });
    }

    private void flushApiEventQueue(boolean loadCachedData) {
        Bus bus = getBus();
        boolean isQueueEmpty = false;
        while (! mApiEventQueue.isEmpty()) {
            ApiCallEvent event = mApiEventQueue.remove();
            isQueueEmpty = mApiEventQueue.isEmpty();
            if (loadCachedData) event.loadCachedData();
            bus.post(event);
            if (isQueueEmpty) {     // don't retry, gets into infinite loop
                mApiEventQueue.clear();
            }
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
        RealmResults<Post> posts = mRealm.where(Post.class).findAll();
        List<Post> postsCopy = new ArrayList<>(posts);
        Collections.sort(postsCopy, PostUtils.COMPARATOR_MAIN_LIST);
        return postsCopy;
    }

    private void storeEtag(List<Header> headers, @ETag.Type String etagType) {
        for (Header header : headers) {
            if ("ETag".equals(header.getName())) {
                ETag etag = new ETag(etagType, header.getValue());
                createOrUpdateModel(etag);
            }
        }
    }

    private String loadEtag(@ETag.Type String etagType) {
        ETag etag = mRealm.where(ETag.class).equalTo("type", etagType).findFirst();
        return (etag != null) ? etag.getTag() : "";
    }

    /**
     * Generates a temporary primary key until the actual id is generated by the server. <b>Be
     * careful when calling this in a loop, if you don't save the object before calling it again,
     * you'll get the same id twice!</b>
     */
    @NonNull
    private <T extends RealmModel> String getTempUniqueId(Class<T> clazz) {
        int tempId = Integer.MAX_VALUE;
        while (mRealm.where(clazz).equalTo("uuid", String.valueOf(tempId)).findAll().size() > 0) {
            --tempId;
        }
        return String.valueOf(tempId);
    }

    private <T extends RealmModel> T createOrUpdateModel(T object) {
        return createOrUpdateModel(object, null);
    }

    private <T extends RealmModel> T createOrUpdateModel(T object, @Nullable Runnable transaction) {
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

    private <T extends RealmModel> List<T> createOrUpdateModel(Iterable<T> objects) {
        return createOrUpdateModel(objects, null);
    }

    private <T extends RealmModel> List<T> createOrUpdateModel(Iterable<T> objects,
                                                               @Nullable Runnable transaction) {
        mRealm.beginTransaction();
        List<T> realmObjects = mRealm.copyToRealmOrUpdate(objects);
        if (transaction != null) {
            transaction.run();
        }
        mRealm.commitTransaction();
        return realmObjects;
    }

    private <T extends RealmModel> void deleteModels(List<T> realmObjects) {
        mRealm.beginTransaction();
        Observable.from(realmObjects).forEach(RealmObject::deleteFromRealm);
        mRealm.commitTransaction();
    }

    private Bus getBus() {
        return BusProvider.getBus();
    }

}
