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
import retrofit.ResponseCallback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import rx.functions.Action1;

final class GhostApiUtils {

    private static final String TAG = GhostApiUtils.class.getSimpleName();

    // TODO this doesn't really belong here
    static Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(ConfigurationList.class, new ConfigurationListDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(new RealmExclusionStrategy(), new AnnotationExclusionStrategy())
                .create();
    }

    static void doWithClientSecret(@NonNull GhostApiService apiService,
                                          @NonNull Action1<String> callback) {
        // get dynamic client secret, if the blog supports it
        apiService.getLoginPage(new ResponseCallback() {
            @Override
            public void success(Response response) {
                String html = new String(((TypedByteArray) response.getBody()).getBytes());
                String clientSecret = GhostApiUtils.extractClientSecretFromHtml(html);
                if (clientSecret == null) {
                    Log.w(TAG, "No client secret found, assuming old Ghost version without client secret support");
                }
                callback.call(clientSecret);
            }

            @Override
            public void failure(RetrofitError error) {
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
