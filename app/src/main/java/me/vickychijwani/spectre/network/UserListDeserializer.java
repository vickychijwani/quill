package me.vickychijwani.spectre.network;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

import me.vickychijwani.spectre.model.entity.User;
import me.vickychijwani.spectre.network.entity.UserList;

// FIXME this class only exists for debugging issue #124, it just logs the JSON input
/* package */ class UserListDeserializer implements JsonDeserializer<UserList> {

    private static final String TAG = UserListDeserializer.class.getSimpleName();

    @Override
    public UserList deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray userListJson = element.getAsJsonObject().get("users").getAsJsonArray();
        UserList userList = new UserList();
        userList.users = new ArrayList<>(1);
        for (JsonElement userJson : userListJson) {
            Crashlytics.log(Log.DEBUG, TAG, "USER JSON = " + userJson.toString());
            userList.users.add(context.deserialize(userJson, User.class));
        }
        return userList;
    }

}
