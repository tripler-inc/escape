package com.escape.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class SoundPlayer {
    private static final float SAMPLE_RATE = 44100f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final DataLine.Info INFO = new DataLine.Info(SourceDataLine.class, FORMAT);

    // Reuse fixed sound patterns so we do not regenerate PCM each playback.
//    private static final byte[] FOOTSTEP_PCM = buildPatternPcm(new double[] { 182.0, 164.0 }, 32, 3, 0.1125);
    private static final byte[] FOOTSTEP_PCM = buildPatternPcm(new double[] { 182.0, 164.0 }, 20, 3, 0.21875);
    private static final byte[] KEY_PICKUP_PCM = buildPatternPcm(new double[] { 880.0, 1174.7, 1568.0 }, 28, 4, 0.28125);
//    private static final byte[] COIN_CLINK_PCM = buildPatternPcm(new double[] { 1318.5, 1760.0 }, 18, 2, 0.21875);
    private static final byte[] COIN_CLINK_PCM = buildPatternPcm(new double[] { 1318.5, 1760.0 }, 18, 2, 0.28125);
    private static final byte[] LADDER_UP_PCM = buildPatternPcm(new double[] { 392.0, 493.9, 587.3 }, 46, 8, 0.2);
    private static final byte[] LADDER_DOWN_PCM = buildPatternPcm(new double[] { 587.3, 493.9, 392.0 }, 46, 8, 0.2);

    private static SourceDataLine sharedLine;

    public SoundPlayer() {
    }
    
    public void playFootsteps() {
        playPcm(FOOTSTEP_PCM);
    }

    public void playKeyPickup() {
        playPcm(KEY_PICKUP_PCM);
    }

    public void playCoinClink() {
        playPcm(COIN_CLINK_PCM);
    }

    public void playLadderUp() {
        playPcm(LADDER_UP_PCM);
    }

    public void playLadderDown() {
        playPcm(LADDER_DOWN_PCM);
    }

    public void playTone(double frequencyHz, int durationMs, double volume) {
        playPattern(new double[] { frequencyHz }, durationMs, 0, volume);
    }

    public synchronized void playPattern(double[] frequenciesHz, int durationMs, int gapMs, double volume) {
        playPcm(buildPatternPcm(frequenciesHz, durationMs, gapMs, volume));
    }

    private static synchronized void playPcm(byte[] pcm) {
        try {
            SourceDataLine line = ensureLineOpen();
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception ex) {
            System.err.println("Sound playback failed: " + ex.getMessage());
        }
    }

    private static SourceDataLine ensureLineOpen() throws Exception {
        if (sharedLine == null || !sharedLine.isOpen()) {
            sharedLine = (SourceDataLine) AudioSystem.getLine(INFO);
            // Small fixed buffer helps keep trigger latency low.
            sharedLine.open(FORMAT, 2048);
        }
        return sharedLine;
    }

    private static byte[] buildPatternPcm(double[] frequenciesHz, int durationMs, int gapMs, double volume) {
        int toneSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        int gapSamples = gapMs > 0 ? (int) ((gapMs / 1000.0) * SAMPLE_RATE) : 0;
        int totalSamples = (toneSamples * frequenciesHz.length) + (gapSamples * Math.max(0, frequenciesHz.length - 1));
        byte[] pcm = new byte[totalSamples * 2];

        int sampleOffset = 0;
        for (int idx = 0; idx < frequenciesHz.length; idx++) {
            writeToneInto(pcm, sampleOffset, frequenciesHz[idx], toneSamples, volume);
            sampleOffset += toneSamples;
            if (gapSamples > 0 && idx < frequenciesHz.length - 1) {
                sampleOffset += gapSamples;
            }
        }
        return pcm;
    }

    private static void writeToneInto(byte[] buffer, int sampleOffset, double frequencyHz, int samples, double volume) {
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double attack = Math.min(1.0, i / (samples * 0.12));
            double decay = Math.pow(1.0 - (double) i / samples, 1.35);
            double envelope = attack * decay;
            double sample = Math.sin(2.0 * Math.PI * frequencyHz * t) * envelope * volume;
            short value = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 32767);
            int byteIndex = (sampleOffset + i) * 2;
            buffer[byteIndex] = (byte) (value & 0xFF);
            buffer[byteIndex + 1] = (byte) ((value >>> 8) & 0xFF);
        }
    }
}
