package com.escape;

import com.escape.audio.AudioPlayer;
import com.escape.player.Player;
import com.escape.render.FirstPersonRenderer;
import com.escape.render.HUDRenderer;
import com.escape.render.MapRenderer;
import com.escape.render.RenderMode;
import com.escape.ui.ConfettiAnimation;
import com.escape.ui.ExitConfirmDialog;
import com.escape.world.World;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Main game panel.  Houses the game loop (Swing Timer at ~60 fps),
 * keyboard input, and delegates rendering to the appropriate renderer.
 */
public class GamePanel extends JPanel {

    public  static final int WIDTH  = 900;
    public  static final int HEIGHT = 600;

    private static final float MOVE_SPEED = 0.05f;
    private static final float ROT_SPEED  = 0.035f; // radians per tick

    // ── Game objects ──────────────────────────────────────────────────
    private final World world;
    private RenderMode renderMode = RenderMode.FIRST_PERSON;

    private final FirstPersonRenderer fpRenderer;
    private final MapRenderer         mapRenderer;
    private final HUDRenderer         hudRenderer;
    private final ConfettiAnimation   confetti;
    private final AudioPlayer         audioPlayer;

    // ── Input state ───────────────────────────────────────────────────
    private final Set<Integer> heldKeys = new HashSet<>();
    private boolean winStarted = false;

    // ── Constructor ───────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        world       = new World();
        fpRenderer  = new FirstPersonRenderer();
        mapRenderer = new MapRenderer();
        hudRenderer = new HUDRenderer();
        confetti    = new ConfettiAnimation(WIDTH);
        audioPlayer = new AudioPlayer();

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e)  { onKeyPressed(e);  }
            @Override public void keyReleased(KeyEvent e) { heldKeys.remove(e.getKeyCode()); }
        });

        new Timer(16, e -> tick()).start(); // ~62 fps
    }

    // ── Game loop ─────────────────────────────────────────────────────

    private void tick() {
        if (world.getGameState() == World.GameState.PLAYING) {
            handleHeldKeys();
            world.collectItems();
            world.tickLockedMessage();
        } else if (world.getGameState() == World.GameState.WIN && !winStarted) {
            winStarted = true;
            confetti.start();
            audioPlayer.play();
        }

        if (confetti.isActive()) confetti.update();
        repaint();
    }

    private void handleHeldKeys() {
        Player player = world.getPlayer();
        float dirX = (float) Math.cos(player.angle);
        float dirY = (float) Math.sin(player.angle);

        if (heldKeys.contains(KeyEvent.VK_UP)) {
            world.tryMove(dirX * MOVE_SPEED, dirY * MOVE_SPEED);
        }
        if (heldKeys.contains(KeyEvent.VK_DOWN)) {
            world.tryMove(-dirX * MOVE_SPEED, -dirY * MOVE_SPEED);
        }
        if (heldKeys.contains(KeyEvent.VK_LEFT)) {
            player.angle -= ROT_SPEED;
        }
        if (heldKeys.contains(KeyEvent.VK_RIGHT)) {
            player.angle += ROT_SPEED;
        }
    }

    // ── Key events ────────────────────────────────────────────────────

    private void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        heldKeys.add(key);

        // Q: quit confirmation – works in any game state
        if (key == KeyEvent.VK_Q) {
            if (ExitConfirmDialog.confirm(this)) System.exit(0);
            return;
        }

        if (world.getGameState() != World.GameState.PLAYING) return;

        switch (key) {
            case KeyEvent.VK_U: world.tryClimb();   break;
            case KeyEvent.VK_D: world.tryDescend();  break;
            case KeyEvent.VK_M:
                renderMode = (renderMode == RenderMode.FIRST_PERSON)
                        ? RenderMode.MAP : RenderMode.FIRST_PERSON;
                break;
        }
    }

    // ── Painting ──────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (renderMode == RenderMode.FIRST_PERSON || world.getGameState() == World.GameState.WIN) {
            fpRenderer.render(g2, w, h, world);
            hudRenderer.render(g2, w, h, world);
        } else {
            mapRenderer.render(g2, w, h, world);
        }

        if (confetti.isActive()) {
            confetti.draw(g2, w, h);
        }
    }
}
