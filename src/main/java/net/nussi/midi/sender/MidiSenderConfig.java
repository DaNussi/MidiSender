package net.nussi.midi.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "midi.sender")
@ConfigurationPropertiesScan
public class MidiSenderConfig {

    private Dev dev;
    private Play play;

    public Dev getDev() {
        return dev;
    }

    public void setDev(Dev dev) {
        this.dev = dev;
    }

    public Play getPlay() {
        return play;
    }

    public void setPlay(Play play) {
        this.play = play;
    }

    public static class Play {
        private Config config;

        public Config getConfig() {
            return config;
        }

        public void setConfig(Config config) {
            this.config = config;
        }

        public static class Config {
            private String version;

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }
        }

    }

    public static class Dev {
        private boolean enabled;

        private String midiFile;
        private String confFile;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMidiFile() {
            return midiFile;
        }

        public void setMidiFile(String midiFile) {
            this.midiFile = midiFile;
        }

        public String getConfFile() {
            return confFile;
        }

        public void setConfFile(String confFile) {
            this.confFile = confFile;
        }
    }
}
