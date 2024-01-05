package net.nussi.midi.sender.model;

import java.util.HashMap;
import java.util.Map;

public class DeviceConfig {
    public String name;
    public String type;
    public Map<String, String> options;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "DeviceConfig{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", options=" + options +
                '}';
    }
}
