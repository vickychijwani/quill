package me.vickychijwani.spectre.model;

import io.realm.annotations.RealmModule;
import me.vickychijwani.spectre.model.entity.BlogMetadata;

// set of classes included in the schema for blog metadata Realm

@RealmModule(classes = {
        BlogMetadata.class
})
public class BlogMetadataModule {}
