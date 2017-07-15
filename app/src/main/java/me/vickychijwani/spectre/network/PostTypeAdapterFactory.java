package me.vickychijwani.spectre.network;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import me.vickychijwani.spectre.model.entity.Post;


class PostTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        @SuppressWarnings("unchecked")
        Class<T> rawType = (Class<T>) type.getRawType();
        if (rawType != Post.class) {
            return null;
        }

        final TypeAdapter delegate = gson.getDelegateAdapter(this, type);
        //noinspection unchecked
        return (TypeAdapter<T>) new TypeAdapter<Post>() {
            @Override
            public void write(JsonWriter out, Post value) throws IOException {
                //noinspection unchecked
                delegate.write(out, value);
            }

            @Override
            public Post read(JsonReader in) throws IOException {
                Post post = (Post) delegate.read(in);

                // Post JSON example:
                // {
                //   "mobiledoc": "{\"version\": \"0.3.1\", ... }",
                //   ...
                // }
                post.setMarkdown(GhostApiUtils.mobiledocToMarkdown(post.getMobiledoc()));

                return post;
            }
        };
    }
}
