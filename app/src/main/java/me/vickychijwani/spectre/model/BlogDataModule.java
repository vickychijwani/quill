package me.vickychijwani.spectre.model;

import io.realm.annotations.RealmModule;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.model.entity.ConfigurationParam;
import me.vickychijwani.spectre.model.entity.ETag;
import me.vickychijwani.spectre.model.entity.PendingAction;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Role;
import me.vickychijwani.spectre.model.entity.Setting;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.model.entity.User;

// set of classes included in the schema for blog data Realms

@RealmModule(classes = {
        AuthToken.class,
        ConfigurationParam.class,
        ETag.class,
        PendingAction.class,
        Post.class,
        Role.class,
        Setting.class,
        Tag.class,
        User.class
})
public class BlogDataModule {}
