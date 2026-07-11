package com.escape.render;

import com.escape.player.Player;
import com.escape.world.ItemType;
import com.escape.world.World;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Draws the HUD overlay on top of the first-person 3-D view:
 *   - Top-left  : gold coin icon + coin count
 *   - Top-right : key icon (only when player has the key)
 *   - Centre-bottom : contextual hint (climb / descend)
 *   - Centre     : locked-exit warning (timed, 2 s)
 */
public class HUDRenderer {

    public void render(Graphics2D g, int width, int height, World world) {
        Player   player = world.getPlayer();
        ItemType cell   = world.getCurrentFloor()
                               .getItemAt(player.getGridRow(), player.getGridCol());

        drawCoinCounter(g, player.coins);

        if (player.hasKey) {
            drawKeyIcon(g, width - 54, 10);
        }

        drawContextHint(g, width, height, cell);

        if (world.isLockedMessageVisible()) {
            drawLockedMessage(g, width, height);
        }
    }

    // ── Coin counter (top-left) ──────────────────────────────────────

    private void drawCoinCounter(Graphics2D g, int count) {
        // Gold disc
        g.setColor(new Color(255, 210, 0));
        g.fillOval(12, 12, 22, 22);
        g.setColor(new Color(180, 140, 0));
        g.drawOval(12, 12, 22, 22);

        // Inner shine
        g.setColor(new Color(255, 240, 120));
        g.fillOval(16, 15, 8, 7);

        // Count text
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("\u00d7 " + count, 40, 30);
    }

    // ── Key icon (top-right) ─────────────────────────────────────────

    private void drawKeyIcon(Graphics2D g, int x, int y) {
        // Head circle
        g.setColor(new Color(255, 210, 0));
        g.fillOval(x, y, 22, 22);
        g.setColor(new Color(180, 140, 0));
        g.drawOval(x, y, 22, 22);

        // Hole
        g.setColor(new Color(40, 25, 0));
        g.fillOval(x + 7, y + 7, 8, 8);

        // Shaft
        g.setColor(new Color(255, 210, 0));
        g.fillRect(x + 22, y + 9, 24, 5);

        // Teeth
        g.fillRect(x + 38, y + 14, 4, 7);
        g.fillRect(x + 30, y + 14, 4, 5);

        // Shaft border
        g.setColor(new Color(180, 140, 0));
        g.drawRect(x + 22, y + 9, 24, 5);
    }

    // ── Contextual hint (bottom-centre) ─────────────────────────────

    private void drawContextHint(Graphics2D g, int width, int height, ItemType cell) {
        String hint = null;
        if (cell == ItemType.LADDER_UP)  hint = "Press U to climb";
        if (cell == ItemType.HOLE_DOWN)  hint = "Press D to descend";
        if (hint == null) return;

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(hint);
        int tx = (width - tw) / 2;
        int ty = height - 28;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(tx - 10, ty - 20, tw + 20, 30, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(hint, tx, ty);
    }

    // ── Locked-exit warning (centre) ────────────────────────────────

    private void drawLockedMessage(Graphics2D g, int width, int height) {
        String msg = "The exit is locked!  Find the key.";
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int tx = (width - tw) / 2;
        int ty = height / 2 - 10;

        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(tx - 16, ty - 30, tw + 32, 44, 12, 12);
        g.setColor(new Color(255, 80, 80));
        g.drawString(msg, tx, ty);
    }
}
