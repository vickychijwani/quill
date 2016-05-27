package me.vickychijwani.spectre.network.entity;

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

}
