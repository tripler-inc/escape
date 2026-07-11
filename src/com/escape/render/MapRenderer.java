package com.escape.render;

import com.escape.maze.Maze;
import com.escape.player.Player;
import com.escape.world.Floor;
import com.escape.world.World;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Renders a top-down 2-D map of the current floor.
 * Shows: maze walls, passage cells, player position and direction.
 * Does NOT show coins, key, exit, ladders or holes.
 */
public class MapRenderer {

    private static final Color COL_BORDER  = new Color(15, 15, 15);
    private static final Color COL_WALL    = new Color(55, 55, 65);
    private static final Color COL_PASSAGE = new Color(215, 210, 195);
    private static final Color COL_PLAYER  = new Color(220, 30, 30);

    public void render(Graphics2D g, int width, int height, World world) {
        Player player = world.getPlayer();
        Floor  floor  = world.getCurrentFloor();
        Maze   maze   = floor.getMaze();

        int mazeSize = Maze.SIZE; // 26
        int cellSize = Math.min(width, height) / mazeSize;
        int offsetX  = (width  - cellSize * mazeSize) / 2;
        int offsetY  = (height - cellSize * mazeSize) / 2;

        // ── Background ────────────────────────────────────────────────
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // ── Grid cells ────────────────────────────────────────────────
        for (int r = 0; r < mazeSize; r++) {
            for (int c = 0; c < mazeSize; c++) {
                int px = offsetX + c * cellSize;
                int py = offsetY + r * cellSize;

                boolean isBorder = (r == 0 || r == mazeSize - 1
                                 || c == 0 || c == mazeSize - 1);
                if (isBorder) {
                    g.setColor(COL_BORDER);
                } else if (maze.isWall(r, c)) {
                    g.setColor(COL_WALL);
                } else {
                    g.setColor(COL_PASSAGE);
                }
                g.fillRect(px, py, cellSize, cellSize);
            }
        }

        // ── Player dot ────────────────────────────────────────────────
        float playerScreenX = offsetX + player.px * cellSize;
        float playerScreenY = offsetY + player.py * cellSize;

        int dotDiam = Math.max(4, cellSize * 2 / 3);
        g.setColor(COL_PLAYER);
        g.fillOval(
            (int)(playerScreenX - dotDiam / 2.0),
            (int)(playerScreenY - dotDiam / 2.0),
            dotDiam, dotDiam);

        // ── Direction arrow ───────────────────────────────────────────
        float arrowLen = cellSize * 1.8f;
        float arrowEndX = playerScreenX + (float) Math.cos(player.angle) * arrowLen;
        float arrowEndY = playerScreenY + (float) Math.sin(player.angle) * arrowLen;

        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(COL_PLAYER);
        g.drawLine((int) playerScreenX, (int) playerScreenY,
                   (int) arrowEndX,    (int) arrowEndY);
        g.setStroke(saved);

        // ── Floor label (top-left) ────────────────────────────────────
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Floor " + (player.floor + 1) + " of 3", 10, 24);
    }
}
