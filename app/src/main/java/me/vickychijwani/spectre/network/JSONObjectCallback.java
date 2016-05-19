package me.vickychijwani.spectre.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import retrofit.ResponseCallback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*package*/ abstract class JSONObjectCallback extends ResponseCallback {

    public abstract void onSuccess(JSONObject json, Response response);

    public abstract void onFailure(RetrofitError error);

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
            onSuccess(json, response);
        } catch (IOException e) {
            onFailure(RetrofitError.unexpectedError(response.getUrl(), e));
        } catch (JSONException e) {
            onFailure(RetrofitError.unexpectedError(response.getUrl(), e));
        }
    }

    @Override
    public void failure(RetrofitError error) {
        onFailure(error);
    }

}
