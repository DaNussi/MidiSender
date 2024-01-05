package net.nussi.midi.sender;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.nussi.midi.sender.device.Device;
import net.nussi.midi.sender.device.DeviceArduino;
import net.nussi.midi.sender.model.DeviceConfig;
import net.nussi.midi.sender.model.MappingConfig;
import net.nussi.midi.sender.model.PlayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

@ShellComponent
public class MidiSenderCmd {
    public static final Logger LOGGER = LoggerFactory.getLogger(MidiSenderCmd.class);
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static final HashMap<DeviceConfig, Device> devices = new HashMap<>();
    public static final HashMap<String, Device> devicesByName = new HashMap<>();
    public static final  List<NoteListener> noteListeners = new ArrayList<>();

    @Autowired
    MidiSenderConfig config;

    @Bean
    CommandRegistration commandRegistrationRiotScan() {
        return CommandRegistration.builder()
                .command("play")
                .withOption()
                    .longNames("midi")
                    .shortNames('m')
                    .description("Detailpfad zur midi datei.")
                    .defaultValue(config.getDev().isEnabled() ? config.getDev().getMidiFile() : null)
                    .and()
                .withOption()
                    .longNames("config")
                    .shortNames('c')
                    .description("Detailpfad zur configurations datei.")
                    .defaultValue(config.getDev().isEnabled() ? config.getDev().getConfFile() : null)
                    .and()
                .withTarget()
                    .consumer((commandContext) -> {
                        playCmd(commandContext);
                    })
                    .and()
                .build();
    }

    public void playCmd(CommandContext commandContext) {
        // Check Conf File
        LOGGER.info("Checking config file");
        String confPathString = commandContext.getOptionValue("config");
        Path confPath = Path.of(confPathString);
        if(!Files.exists(confPath)) {
            LOGGER.error("Config file not found in " + confPath);
            return;
        }
        File confFile = new File(confPath.toUri());

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PlayConfig playConfig;
        try {
            playConfig = mapper.readValue(confFile, PlayConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Config file error! " + e.getMessage());
            return;
        }
        LOGGER.info("Config file gefunden");
        if(!config.getPlay().getConfig().getVersion().equals(playConfig.version)) {
            LOGGER.error("Config file has incompatible version! Required: " + config.getPlay().getConfig().getVersion() + "    Presented: " + playConfig.version);
            return;
        }

        // Check Midi File
        LOGGER.info("Checking midi file");
        String midiPathString = commandContext.getOptionValue("midi");
        Path midiPath = Path.of(midiPathString);
        if(!Files.exists(midiPath)) {
            LOGGER.error("Midi file not found in " + midiPath);
            return;
        }
        File midiFile = new File(midiPath.toUri());
        LOGGER.info("Found midi file");

        // Index devices
        for(DeviceConfig deviceConfig : playConfig.getDevices()) {
            Device device = null;
            switch (deviceConfig.getType()) {
                case "Arduino":
                    device = new DeviceArduino(deviceConfig);
                    break;
                default:
                    LOGGER.error("Unknown device type " + deviceConfig.getType());
                    return;
            }
            devices.put(deviceConfig, device);
            devicesByName.put(deviceConfig.name, device);
            LOGGER.info("Found device config for " + deviceConfig.getType() + " with name " + deviceConfig.getName());
            LOGGER.info("Crated device " + deviceConfig.getName() + " " + device);
        }
        LOGGER.info("Created " + devices.size() + " devices!");
        if(devices.size() < 1) {
            LOGGER.error("No devices where created check your config!");
            return;
        }

        // Index Mapping
        for(MappingConfig mappingConfig : playConfig.getMappings()) {
            Device device = devicesByName.get(mappingConfig.getDevice());
            if(device == null) {
                LOGGER.error("No device found with name " + mappingConfig.getDevice());
                return;
            }

            String noteId;
            try {
                noteId = generateNoteId(mappingConfig.getChannel(), mappingConfig.getNote(), mappingConfig.getOctave());

                device.checkEndpoint(mappingConfig.getEndpoint());
            } catch (IllegalArgumentException e) {
                LOGGER.error(e.getMessage());
                return;
            }

            noteListeners.add(new NoteListener(device, mappingConfig.getEndpoint(), noteId));
        }


        // Play Midi
        devices.forEach((deviceConfig, device) -> device.start());
        playMidi(midiFile, midiPath);
        devices.forEach((deviceConfig, device) -> device.stop());

    }

    public void playMidi(File midiFile, Path midiPath) {
        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(midiFile);
        } catch (Exception e) {
            LOGGER.error("Failed to parse midi file " + midiPath);
            return;
        }

        long milisPerTick = (sequence.getMicrosecondLength() / 1000)/sequence.getTickLength();
        long trackStartTime = System.currentTimeMillis();

        int trackNumber = 0;
        for (Track track :  sequence.getTracks()) {
            trackNumber++;
            LOGGER.info("Track " + trackNumber + ": size = " + track.size());

            long oldTick = 0;
            List<MidiEvent> messages = new ArrayList<>();
            for (int i=0; i < track.size(); i++) {
                MidiEvent event = track.get(i);

                if(oldTick != event.getTick()) {
                    oldTick = event.getTick();

                    executeTick(messages, trackNumber, event.getTick());
                    messages.clear();

                    try {
                        long estimatedTime = event.getTick() * milisPerTick;
                        long actualTime = System.currentTimeMillis() - trackStartTime;
                        long compensation = estimatedTime - actualTime;
                        if(compensation <= 0) {
                            LOGGER.warn("Tick execution to slow!" +Math.abs(compensation) +" ms behind schedule");
                        } else {
                            Thread.sleep(compensation);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        LOGGER.error("Schedule compensation failed! " + e.getMessage());
                        return;
                    }
                }
                messages.add(event);
            }
        }

    }


    // can't be slower thant 28ms execution time
    public void executeTick(List<MidiEvent> midiEvents, int track, long tick) {
        LOGGER.info("Tick " + tick);


        for(MidiEvent midiEvent : midiEvents) {
            MidiMessage message = midiEvent.getMessage();
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                if (sm.getCommand() == NOTE_ON) {
                    int key = sm.getData1();
                    int octave = (key / 12) - 1;
                    int note = key % 12;
                    String noteName = NOTE_NAMES[note];
                    String noteId = generateNoteId(sm.getChannel(), noteName, octave);
                    noteListeners.forEach(e -> e.emit(noteId, sm.getCommand()));
                    LOGGER.debug("Channel: " + sm.getChannel() + " " + "Note on, " + noteName + octave + " key=" + key);
                } else if (sm.getCommand() == NOTE_OFF) {
                    int key = sm.getData1();
                    int octave = (key / 12) - 1;
                    int note = key % 12;
                    String noteName = NOTE_NAMES[note];
                    String noteId = generateNoteId(sm.getChannel(), noteName, octave);
                    noteListeners.forEach(e -> e.emit(noteId, sm.getCommand()));
                    LOGGER.debug("Channel: " + sm.getChannel() + " " + "Note off, " + noteName + octave + " key=" + key);
                } else {
                    LOGGER.info("Command:" + sm.getCommand());
                }
            } else {
                LOGGER.info("Other message: " + message.getClass());
            }
        }

    }

    public static String generateNoteId(int channel, String note, int octave) throws IllegalArgumentException {
        if(channel < 0 || channel >= 16) throw new IllegalArgumentException("Channel can only be between  0 - 15");
        if(octave < -1 || octave  >= 10) throw new IllegalArgumentException("Channel can only be between -1 - 9");
        if(!List.of(NOTE_NAMES).contains(note)) throw new IllegalArgumentException("Unknown note " + note);

        return channel + "-" + note + "-" + octave;
    }


    public static class NoteListener {
        Device device;
        String endpoint;
        String noteId;

        public NoteListener(Device device, String endpoint, String noteId) {
            this.device = device;
            this.endpoint = endpoint;
            this.noteId = noteId;
        }

        public void emit(String noteId, int command) {
            if(this.noteId.equals(noteId)) device.callEndpoint(endpoint,command);
        };
    }

}
