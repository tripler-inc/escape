package com.escape.render;

import com.escape.Config;
import com.escape.maze.Maze;
import com.escape.player.Player;
import com.escape.world.Floor;
import com.escape.world.ItemType;
import com.escape.world.World;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Map;

/**
 * Renders a top-down 2-D map of the current floor.
 * Shows: maze walls, passage cells, player position and direction.
 * Does NOT show coins or key. Ladders and discovered exit can be marked.
 */
public class MapRenderer {

    private static final Color COL_BORDER  = Config.MAP.border;
    private static final Color COL_WALL    = Config.MAP.wall;
    private static final Color COL_PASSAGE = Config.MAP.passage;
    private static final Color COL_PLAYER  = Config.MAP.player;
    private static final Color COL_LADDER_UP_SEEN = Config.MAP.ladderUpSeen;
    private static final Color COL_LADDER_DOWN = Config.MAP.ladderDown;
    private static final Color COL_MARKER_OUTLINE = Config.MAP.markerOutline;
    private static final Color COL_EXIT_SEEN = Config.MAP.exitSeen;

    public void render(Graphics2D g, int width, int height, World world, long elapsedMs) {
        Player player = world.getPlayer();
        Floor  floor  = world.getCurrentFloor();
        Maze   maze   = floor.getMaze();

        int mazeSize = Maze.SIZE; // 26
        int cellSize = Math.min(width, height) / mazeSize;
        int offsetX  = (width  - cellSize * mazeSize) / 2;
        int offsetY  = (height - cellSize * mazeSize) / 2;

        // ── Background ────────────────────────────────────────────────
        g.setColor(Config.MAP.backgroundColor);
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

        // ── Ladder markers ───────────────────────────────────────────
        drawLadderMarkers(g, world, floor, cellSize, offsetX, offsetY);
        drawExitMarker(g, world, floor, cellSize, offsetX, offsetY);

        int dotDiam = Math.max(4, cellSize * 2 / 3);
        g.setColor(COL_PLAYER);
        g.fillOval(
            (int)(playerScreenX - dotDiam / 2.0),
            (int)(playerScreenY - dotDiam / 2.0),
            dotDiam, dotDiam);

        // ── Direction arrow ───────────────────────────────────────────
        float arrowLen = cellSize * 1.001f;
        float arrowEndX = playerScreenX + (float) Math.cos(player.angle) * arrowLen;
        float arrowEndY = playerScreenY + (float) Math.sin(player.angle) * arrowLen;

        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(COL_PLAYER);
        g.drawLine((int) playerScreenX, (int) playerScreenY,
                   (int) arrowEndX,    (int) arrowEndY);
        g.setStroke(saved);

        // ── Floor label (below HUD) ───────────────────────────────────
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.WHITE);
//        g.drawString("Floor " + (player.floor + 1) + " of 3", 10, 24);
        g.drawString(Config.MAP.labelFloor + (player.floor + 1), 10, 56);
        g.drawString(Config.MAP.labelTime + formatElapsed(elapsedMs), 10, 78);
        g.drawString(Config.MAP.labelScore + world.getScore(), 10, 100);
        int detailY = 122;
        if (world.hasExitBonusCalculated()) {
            g.drawString(Config.MAP.labelTimeBonus + formatSigned(world.getExitBonus()), 10, detailY);
            detailY += 22;
        }
        if (world.getGameState() == World.GameState.WIN && world.hasAllCoinsBonusAwarded()) {
            g.drawString(Config.MAP.labelAllCoinsBonus + formatSigned(world.getAllCoinsBonus()), 10, detailY);
        }
    }

    private String formatSigned(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private String formatElapsed(long elapsedMs) {
        long safeMs = Math.max(0L, elapsedMs);
        long totalSeconds = safeMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void drawLadderMarkers(Graphics2D g, World world, Floor floor,
                                   int cellSize, int offsetX, int offsetY) {
        boolean showUp = world.hasSeenUpLadder(floor.getFloorIndex());
        int markerDiam = Math.max(4, cellSize / 2);

        for (Map.Entry<Point, ItemType> entry : floor.getItems().entrySet()) {
            Point p = entry.getKey();
            ItemType item = entry.getValue();

            boolean draw = false;
            Color color = null;

            if (item == ItemType.HOLE_DOWN) {
                draw = true;
                color = COL_LADDER_DOWN;
            } else if (item == ItemType.LADDER_UP && showUp) {
                draw = true;
                color = COL_LADDER_UP_SEEN;
            }

            if (!draw) continue;

            int cx = offsetX + p.x * cellSize + cellSize / 2;
            int cy = offsetY + p.y * cellSize + cellSize / 2;
            g.setColor(color);
            g.fillOval(cx - markerDiam / 2, cy - markerDiam / 2, markerDiam, markerDiam);
            g.setColor(COL_MARKER_OUTLINE);
            g.drawOval(cx - markerDiam / 2, cy - markerDiam / 2, markerDiam, markerDiam);
        }
    }

    private void drawExitMarker(Graphics2D g, World world, Floor floor,
                                int cellSize, int offsetX, int offsetY) {
        int floorIndex = floor.getFloorIndex();
        if (!world.hasSeenExit(floorIndex) || floorIndex != world.getNumFloors() - 1) {
            return;
        }

        int row = 0;
        int col = world.getExitCol();
        int cellX = offsetX + col * cellSize;
        int cellY = offsetY + row * cellSize;
        int inset = Math.max(1, cellSize / 6);
        int size = Math.max(2, cellSize - inset * 2);

        g.setColor(COL_EXIT_SEEN);
        g.fillRect(cellX + inset, cellY + inset, size, size);
        g.setColor(COL_MARKER_OUTLINE);
        g.drawRect(cellX + inset, cellY + inset, size, size);
    }
}
