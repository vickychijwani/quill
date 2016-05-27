package me.vickychijwani.spectre.network;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.util.Map;

import me.vickychijwani.spectre.model.ConfigurationList;
import me.vickychijwani.spectre.model.ConfigurationParam;

public class ConfigurationListDeserializer implements JsonDeserializer<ConfigurationList> {

    @Override
    public ConfigurationList deserialize(JsonElement element, Type type,
                                         JsonDeserializationContext context)
            throws JsonParseException {
        try {
            JsonArray configJsons = element.getAsJsonObject().getAsJsonArray("configuration");
            if (configJsons.size() > 0 && !configJsons.get(0).getAsJsonObject().has("key")) {
                // new configuration format - dictionary style
                // { configuration: [{ fileStorage: {value: true, type: "bool"}, ... }] }
                return parseDictionaryStyleConfig(configJsons.get(0).getAsJsonObject());
            } else {
                // old configuration format - array of entries
                // { configuration: [ {key: fileStorage, value: true}, ... ] }
                return parseArrayOfEntriesConfig(configJsons);
            }
        } catch (Exception e) {
            // FIXME temp log to help debug Crashlytics issue #87
            Crashlytics.log(Log.DEBUG, "ParseException", "Exception thrown while trying to parse JSON: " + element.toString());
            throw e;
        }
    }

    private ConfigurationList parseDictionaryStyleConfig(JsonObject configJson) {
        ConfigurationList config = new ConfigurationList();
        for (Map.Entry<String, JsonElement> entryJson : configJson.entrySet()) {
            String key = entryJson.getKey();
            JsonPrimitive valueJson = entryJson.getValue().getAsJsonObject()
                    .get("value").getAsJsonPrimitive();
            config.configuration.add(makeConfigParam(key, valueJson));
        }
        return config;
    }

    private ConfigurationList parseArrayOfEntriesConfig(JsonArray configJsons) {
        ConfigurationList config = new ConfigurationList();
        for (JsonElement itemJsonEl : configJsons) {
            JsonObject itemJson = itemJsonEl.getAsJsonObject();
            String key = itemJson.get("key").getAsString();
            JsonPrimitive valueJson = itemJson.getAsJsonPrimitive("value");
            config.configuration.add(makeConfigParam(key, valueJson));
        }
        return config;
    }

    private ConfigurationParam makeConfigParam(String key, JsonPrimitive value)
            throws JsonParseException {
        ConfigurationParam param = new ConfigurationParam();
        String valueStr;
        if (value == null) {
            // FIXME temp log to help debug Crashlytics issue #87
            throw new NullPointerException("value for key '" + key + "' is null!");
        } else if (value.isString()) {
            valueStr = value.getAsString();
        } else if (value.isBoolean()) {
            valueStr = String.valueOf(value.getAsBoolean());
        } else if (value.isNumber()) {
            valueStr = String.valueOf(value.getAsDouble());
        } else {
            throw new JsonParseException("unknown value type in Ghost configuration list");
        }
        param.setKey(key);
        param.setValue(valueStr);
        return param;
    }

}
