package com.escape.audio;

import com.escape.Config;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class SoundPlayer {
    private static final float SAMPLE_RATE = Config.SOUND.sampleRate;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final DataLine.Info INFO = new DataLine.Info(SourceDataLine.class, FORMAT);

    // Reuse fixed sound patterns so we do not regenerate PCM each playback.
    private static final byte[] FOOTSTEP_PCM = buildPatternPcm(
            Config.SOUND.footstepFreqs, Config.SOUND.footstepDurationMs,
            Config.SOUND.footstepGapMs, Config.SOUND.footstepVolume);
    private static final byte[] KEY_PICKUP_PCM = buildPatternPcm(
            Config.SOUND.keyPickupFreqs, Config.SOUND.keyPickupDurationMs,
            Config.SOUND.keyPickupGapMs, Config.SOUND.keyPickupVolume);
    private static final byte[] COIN_CLINK_PCM = buildPatternPcm(
            Config.SOUND.coinClinkFreqs, Config.SOUND.coinClinkDurationMs,
            Config.SOUND.coinClinkGapMs, Config.SOUND.coinClinkVolume);
    private static final byte[] LADDER_UP_PCM = buildPatternPcm(
            Config.SOUND.ladderUpFreqs, Config.SOUND.ladderUpDurationMs,
            Config.SOUND.ladderUpGapMs, Config.SOUND.ladderUpVolume);
    private static final byte[] LADDER_DOWN_PCM = buildPatternPcm(
            Config.SOUND.ladderDownFreqs, Config.SOUND.ladderDownDurationMs,
            Config.SOUND.ladderDownGapMs, Config.SOUND.ladderDownVolume);

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
            sharedLine.open(FORMAT, Config.SOUND.lineBufferSize);
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
            double attack = Math.min(1.0, i / (samples * Config.SOUND.envelopeAttackRatio));
            double decay = Math.pow(1.0 - (double) i / samples, Config.SOUND.envelopeDecayPower);
            double envelope = attack * decay;
            double sample = Math.sin(2.0 * Math.PI * frequencyHz * t) * envelope * volume;
            short value = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 32767);
            int byteIndex = (sampleOffset + i) * 2;
            buffer[byteIndex] = (byte) (value & 0xFF);
            buffer[byteIndex + 1] = (byte) ((value >>> 8) & 0xFF);
        }
    }
}
