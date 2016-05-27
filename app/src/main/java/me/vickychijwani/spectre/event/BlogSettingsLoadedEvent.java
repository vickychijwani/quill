package me.vickychijwani.spectre.event;

import java.util.List;

import me.vickychijwani.spectre.model.entity.Setting;

public class BlogSettingsLoadedEvent {

    public final List<Setting> settings;

    public BlogSettingsLoadedEvent(List<Setting> settings) {
        this.settings = settings;
    }

}
