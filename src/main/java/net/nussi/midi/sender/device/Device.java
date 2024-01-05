package net.nussi.midi.sender.device;

import net.nussi.midi.sender.model.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Device {
    public final Logger LOGGER = LoggerFactory.getLogger(Device.class);
    private DeviceConfig config;

    public Device(DeviceConfig config) {
        this.config = config;
    }

    public DeviceConfig getConfig() {
        return config;
    }

    public void setConfig(DeviceConfig config) {
        this.config = config;
    }

    public abstract void start();
    public abstract void stop();

    public abstract void callEndpoint(String endpoint, int command);
    public abstract void checkEndpoint(String endpoint) throws IllegalArgumentException;

    @Override
    public String toString() {
        return "Device{" +
                "config=" + config +
                '}';
    }
}
