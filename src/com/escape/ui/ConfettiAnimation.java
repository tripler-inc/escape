package com.escape.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Random;

/**
 * A simple particle-based confetti shower drawn over the win screen.
 * Call start() once when the player wins, then update() + draw() every tick.
 */
public class ConfettiAnimation {

    private static final int NUM_PARTICLES = 150;

    private final float[] x, y, vx, vy, angle, angVel;
    private final Color[] colors;
    private final int[]   sizes;
    private boolean active = false;

    private static final Color[] PALETTE = {
        new Color(255,  50,  50),
        new Color( 50, 200,  50),
        new Color( 50, 100, 255),
        new Color(255, 215,   0),
        new Color(255, 100, 200),
        new Color(100, 255, 255)
    };

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
            y[i]      = -rng.nextFloat() * 300;          // stagger above top
            vx[i]     = (rng.nextFloat() - 0.5f) * 2.0f;
            vy[i]     = 1.0f + rng.nextFloat() * 3.0f;
            angle[i]  = rng.nextFloat() * (float)(Math.PI * 2);
            angVel[i] = (rng.nextFloat() - 0.5f) * 0.18f;
            colors[i] = PALETTE[rng.nextInt(PALETTE.length)];
            sizes[i]  = 6 + rng.nextInt(9);
        }
    }

    public void start() { active = true; }
    public boolean isActive() { return active; }

    public void update() {
        if (!active) return;
        for (int i = 0; i < NUM_PARTICLES; i++) {
            x[i]     += vx[i];
            y[i]     += vy[i];
            vy[i]    += 0.05f;   // gravity
            angle[i] += angVel[i];
        }
    }

    public void draw(Graphics2D g, int screenWidth, int screenHeight) {
        if (!active) return;

        AffineTransform origTransform = g.getTransform();

        for (int i = 0; i < NUM_PARTICLES; i++) {
            if (y[i] > screenHeight + 20) continue; // already off-screen

            // Restore base transform, then apply particle transform
            g.setTransform(origTransform);
            g.translate(x[i], y[i]);
            g.rotate(angle[i]);
            g.setColor(colors[i]);
            g.fillRect(-sizes[i] / 2, -sizes[i] / 4, sizes[i], sizes[i] / 2);
        }

        g.setTransform(origTransform);

        // ── "YOU ESCAPED!" banner ─────────────────────────────────────
        String text = "YOU ESCAPED!";
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int tx = (screenWidth - tw) / 2;
        int ty = screenHeight / 2 + 18;

        // Semi-transparent backing
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(tx - 24, ty - 56, tw + 48, 72, 18, 18);

        // Gold text with dark shadow
        g.setColor(new Color(40, 30, 0));
        g.drawString(text, tx + 3, ty + 3);
        g.setColor(new Color(255, 215, 0));
        g.drawString(text, tx, ty);
    }
}
