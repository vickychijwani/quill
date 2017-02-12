package me.vickychijwani.spectre.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.NetworkUtils;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.functions.Action1;

final class GhostApiUtils {

    private static final String TAG = GhostApiUtils.class.getSimpleName();

    static Retrofit getRetrofit(@NonNull String baseUrl, @NonNull OkHttpClient httpClient) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(ConfigurationList.class, new ConfigurationListDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy(), new AnnotationExclusionStrategy())
                .create();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
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
                String html = response.body();
                String clientSecret = GhostApiUtils.extractClientSecretFromHtml(html);
                if (clientSecret == null) {
                    Log.w(TAG, "No client secret found, assuming old Ghost version without client secret support");
                }
                callback.call(clientSecret);
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
