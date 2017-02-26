package me.vickychijwani.spectre.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.functions.Action1;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public final class GhostApiUtils {

    private static final String TAG = GhostApiUtils.class.getSimpleName();

    public static Retrofit getRetrofit(@NonNull String blogUrl, @NonNull OkHttpClient httpClient) {
        String baseUrl = NetworkUtils.makeAbsoluteUrl(blogUrl, "ghost/api/v0.1/");
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(ConfigurationList.class, new ConfigurationListDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy(), new AnnotationExclusionStrategy())
                .create();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                // for HTML output (e.g., to get the client secret)
                .addConverterFactory(StringConverterFactory.create())
                // for raw JSONObject output (e.g., for the /configuration/about call)
                .addConverterFactory(JSONObjectConverterFactory.create())
                // for domain objects
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    static void doWithClientSecret(@NonNull GhostApiService apiService, @NonNull String blogUrl,
                                   @NonNull Action1<String> callback) {
        // get dynamic client secret, if the blog supports it
        apiService.getLoginPage(NetworkUtils.makeAbsoluteUrl(blogUrl, "ghost/")).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String html = response.body();
                    String clientSecret = GhostApiUtils.extractClientSecretFromHtml(html);
                    if (clientSecret == null) {
                        Crashlytics.log(Log.WARN, TAG, "No client secret found, assuming old Ghost version without client secret support");
                    }
                    callback.call(clientSecret);
                } else {
                    try {
                        Crashlytics.log(Log.ERROR, TAG, "HTML IS NULL - this is definitely a bug");
                        Crashlytics.log(Log.ERROR, TAG, "Response: " + response.errorBody().string());
                    } catch (IOException e) {
                        Crashlytics.log(Log.ERROR, TAG, Log.getStackTraceString(e));
                    }
                    // as they say... fail loudly!
                    throw new RuntimeException("BUG: Code assumes the blog url = " + blogUrl
                            + " is valid, but it's not because the response was NOT successful here!");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable error) {
                // error in transport layer, or lower
                Log.e(TAG, "No client secret found, assuming old Ghost version without client secret support");
                Log.e(TAG, Log.getStackTraceString(error));
                callback.call(null);
            }
        });
    }

    // IMPORTANT: client secret may be null in older Ghost versions (< 0.7.x)
    @Nullable
    static String extractClientSecretFromHtml(@NonNull String html) {
        // quotes around attribute values are optional in HTML5: http://stackoverflow.com/q/6495310/504611
        Pattern clientSecretPattern = Pattern.compile("^.*<meta[ ]+name=['\"]?env-clientSecret['\"]?[ ]+content=['\"]?([^'\"]+)['\"]?.*$", Pattern.DOTALL);
        Matcher matcher = clientSecretPattern.matcher(html);
        String clientSecret = null;
        if (matcher.matches()) {
            clientSecret = matcher.group(1);
        }
        return clientSecret;
    }

}
