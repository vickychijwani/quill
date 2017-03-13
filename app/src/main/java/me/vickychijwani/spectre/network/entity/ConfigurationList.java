package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.vickychijwani.spectre.model.entity.ConfigurationParam;

@SuppressWarnings("unused")
public class ConfigurationList {

    public List<ConfigurationParam> configuration;

    public ConfigurationList() {
        configuration = new ArrayList<>();
    }

    public static ConfigurationList from(ConfigurationParam... configuration) {
        ConfigurationList configurationList = new ConfigurationList();
        configurationList.configuration = Arrays.asList(configuration);
        return configurationList;
    }

    public boolean has(@NonNull String key) {
        for (ConfigurationParam param : configuration) {
            if (key.equals(param.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public String get(@NonNull String key) {
        for (ConfigurationParam param : configuration) {
            if (key.equals(param.getKey())) {
                return param.getValue();
            }
        }
        return null;
    }

    public String getClientSecret() {
        return get("clientSecret");
    }

}
