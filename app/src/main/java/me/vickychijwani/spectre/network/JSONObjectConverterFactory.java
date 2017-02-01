package me.vickychijwani.spectre.network;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/* package */ class JSONObjectConverterFactory extends Converter.Factory {

    private JSONObjectConverter mConverter;

    public static JSONObjectConverterFactory create() {
        return new JSONObjectConverterFactory();
    }

    private JSONObjectConverterFactory() {
        mConverter = new JSONObjectConverter();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if (type != JSONObject.class) {
            return null;
        }
        return mConverter;
    }

    private class JSONObjectConverter implements Converter<ResponseBody, JSONObject> {
        private static final String TAG = "JSONObjectConverter";

        @Nullable
        @Override
        public JSONObject convert(ResponseBody value) throws IOException {
            try {
                return new JSONObject(value.string());
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                return null;
            }
        }
    }

}
