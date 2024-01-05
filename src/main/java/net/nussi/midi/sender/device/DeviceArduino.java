package net.nussi.midi.sender.device;

import net.nussi.midi.sender.MidiSenderCmd;
import net.nussi.midi.sender.model.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.sound.midi.ShortMessage.NOTE_ON;

public class DeviceArduino extends Device {

    public DeviceArduino(DeviceConfig config) {
        super(config);
    }

    @Override
    public void start() {
        LOGGER.info("Starting arduino device " + getConfig().getName());

    }

    @Override
    public void stop() {
        LOGGER.info("Stopping arduino device " + getConfig().getName());

    }

    @Override
    public void callEndpoint(String endpoint, int command) {
        LOGGER.info("Arduino with name " + this.getConfig().getName() + " endpoint " + endpoint + " emitted! " + (command == NOTE_ON ? "NOTE_ON" : "NOTE_OFF"));
    }

    @Override
    public void checkEndpoint(String endpoint) throws IllegalArgumentException {
        if (!endpoint.contains("R"))
            throw new IllegalArgumentException("Invalid endpoint name " + endpoint + "! Arduino endpoint must begin with R! Examples: R1, R2, R16");
    }

    @Override
    public String toString() {
        return "DeviceArduino{" +
                "config=" + getConfig() +
                '}';
    }
}
