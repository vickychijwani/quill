package me.vickychijwani.spectre.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import me.vickychijwani.spectre.model.Tag;

// TODO can be deleted now?
public class TagSerializer implements JsonSerializer<Tag> {

    @Override
    public JsonElement serialize(Tag tag, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("name", tag.getName());
        return json;
    }

}
