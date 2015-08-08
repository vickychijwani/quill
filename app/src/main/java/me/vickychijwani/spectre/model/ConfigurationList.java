package me.vickychijwani.spectre.model;

import java.util.Arrays;
import java.util.List;

public class ConfigurationList {

    public List<ConfigurationParam> configuration;

    public static ConfigurationList from(ConfigurationParam... configuration) {
        ConfigurationList configurationList = new ConfigurationList();
        configurationList.configuration = Arrays.asList(configuration);
        return configurationList;
    }

}
