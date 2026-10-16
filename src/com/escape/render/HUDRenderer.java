package com.escape.render;

import com.escape.Config;
import com.escape.player.Player;
import com.escape.world.ItemType;
import com.escape.world.World;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Draws the HUD overlay on top of the map view:
 *   - Top-left  : gold coin icon + coin count
 *   - Top-right : key icon (only when player has the key)
 *   - Centre-bottom : contextual hint (climb / descend)
 *   - Centre     : locked-exit warning (timed, 2 s)
 */
public class HUDRenderer {

    public void render(Graphics2D g, int width, int height, World world) {
        Player   player = world.getPlayer();

        drawCoinCounter(g, player.coins);

        if (player.hasKey) {
            drawKeyIcon(g, width - 54, 10);
        }

        if (world.isLockedMessageVisible()) {
            drawLockedMessage(g, width, height);
        }
    }

    public void renderCompassRose(Graphics2D g, int width, int height, World world) {
        if (!world.getPlayer().hasCompass) return;
        drawCompassRose(g, width - 76, 18, world.getPlayer());
    }

    public void renderPadStatus(Graphics2D g, int width, int height, World world) {
        if (!world.getPlayer().hasPad) return;
        drawPadStatus(g, width - 116, 62);
    }

    public void renderContextHint(Graphics2D g, int width, int height, World world) {
        Player player = world.getPlayer();
        ItemType cell = world.getCurrentFloor()
                             .getItemAt(player.getGridRow(), player.getGridCol());
        drawContextHint(g, width, height, cell);
    }

    public void renderLockedMessage(Graphics2D g, int width, int height, World world) {
        if (world.isLockedMessageVisible()) {
            drawLockedMessage(g, width, height);
        }
    }

    public void renderMapHint(Graphics2D g, int width) {
        String message = Config.HUD.hintMapMissing;
        g.setFont(Config.HUD.mapHintFont);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(message);
        int tx = (width - tw) / 2;
        int ty = 40;

        g.setColor(Config.HUD.mapHintFill);
        g.fillRoundRect(tx - 16, ty - 30, tw + 32, 44, 12, 12);
        g.setColor(Config.HUD.mapHintText);
        g.drawString(message, tx, ty);
    }

    // ── Coin counter (top-left) ──────────────────────────────────────

    private void drawCoinCounter(Graphics2D g, int count) {
        // Gold disc
        g.setColor(Config.HUD.coin);
        g.fillOval(12, 12, 22, 22);
        g.setColor(Config.HUD.coinBorder);
        g.drawOval(12, 12, 22, 22);

        // Inner shine
        g.setColor(Config.HUD.coinShine);
        g.fillOval(16, 15, 8, 7);

        // Count text
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Config.HUD.coinText);
        g.drawString("\u00d7 " + count, 40, 30);
    }

    // ── Compass rose (top-right, first-person HUD) ─────────────────

    private void drawCompassRose(Graphics2D g, int x, int y, Player player) {
        final int radius = 22;
        final int cx = x + radius;
        final int cy = y + radius;

        g.setColor(Config.HUD.compassBg);
        g.fillOval(cx - radius - 5, cy - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
        g.setColor(Config.HUD.compassRing);
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

        g.drawLine(cx, cy - radius, cx, cy - radius + 5);
        g.drawLine(cx, cy + radius, cx, cy + radius - 5);
        g.drawLine(cx - radius, cy, cx - radius + 5, cy);
        g.drawLine(cx + radius, cy, cx + radius - 5, cy);

        g.setColor(Config.HUD.compassNeedle);
        double heading = -player.angle;
        int needleX = (int) Math.round(Math.cos(heading) * (radius - 6));
        int needleY = (int) Math.round(Math.sin(heading) * (radius - 6));
        g.drawLine(cx, cy, cx + needleX, cy + needleY);
        g.fillOval(cx + needleX - 3, cy + needleY - 3, 6, 6);
        g.fillOval(cx - 3, cy - 3, 6, 6);
    }

    private void drawPadStatus(Graphics2D g, int x, int y) {
        int w = 42;
        int h = 26;
        g.setColor(Config.HUD.padColor);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(Config.HUD.padBorder);
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setColor(Config.HUD.padScreen);
        g.fillRoundRect(x + 6, y + 5, w - 12, h - 10, 5, 5);
        g.setColor(Config.HUD.padHighlight);
        g.fillRoundRect(x + 10, y + 8, w - 20, 4, 2, 2);
    }

    // ── Key icon (top-right) ─────────────────────────────────────────

    private void drawKeyIcon(Graphics2D g, int x, int y) {
        // Head circle
        g.setColor(Config.HUD.keyColor);
        g.fillOval(x, y, 22, 22);
        g.setColor(Config.HUD.keyBorder);
        g.drawOval(x, y, 22, 22);

        // Hole
        g.setColor(Config.HUD.keyHole);
        g.fillOval(x + 7, y + 7, 8, 8);

        // Shaft
        g.setColor(Config.HUD.keyShaft);
        g.fillRect(x + 22, y + 9, 24, 5);

        // Teeth
        g.fillRect(x + 38, y + 14, 4, 7);
        g.fillRect(x + 30, y + 14, 4, 5);

        // Shaft border
        g.setColor(Config.HUD.keyShaftBorder);
        g.drawRect(x + 22, y + 9, 24, 5);
    }

    // ── Contextual hint (bottom-centre) ─────────────────────────────

    private void drawContextHint(Graphics2D g, int width, int height, ItemType cell) {
        String hint = null;
        if (cell == ItemType.LADDER_UP)  hint = Config.HUD.hintUp;
        if (cell == ItemType.HOLE_DOWN)  hint = Config.HUD.hintDown;
        if (hint == null) return;

        g.setFont(Config.HUD.hintFont);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(hint);
        int tx = (width - tw) / 2;
        int ty = height - 28;

        g.setColor(Config.HUD.hintFill);
        g.fillRoundRect(tx - 10, ty - 20, tw + 20, 30, 10, 10);
        g.setColor(Config.HUD.hintText);
        g.drawString(hint, tx, ty);
    }

    // ── Locked-exit warning (centre) ────────────────────────────────

    private void drawLockedMessage(Graphics2D g, int width, int height) {
        String msg = Config.HUD.hintLocked;
        g.setFont(Config.HUD.lockedFont);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int tx = (width - tw) / 2;
        int ty = height / 2 - 10;

        g.setColor(Config.HUD.lockedFill);
        g.fillRoundRect(tx - 16, ty - 30, tw + 32, 44, 12, 12);
        g.setColor(Config.HUD.lockedText);
        g.drawString(msg, tx, ty);
    }
}
