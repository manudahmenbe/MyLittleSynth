package one.empty3.apps.mylittlesynth.rythms;

import com.sun.media.sound.*;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.midi.*;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class TimelineThread extends Thread {


    private RythmPanel panel;


    class PlayMid extends Thread {
        private final File midi;

        public PlayMid(File wave) {
            this.midi = wave;
        }

        @Override
        public void run() {
            super.run();
            try {
                Sequencer sequencer = MidiSystem.getSequencer(); // Get the default Sequencer
                if (sequencer == null) {
                    System.err.println("Sequencer device not supported");
                    return;
                }
                sequencer.open(); // Open device
//                Soundbank soundbank = getSoundbank(new File(".\\midibanks\\KawaiUprightPiano-20190703.sf2"));
//                if(soundbank!=null) {
//                    MidiSystem.getSynthesizer().loadAllInstruments(soundbank);
//                }
                // Create sequence, the File must contain MIDI file data.
                Sequence sequence = MidiSystem.getSequence(midi);
                sequencer.setSequence(sequence); // load it into sequencer
                sequencer.start();  // start the playback
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException ex) {
                ex.printStackTrace();
            }

        }
    }


    public static Soundbank getSoundbank(File file)
            throws InvalidMidiDataException, IOException {
        if (file.exists()) {
            try {
                AudioFileFormat.Type[] audioFileTypes = AudioSystem.getAudioFileTypes();
                for (AudioFileFormat.Type t : audioFileTypes) {
                    System.out.println("Extension : " + t.getExtension());
                }

                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                ais.close();
                ModelByteBufferWavetable osc = new ModelByteBufferWavetable(
                        new ModelByteBuffer(file, 0, file.length()), -4800);
                ModelPerformer performer = new ModelPerformer();
                performer.getOscillators().add(osc);
                SimpleSoundbank sbk = new SimpleSoundbank();
                SimpleInstrument ins = new SimpleInstrument();
                ins.add(performer);
                sbk.addInstrument(ins);
                return sbk;
            } catch (IOException e) {
                return null;
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
                return null;
            }
        } else return null;

    }

    private HashMap<URI, Media> samples = new HashMap<>();
    public static final int ENDS = 0;
    public Timeline[] timeline;
    private double TINCR = 0.001;
    private boolean pause = false;

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }

    private int loop;

    public void run() {
        pause = false;
        double t = 0;
        int loop = timeline.length;
        Timeline.Model[] prev = new Timeline.Model[loop];
        Timeline.Model[] next = new Timeline.Model[loop];
        while (isRunning()) {
            for (int track = 0; track < timeline.length; track++) {
                while (pause) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                next[track] = timeline[track].next();

                if (next[track] == null || !isPlayNow(getT(track), next[track])) {
                    continue;
                }


                if (next[track].wave.getName().endsWith("mid")) {
                    new PlayMid(next[track].wave).start();
                } else {
                    Media media = null;
                    media = new Media(next[track].wave.toURI().toString());
                    new MediaPlayer(media).play();
                }

                prev[track] = next[track];

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isPlayNow(double t, Timeline.Model next) {
        return Math.abs(next.timeOnTimeline - t) < 0.5;
    }

    public TimelineThread(RythmPanel panel) {
        this.panel = panel;
        timeline = panel.timeline;

    }

    private double getT(int track) {
        double t;
        t = panel.loopTimer[track].getCurrentTimeOnLineSec();
        return t;
    }

    private boolean isRunning() {
        return true;
    }

    public void pause() {
        pause = true;
    }
}
