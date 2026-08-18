package com.escape.ui;

import com.escape.Config;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Random;

/**
 * A simple particle-based confetti shower drawn over the win screen.
 * Call start() once when the player wins, then update() + draw() every tick.
 */
public class ConfettiAnimation {

    private static final int NUM_PARTICLES = Config.CONFETTI.numParticles;

    private final float[] x, y, vx, vy, angle, angVel;
    private final Color[] colors;
    private final int[]   sizes;
    private boolean active = false;

    public ConfettiAnimation(int screenWidth) {
        x      = new float[NUM_PARTICLES];
        y      = new float[NUM_PARTICLES];
        vx     = new float[NUM_PARTICLES];
        vy     = new float[NUM_PARTICLES];
        angle  = new float[NUM_PARTICLES];
        angVel = new float[NUM_PARTICLES];
        colors = new Color[NUM_PARTICLES];
        sizes  = new int[NUM_PARTICLES];

        Random rng = new Random();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            x[i]      = rng.nextFloat() * screenWidth;
            y[i]      = -rng.nextFloat() * Config.CONFETTI.spawnHeight;
            vx[i]     = (rng.nextFloat() - 0.5f) * Config.CONFETTI.vxRange;
            vy[i]     = Config.CONFETTI.vyBase + rng.nextFloat() * Config.CONFETTI.vyRange;
            angle[i]  = rng.nextFloat() * (float)(Math.PI * 2);
            angVel[i] = (rng.nextFloat() - 0.5f) * Config.CONFETTI.angVelRange;
            colors[i] = Config.CONFETTI.palette[rng.nextInt(Config.CONFETTI.palette.length)];
            sizes[i]  = Config.CONFETTI.sizeMin + rng.nextInt(Config.CONFETTI.sizeRange);
        }
    }

    public void start() { active = true; }
    public boolean isActive() { return active; }

    public void update() {
        if (!active) return;
        for (int i = 0; i < NUM_PARTICLES; i++) {
            x[i]     += vx[i];
            y[i]     += vy[i];
            vy[i]    += Config.CONFETTI.gravity;
            angle[i] += angVel[i];
        }
    }

    public void draw(Graphics2D g, int screenWidth, int screenHeight) {
        if (!active) return;

        AffineTransform origTransform = g.getTransform();

        for (int i = 0; i < NUM_PARTICLES; i++) {
            if (y[i] > screenHeight + Config.CONFETTI.offscreenMargin) continue;

            // Restore base transform, then apply particle transform
            g.setTransform(origTransform);
            g.translate(x[i], y[i]);
            g.rotate(angle[i]);
            g.setColor(colors[i]);
            g.fillRect(-sizes[i] / 2, -sizes[i] / 4, sizes[i], sizes[i] / 2);
        }

        g.setTransform(origTransform);

        // ── "YOU ESCAPED!" banner ─────────────────────────────────────
        String text = Config.CONFETTI.bannerText;
        g.setFont(Config.CONFETTI.bannerFont);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int tx = (screenWidth - tw) / 2;
        int ty = screenHeight / 2 + Config.CONFETTI.bannerTextYOffset;

        g.setColor(Config.CONFETTI.bannerBg);
        g.fillRoundRect(
                tx - Config.CONFETTI.bannerPadX,
                ty - Config.CONFETTI.bannerBoxTopOffset,
                tw + Config.CONFETTI.bannerPadX * 2,
                Config.CONFETTI.bannerBoxHeight,
                Config.CONFETTI.bannerRadius,
                Config.CONFETTI.bannerRadius);

        g.setColor(Config.CONFETTI.bannerShadow);
        g.drawString(text,
                tx + Config.CONFETTI.bannerShadowOffset,
                ty + Config.CONFETTI.bannerShadowOffset);
        g.setColor(Config.CONFETTI.bannerTextColor);
        g.drawString(text, tx, ty);
    }
}
