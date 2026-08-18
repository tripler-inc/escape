package com.escape.audio;

import com.escape.Config;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Plays a programmatically generated MIDI victory fanfare.
 * No external audio files are required.
 */
public class AudioPlayer {

    private Sequencer sequencer;

    public AudioPlayer() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(buildVictorySequence());
            sequencer.setLoopCount(Config.AUDIO.loopContinuous
                    ? Sequencer.LOOP_CONTINUOUSLY : 0);
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            sequencer = null; // audio unavailable; game continues silently
        }
    }

    public void play() {
        if (sequencer != null && !sequencer.isRunning()) {
            sequencer.setTickPosition(0);
            sequencer.start();
        }
    }

    public void stop() {
        if (sequencer != null && sequencer.isRunning()) {
            sequencer.stop();
        }
    }

    // ── Sequence builder ─────────────────────────────────────────────

    /**
     * A bright, celebratory 4-bar fanfare in C major.
     * Tick resolution: 8 ticks per beat (PPQ = 8).
     * Each entry: { pitch, startTick, durationTicks, velocity }
     */
    private static Sequence buildVictorySequence() throws InvalidMidiDataException {
        Sequence seq   = new Sequence(Sequence.PPQ, Config.AUDIO.victoryPpq);
        Track    track = seq.createTrack();

        addMsg(track, ShortMessage.PROGRAM_CHANGE, 0, Config.AUDIO.victoryProgram, 0, 0);

        MetaMessage tempo = new MetaMessage();
        int us = Config.AUDIO.victoryTempoUsPerBeat;
        byte[] tempoBytes = {
            (byte) ((us >> 16) & 0xFF),
            (byte) ((us >>  8) & 0xFF),
            (byte) (us & 0xFF)
        };
        tempo.setMessage(0x51, tempoBytes, 3);
        track.add(new MidiEvent(tempo, 0));

        // Notes: { MIDI pitch, startTick, lengthTicks, velocity }
        for (int[] n : Config.AUDIO.victoryMelody) {
            int pitch = n[0], start = n[1], len = n[2], vel = n[3];
            addMsg(track, ShortMessage.NOTE_ON,  0, pitch, vel,   start);
            addMsg(track, ShortMessage.NOTE_OFF, 0, pitch,   0,   start + len);
        }

        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, Config.AUDIO.eotTick));

        return seq;
    }

    private static void addMsg(Track track, int cmd, int chan,
                               int d1, int d2, long tick)
            throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(cmd, chan, d1, d2);
        track.add(new MidiEvent(msg, tick));
    }
}
