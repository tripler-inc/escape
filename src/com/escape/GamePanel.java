package com.escape;

import com.escape.audio.AudioPlayer;
import com.escape.player.Player;
import com.escape.render.FirstPersonRenderer;
import com.escape.render.HUDRenderer;
import com.escape.render.MapRenderer;
import com.escape.render.RenderMode;
import com.escape.ui.ConfettiAnimation;
import com.escape.ui.ConfigDialog;
import com.escape.ui.ExitConfirmDialog;
import com.escape.ui.HelpOverlay;
import com.escape.world.World;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Window;
import java.util.HashSet;
import java.util.Set;

/**
 * Main game panel.  Houses the game loop (Swing Timer at ~60 fps),
 * keyboard input, and delegates rendering to the appropriate renderer.
 */
public class GamePanel extends JPanel {

    public  static final int WIDTH  = Config.GAME_PANEL.width;
    public  static final int HEIGHT = Config.GAME_PANEL.height;

    private static final float MOVE_SPEED = Config.GAME_PANEL.moveSpeed;
    private static final float ROT_SPEED  = Config.GAME_PANEL.rotSpeed;
    private static final float MAX_MOVE_SPEED = Config.GAME_PANEL.maxMoveSpeed;
    private static final float SHIFT_SPEED_MULTIPLIER = 2.0f;
    private static final float MAX_ROT_SPEED  = Config.GAME_PANEL.maxRotSpeed;
    private static final float MOVE_ACCEL = Config.GAME_PANEL.moveAccel;
    private static final float ROT_ACCEL  = Config.GAME_PANEL.rotAccel;
    private static final int FOOTSTEP_INTERVAL_MS = Config.SOUND.footstepIntervalMs;
    private static final int MAP_HINT_DURATION_MS = Config.GAME_PANEL.mapHintDurationMs;
    private float moveSpeed = MOVE_SPEED; // Set to default
    private float rotSpeed  = ROT_SPEED;  // Set to default

    // ── Game objects ──────────────────────────────────────────────────
    private final World world;
    private RenderMode renderMode = RenderMode.FIRST_PERSON;

    private final FirstPersonRenderer fpRenderer;
    private final MapRenderer         mapRenderer;
    private final HUDRenderer         hudRenderer;
    private final ConfettiAnimation   confetti;
    private final AudioPlayer         audioPlayer;
    private final HelpOverlay         helpOverlay;

    // ── Input state ───────────────────────────────────────────────────
    private final Set<Integer> heldKeys = new HashSet<>();
    private boolean winStarted = false;
    private int footstepCooldownMs = 0;
    private long runStartMs = -1L;
    private long runElapsedMs = 0L;
    private long helpOpenedAtMs = -1L;
    private int mapHintRemainingMs = 0;
    private boolean helpVisible = false;
    private boolean waitingForStart = true;

    // ── Constructor ───────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Config.GAME_PANEL.backgroundColor);
        setFocusable(true);

        world       = new World();
        fpRenderer  = new FirstPersonRenderer();
        mapRenderer = new MapRenderer();
        hudRenderer = new HUDRenderer();
        confetti    = new ConfettiAnimation(WIDTH);
        audioPlayer = new AudioPlayer();
        helpOverlay = new HelpOverlay();

        // Startup screen: show help immediately and wait for Esc to begin the run.
        helpOverlay.reload();
        helpVisible = true;

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e)  {
                onKeyPressed(e);
            }
            @Override public void keyReleased(KeyEvent e) {
                heldKeys.remove(e.getKeyCode());
                // Only reset accel on an axis when neither key on that axis is held.
                if (!heldKeys.contains(KeyEvent.VK_LEFT) && !heldKeys.contains(KeyEvent.VK_RIGHT)) {
                    rotSpeed = ROT_SPEED;
                }
                if (!heldKeys.contains(KeyEvent.VK_UP) && !heldKeys.contains(KeyEvent.VK_DOWN)) {
                    moveSpeed = MOVE_SPEED;
                }
            }
        });

        addMouseWheelListener(e -> {
            if (helpVisible) {
                helpOverlay.scrollBy(e.getWheelRotation() * 3);
                repaint();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                resetInputState();
            }
        });

        new Timer(Config.GAME_PANEL.tickMs, e -> safeTick()).start();
    }

    // ── Game loop ─────────────────────────────────────────────────────

    private void safeTick() {
        try {
            tick();
        } catch (Throwable t) {
            resetInputState();
            System.err.println("Game loop error: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void tick() {
        if (world.getGameState() == World.GameState.PLAYING) {
            if (!helpVisible) {
                handleHeldKeys();
                if (runStartMs >= 0L) {
                    runElapsedMs = System.currentTimeMillis() - runStartMs;
                }
                if (mapHintRemainingMs > 0) {
                    mapHintRemainingMs = Math.max(0, mapHintRemainingMs - Config.GAME_PANEL.tickMs);
                }

                if (renderMode == RenderMode.MAP && !world.hasMapForCurrentFloor()) {
                    renderMode = RenderMode.FIRST_PERSON;
                }

                // only collect items when in first-person mode
                if (renderMode == RenderMode.FIRST_PERSON) {
                    world.collectItems();
                }
                world.tickLockedMessage();
            }
        } else if (world.getGameState() == World.GameState.WIN && !winStarted) {
            winStarted = true;
            if (runStartMs >= 0L) {
                runElapsedMs = System.currentTimeMillis() - runStartMs;
            }
            world.applyExitTimeBonus(runElapsedMs);
            confetti.start();
            audioPlayer.play();
        }

        if (confetti.isActive()) confetti.update();
        repaint();
    }

    private void resetInputState() {
        heldKeys.clear();
        rotSpeed = ROT_SPEED;
        moveSpeed = MOVE_SPEED;
        footstepCooldownMs = 0;
    }

    private void handleHeldKeys() {
        Player player = world.getPlayer();
        float dirX = (float) Math.cos(player.angle);
        float dirY = (float) Math.sin(player.angle);
        boolean shiftHeld = heldKeys.contains(KeyEvent.VK_SHIFT);
        float currentMaxMoveSpeed = shiftHeld ? MAX_MOVE_SPEED * SHIFT_SPEED_MULTIPLIER : MAX_MOVE_SPEED;
        if (moveSpeed > currentMaxMoveSpeed) {
            moveSpeed = currentMaxMoveSpeed;
        }
        float beforePx = player.px;
        float beforePy = player.py;
        boolean moved = false;

        if (heldKeys.contains(KeyEvent.VK_UP)) {
            world.tryMove(dirX * moveSpeed, dirY * moveSpeed);
            moved |= player.px != beforePx || player.py != beforePy;
            if (moveSpeed < currentMaxMoveSpeed) {
                moveSpeed = Math.min(moveSpeed + MOVE_ACCEL, currentMaxMoveSpeed);
            }
        }
        if (heldKeys.contains(KeyEvent.VK_DOWN)) {
            world.tryMove(-dirX * moveSpeed, -dirY * moveSpeed);
            moved |= player.px != beforePx || player.py != beforePy;
            if (moveSpeed < currentMaxMoveSpeed) {
                moveSpeed = Math.min(moveSpeed + MOVE_ACCEL, currentMaxMoveSpeed);
            }
        }
        if (heldKeys.contains(KeyEvent.VK_LEFT)) {
            player.angle -= rotSpeed;
            if (rotSpeed < MAX_ROT_SPEED) {
                rotSpeed += ROT_ACCEL;
            }
        }
        if (heldKeys.contains(KeyEvent.VK_RIGHT)) {
            player.angle += rotSpeed;
            if (rotSpeed < MAX_ROT_SPEED) {
                rotSpeed += ROT_ACCEL;
            }
        }

        if (footstepCooldownMs > 0) {
            int cooldownStep = shiftHeld
                    ? Math.max(1, Math.round(Config.GAME_PANEL.tickMs * SHIFT_SPEED_MULTIPLIER))
                    : Config.GAME_PANEL.tickMs;
            footstepCooldownMs -= cooldownStep;
        }
        if (moved && footstepCooldownMs <= 0) {
            world.soundPlayer.playFootsteps();
            footstepCooldownMs = shiftHeld
                    ? Math.max(1, Math.round(FOOTSTEP_INTERVAL_MS / SHIFT_SPEED_MULTIPLIER))
                    : FOOTSTEP_INTERVAL_MS;
        }
    }

    // ── Key events ────────────────────────────────────────────────────

    private void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE && helpVisible) {
            if (waitingForStart) {
                waitingForStart = false;
                helpOpenedAtMs = -1L;
                helpVisible = false;
                runStartMs = System.currentTimeMillis();
                runElapsedMs = 0L;
                resetInputState();
                return;
            }

            if (helpOpenedAtMs >= 0L && runStartMs >= 0L) {
                long pausedMs = Math.max(0L, System.currentTimeMillis() - helpOpenedAtMs);
                runStartMs += pausedMs;
            }
            helpOpenedAtMs = -1L;
            helpVisible = false;
            resetInputState();
            return;
        }

        if (key == KeyEvent.VK_H) {
            if (helpVisible) {
                // Keep startup flow explicit: Esc starts the run from the intro overlay.
                if (waitingForStart) {
                    return;
                }
                if (helpOpenedAtMs >= 0L && runStartMs >= 0L) {
                    long pausedMs = Math.max(0L, System.currentTimeMillis() - helpOpenedAtMs);
                    runStartMs += pausedMs;
                }
                helpOpenedAtMs = -1L;
                helpVisible = false;
                resetInputState();
                return;
            }

            helpOverlay.reload();
            helpOpenedAtMs = System.currentTimeMillis();
            helpVisible = true;
            resetInputState();
            return;
        }

        if (helpVisible && key == KeyEvent.VK_C) {
            openConfigScreen();
            return;
        }

        if (helpVisible) {
            switch (key) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_PAGE_UP:
                    helpOverlay.scrollBy(key == KeyEvent.VK_PAGE_UP ? -HelpOverlay.SCROLL_PAGE_LINES : -1);
                    repaint();
                    return;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_PAGE_DOWN:
                    helpOverlay.scrollBy(key == KeyEvent.VK_PAGE_DOWN ? HelpOverlay.SCROLL_PAGE_LINES : 1);
                    repaint();
                    return;
                case KeyEvent.VK_HOME:
                    helpOverlay.scrollToTop();
                    repaint();
                    return;
                case KeyEvent.VK_END:
                    helpOverlay.scrollToBottom();
                    repaint();
                    return;
            }
        }

        // Q: quit confirmation – works in any game state
        if (key == KeyEvent.VK_Q) {
            resetInputState();
            if (ExitConfirmDialog.confirm(this)) System.exit(0);
            return;
        }

        if (helpVisible) return;

        heldKeys.add(key);

        if (world.getGameState() != World.GameState.PLAYING) return;

        switch (key) {
            case KeyEvent.VK_U: world.tryClimb();   break;
            case KeyEvent.VK_D: world.tryDescend();  break;
            case KeyEvent.VK_M:
                if (renderMode == RenderMode.MAP) {
                    renderMode = RenderMode.FIRST_PERSON;
                } else if (world.hasMapForCurrentFloor()) {
                    renderMode = RenderMode.MAP;
                } else {
                    mapHintRemainingMs = MAP_HINT_DURATION_MS;
                }
                break;
        }
    }

    private void openConfigScreen() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        ConfigDialog.showDialog(owner);
        requestFocusInWindow();
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

        boolean showMapView = world.getGameState() == World.GameState.WIN
            || renderMode == RenderMode.MAP;

        if (!showMapView) {
            fpRenderer.render(g2, w, h, world);
            if (world.getGameState() == World.GameState.PLAYING) {
                hudRenderer.renderContextHint(g2, w, h, world);
                hudRenderer.renderLockedMessage(g2, w, h, world);
                if (world.getPlayer().hasCompass) {
                    hudRenderer.renderCompassRose(g2, w, h, world);
                }
                if (world.getPlayer().hasPad) {
                    hudRenderer.renderPadStatus(g2, w, h, world);
                }
                if (mapHintRemainingMs > 0) {
                    hudRenderer.renderMapHint(g2, w);
                }
            }
        } else {
            mapRenderer.render(g2, w, h, world, runElapsedMs);
            hudRenderer.render(g2, w, h, world);
        }

        if (confetti.isActive()) {
            confetti.draw(g2, w, h);
        }

        if (helpVisible) {
            helpOverlay.draw(g2, w, h);
            if (waitingForStart) {
                helpOverlay.drawStartupPrompt(g2, w, h);
            }
        }
    }
}
