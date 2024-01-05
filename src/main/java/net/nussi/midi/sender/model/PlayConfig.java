package net.nussi.midi.sender.model;

import java.util.List;

public class PlayConfig {
    public String version;

    public List<DeviceConfig> devices;

    public List<MappingConfig> mappings;

    public List<MappingConfig> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingConfig> mappings) {
        this.mappings = mappings;
    }

    public List<DeviceConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceConfig> devices) {
        this.devices = devices;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
