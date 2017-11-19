package me.vickychijwani.spectre.util;

import android.support.annotation.DrawableRes;
import android.view.View;

import java.util.List;

public final class Tip {

    public final @DrawableRes int image;
    public final String content;
    public final List<Action> actions;

    public Tip(@DrawableRes int image, String content, List<Action> actions) {
        this.image = image;
        this.content = content;
        this.actions = actions;
    }


    public static final class Action {
        public final @DrawableRes int icon;
        public final String text;
        public final View.OnClickListener clickListener;

        public Action(@DrawableRes int icon, String text, View.OnClickListener clickListener) {
            this.icon = icon;
            this.text = text;
            this.clickListener = clickListener;
        }
    }

}
