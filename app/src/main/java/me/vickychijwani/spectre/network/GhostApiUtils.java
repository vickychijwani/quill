package me.vickychijwani.spectre.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Date;

import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.entity.ApiErrorList;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.NetworkUtils;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

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

    @Nullable
    public static ApiErrorList parseApiErrors(Retrofit retrofit, HttpException exception) {
        ApiErrorList apiErrors = null;
        try {
            //noinspection unchecked
            Response<AuthToken> response = (Response<AuthToken>) exception.response();
            ResponseBody errorBody = response.errorBody();
            apiErrors = (ApiErrorList) retrofit.responseBodyConverter(
                    ApiErrorList.class, new Annotation[0]).convert(errorBody);
        } catch (IOException | ClassCastException e) {
            Timber.e("Error while parsing login errors! Response code = "
                    + exception.response().code());
            Timber.e(e);
        }
        return apiErrors;
    }

}
