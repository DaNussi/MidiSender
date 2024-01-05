package net.nussi.midi.sender;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.nussi.midi.sender.model.PlayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

@ShellComponent
public class MidiSenderCmd {
    public static final Logger LOGGER = LoggerFactory.getLogger(MidiSenderCmd.class);
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

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

                        // Check Conf File
                        LOGGER.info("Checking conf file!");
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
                        LOGGER.info("Config file gefunden!");
                        if(!config.getPlay().getConfig().getVersion().equals(playConfig.version)) {
                            LOGGER.error("Config file has incompatible version! Required: " + config.getPlay().getConfig().getVersion() + "    Presented: " + playConfig.version);
                            return;
                        }

                        // Check Midi File
                        LOGGER.info("Checking midi file!");
                        String midiPathString = commandContext.getOptionValue("midi");
                        Path midiPath = Path.of(midiPathString);
                        if(!Files.exists(midiPath)) {
                            LOGGER.error("Midi file not found in " + midiPath);
                            return;
                        }
                        File midiFile = new File(midiPath.toUri());
                        LOGGER.info("Midi file gefunden!");


                        // Play Midi
                        Sequence sequence = null;
                        try {
                            sequence = MidiSystem.getSequence(midiFile);
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse midi file " + midiPath);
                            return;
                        }

                        int trackNumber = 0;
                        for (Track track :  sequence.getTracks()) {
                            trackNumber++;
                            System.out.println("Track " + trackNumber + ": size = " + track.size());
                            System.out.println();
                            for (int i=0; i < track.size(); i++) {
                                MidiEvent event = track.get(i);
                                System.out.print("@" + event.getTick() + " ");
                                MidiMessage message = event.getMessage();
                                if (message instanceof ShortMessage) {
                                    ShortMessage sm = (ShortMessage) message;
                                    System.out.print("Channel: " + sm.getChannel() + " ");
                                    if (sm.getCommand() == NOTE_ON) {
                                        int key = sm.getData1();
                                        int octave = (key / 12)-1;
                                        int note = key % 12;
                                        String noteName = NOTE_NAMES[note];
                                        int velocity = sm.getData2();
                                        System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                                    } else if (sm.getCommand() == NOTE_OFF) {
                                        int key = sm.getData1();
                                        int octave = (key / 12)-1;
                                        int note = key % 12;
                                        String noteName = NOTE_NAMES[note];
                                        int velocity = sm.getData2();
                                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                                    } else {
                                        System.out.println("Command:" + sm.getCommand());
                                    }
                                } else {
                                    System.out.println("Other message: " + message.getClass());
                                }
                            }
                        System.out.println();
                    }



                    })
                    .and()
                .build();
    }



}
