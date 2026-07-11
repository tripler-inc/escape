package com.escape.audio;

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
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
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
        Sequence seq   = new Sequence(Sequence.PPQ, 8);
        Track    track = seq.createTrack();

        // Program change: Acoustic Grand Piano on channel 0
        addMsg(track, ShortMessage.PROGRAM_CHANGE, 0, 0, 0, 0);

        // Tempo: ~140 bpm  (428 571 microseconds per beat)
        MetaMessage tempo = new MetaMessage();
        byte[] tempoBytes = { 0x06, (byte)0x8B, (byte)0x37 }; // 428 791 µs ≈ 140 bpm
        tempo.setMessage(0x51, tempoBytes, 3);
        track.add(new MidiEvent(tempo, 0));

        // Notes: { MIDI pitch, startTick, lengthTicks, velocity }
        int[][] melody = {
            // Bar 1 — ascending arpeggio to C6
            { 60, 0,   4, 90 },  // C4
            { 64, 4,   4, 90 },  // E4
            { 67, 8,   4, 90 },  // G4
            { 72, 12,  8,110 },  // C5  (held)
            // Bar 2 — E5 G5 back to C5
            { 76, 20,  4, 95 },  // E5
            { 79, 24,  4, 95 },  // G5
            { 84, 28, 12,115 },  // C6  (big hit, held)
            // Bar 3 — descending fill
            { 79, 40,  4, 85 },  // G5
            { 76, 44,  4, 85 },  // E5
            { 72, 48,  4, 85 },  // C5
            { 69, 52,  4, 80 },  // A4
            { 67, 56,  4, 80 },  // G4
            // Bar 4 — triumphant finish
            { 72, 60,  4, 100 }, // C5
            { 76, 64,  4, 100 }, // E5
            { 79, 68,  4, 100 }, // G5
            { 84, 72,  4, 110 }, // C6
            { 88, 76, 16, 127 }, // E6  (finale, held)
        };

        for (int[] n : melody) {
            int pitch = n[0], start = n[1], len = n[2], vel = n[3];
            addMsg(track, ShortMessage.NOTE_ON,  0, pitch, vel,   start);
            addMsg(track, ShortMessage.NOTE_OFF, 0, pitch,   0,   start + len);
        }

        // End-of-track meta event
        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, 96));

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
