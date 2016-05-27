package me.vickychijwani.spectre.event;

import java.util.List;

import me.vickychijwani.spectre.model.entity.ConfigurationParam;

public class ConfigurationLoadedEvent {

    public final List<ConfigurationParam> params;

    public ConfigurationLoadedEvent(List<ConfigurationParam> params) {
        this.params = params;
    }

}
