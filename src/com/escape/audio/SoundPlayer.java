package com.escape.audio;

import com.escape.Config;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final byte[] MAP_SHUFFLE_PCM = buildPaperShufflePcm(
            Config.SOUND.mapShuffleDurationMs,
            Config.SOUND.mapShuffleVolume,
            Config.SOUND.mapShuffleHiPassHz);
    private static final byte[] COIN_CLINK_PCM = buildPatternPcm(
            Config.SOUND.coinClinkFreqs, Config.SOUND.coinClinkDurationMs,
            Config.SOUND.coinClinkGapMs, Config.SOUND.coinClinkVolume);
    private static final byte[] LADDER_UP_PCM = buildPatternPcm(
            Config.SOUND.ladderUpFreqs, Config.SOUND.ladderUpDurationMs,
            Config.SOUND.ladderUpGapMs, Config.SOUND.ladderUpVolume);
    private static final byte[] LADDER_DOWN_PCM = buildPatternPcm(
            Config.SOUND.ladderDownFreqs, Config.SOUND.ladderDownDurationMs,
            Config.SOUND.ladderDownGapMs, Config.SOUND.ladderDownVolume);

    private static final ExecutorService FOOTSTEP_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "footstep-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService SFX_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sfx-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean FOOTSTEP_ACTIVE = new AtomicBoolean(false);

    private static final Object FOOTSTEP_LINE_LOCK = new Object();
    private static final Object SFX_LINE_LOCK = new Object();
    private static SourceDataLine footstepLine;
    private static SourceDataLine sfxLine;

    public SoundPlayer() {
    }
    
    public void playFootsteps() {
        if (!FOOTSTEP_ACTIVE.compareAndSet(false, true)) {
            return;
        }
        FOOTSTEP_EXECUTOR.submit(() -> {
            try {
                playPcmOnFootstepChannel(FOOTSTEP_PCM);
            } finally {
                FOOTSTEP_ACTIVE.set(false);
            }
        });
    }

    public void playKeyPickup() {
        playSfxAsync(KEY_PICKUP_PCM);
    }

    public void playCoinClink() {
        playSfxAsync(COIN_CLINK_PCM);
    }

    public void playMapPickup() {
        playSfxAsync(MAP_SHUFFLE_PCM);
    }

    public void playLadderUp() {
        playSfxAsync(LADDER_UP_PCM);
    }

    public void playLadderDown() {
        playSfxAsync(LADDER_DOWN_PCM);
    }

    public void playTone(double frequencyHz, int durationMs, double volume) {
        playPattern(new double[] { frequencyHz }, durationMs, 0, volume);
    }

    public void playPattern(double[] frequenciesHz, int durationMs, int gapMs, double volume) {
        playSfxAsync(buildPatternPcm(frequenciesHz, durationMs, gapMs, volume));
    }

    private static void playSfxAsync(byte[] pcm) {
        SFX_EXECUTOR.submit(() -> playPcmOnSfxChannel(pcm));
    }

    private static void playPcmOnFootstepChannel(byte[] pcm) {
        synchronized (FOOTSTEP_LINE_LOCK) {
            try {
                SourceDataLine line = ensureFootstepLineOpen();
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
            } catch (Exception ex) {
                System.err.println("Footstep playback failed: " + ex.getMessage());
            }
        }
    }

    private static void playPcmOnSfxChannel(byte[] pcm) {
        synchronized (SFX_LINE_LOCK) {
            try {
                SourceDataLine line = ensureSfxLineOpen();
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
            } catch (Exception ex) {
                System.err.println("SFX playback failed: " + ex.getMessage());
            }
        }
    }

    private static SourceDataLine ensureFootstepLineOpen() throws Exception {
        if (footstepLine == null || !footstepLine.isOpen()) {
            footstepLine = (SourceDataLine) AudioSystem.getLine(INFO);
            // Small fixed buffer helps keep trigger latency low.
            footstepLine.open(FORMAT, Config.SOUND.lineBufferSize);
        }
        return footstepLine;
    }

    private static SourceDataLine ensureSfxLineOpen() throws Exception {
        if (sfxLine == null || !sfxLine.isOpen()) {
            sfxLine = (SourceDataLine) AudioSystem.getLine(INFO);
            // Keep SFX on a separate channel so pickups do not block footsteps.
            sfxLine.open(FORMAT, Config.SOUND.lineBufferSize);
        }
        return sfxLine;
    }

    private static void playPcm(byte[] pcm) {
        try {
            SourceDataLine line = ensureSfxLineOpen();
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception ex) {
            System.err.println("Sound playback failed: " + ex.getMessage());
        }
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

    /**
     * Noise-based paper shuffle with a gentle high-pass response and short bursts.
     * This avoids metallic tones and sounds closer to rustling paper.
     */
    private static byte[] buildPaperShufflePcm(int durationMs, double volume, double hiPassHz) {
        int samples = Math.max(1, (int) ((durationMs / 1000.0) * SAMPLE_RATE));
        byte[] pcm = new byte[samples * 2];

        Random rng = new Random(0x50A9E7L);
        double rc = 1.0 / (2.0 * Math.PI * Math.max(20.0, hiPassHz));
        double dt = 1.0 / SAMPLE_RATE;
        double alpha = rc / (rc + dt);

        double prevX = 0.0;
        double prevY = 0.0;

        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;

            // Two envelope humps emulate quick hand movement while unfolding paper.
            double envA = Math.exp(-Math.pow((t - 0.30) / 0.18, 2.0));
            double envB = 0.85 * Math.exp(-Math.pow((t - 0.68) / 0.16, 2.0));
            double movement = Math.min(1.0, envA + envB);

            double x = (rng.nextDouble() * 2.0 - 1.0) * movement;
            double y = alpha * (prevY + x - prevX);
            prevX = x;
            prevY = y;

            double edgeFade = Math.sin(Math.PI * t);
            double sample = y * edgeFade * volume;

            short value = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 32767);
            int byteIndex = i * 2;
            pcm[byteIndex] = (byte) (value & 0xFF);
            pcm[byteIndex + 1] = (byte) ((value >>> 8) & 0xFF);
        }
        return pcm;
    }
}
