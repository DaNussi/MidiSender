package net.nussi.midi.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@CommandScan
public class MidiSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidiSenderApplication.class, args);
    }

}
