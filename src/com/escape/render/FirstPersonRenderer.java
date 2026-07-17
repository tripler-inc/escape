package com.escape.render;

import com.escape.maze.Maze;
import com.escape.player.Player;
import com.escape.world.Floor;
import com.escape.world.ItemType;
import com.escape.world.World;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws a first-person 3-D corridor view using the classic DDA raycasting
 * algorithm (Lode's raycasting tutorial style).
 *
 * Coordinate convention:
 *   player.px  = column (x in world space)
 *   player.py  = row    (y in world space; row 0 = north)
 *   angle = 0   → east
 *   angle = -PI/2 → north  (i.e. decreasing py)
 */
public class FirstPersonRenderer {

    private static final Color CEILING_COLOR  = new Color(40, 40, 60);
    private static final Color FLOOR_COLOR    = new Color(60, 40, 20);
    private static final Color COIN_GOLD      = new Color(255, 200,   0);
    private static final Color COIN_HIGHLIGHT = new Color(255, 245, 140);
    private static final Color LADDER_RAIL    = new Color(110,  65,  15);
    private static final Color LADDER_RUNG    = new Color(150,  90,  30);
    private static final Color HOLE_DARK      = new Color( 15,  10,   5);
    private static final Color HOLE_EDGE      = new Color( 40,  28,  12);

    // Base wall brightness for each hit-side
    private static final int BASE_NS = 120; // x-side hit  (N/S walls)
    private static final int BASE_EW = 160; // y-side hit  (E/W walls)

    private static final int NUM_FLOORS       = 5;

    /** Per-column wall distance, populated during the DDA pass. */
    private double[] zBuffer = new double[0];

    public void render(Graphics2D g, int width, int height, World world) {
        if (zBuffer.length != width) zBuffer = new double[width];
        Player player = world.getPlayer();
        Floor  floor  = world.getCurrentFloor();
        Maze   maze   = floor.getMaze();

        double px    = player.px;
        double py    = player.py;
        double angle = player.angle;

        double dirX    =  Math.cos(angle);
        double dirY    =  Math.sin(angle);
        double planeX  = -Math.sin(angle) * 0.66; // camera plane → ~66° FOV
        double planeY  =  Math.cos(angle) * 0.66;

        // ── Ceiling and floor fills ────────────────────────────────────
        g.setColor(CEILING_COLOR);
        g.fillRect(0, 0, width, height / 2);
        g.setColor(FLOOR_COLOR);
        g.fillRect(0, height / 2, width, height / 2 + 1);

        // ── Raycast one ray per screen column ─────────────────────────
        for (int x = 0; x < width; x++) {

            double cameraX = 2.0 * x / width - 1.0; // [-1 .. 1]
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) px;
            int mapY = (int) py;

            // How far the ray travels to cross one grid unit in each direction
            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1.0 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1.0 / rayDirY);

            int    stepX, stepY;
            double sideDistX, sideDistY;

            if (rayDirX < 0) {
                stepX     = -1;
                sideDistX = (px - mapX) * deltaDistX;
            } else {
                stepX     =  1;
                sideDistX = (mapX + 1.0 - px) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY     = -1;
                sideDistY = (py - mapY) * deltaDistY;
            } else {
                stepY     =  1;
                sideDistY = (mapY + 1.0 - py) * deltaDistY;
            }

            // ── DDA march ─────────────────────────────────────────────
            int  side   = 0;
            boolean isExit = false;

            while (true) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX      += stepX;
                    side       = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY      += stepY;
                    side       = 1;
                }

                // Bounds guard
                if (mapY < 0 || mapY >= Maze.SIZE || mapX < 0 || mapX >= Maze.SIZE) break;

                if (maze.isWall(mapY, mapX)) {
                    // Check whether this is the exit door
                    if (player.floor == NUM_FLOORS-1 && mapY == 0 && mapX == world.getExitCol()) {
                        isExit = true;
                    }
                    break;
                }
            }

            // ── Perpendicular distance (fisheye-corrected) ────────────
            double perpDist;
            if (side == 0) {
                perpDist = (mapX - px + (1 - stepX) / 2.0) / rayDirX;
            } else {
                perpDist = (mapY - py + (1 - stepY) / 2.0) / rayDirY;
            }
            if (perpDist < 0.01) perpDist = 0.01;

            zBuffer[x] = perpDist; // record for sprite occlusion test

            // ── Wall-stripe height ─────────────────────────────────────
            int lineHeight = (int) (height / perpDist);
            int drawStart  = Math.max(0,          -lineHeight / 2 + height / 2);
            int drawEnd    = Math.min(height - 1,  lineHeight / 2 + height / 2);

            // ── Wall colour ───────────────────────────────────────────
            Color wallColor;
            if (isExit) {
                // Golden door
                double shade = Math.max(0.3, 1.0 / (1.0 + perpDist * 0.15));
                int r = (int)(200 * shade);
                int grn = (int)(160 * shade);
                wallColor = new Color(r, grn, 0);
            } else {
                double shade = Math.max(0.2, 1.0 / (1.0 + perpDist * 0.2));
                int base = (side == 0) ? BASE_NS : BASE_EW;
                int c    = (int)(base * shade);
                wallColor = new Color(c, c, c);
            }

            g.setColor(wallColor);
            g.drawLine(x, drawStart, x, drawEnd);
        }

        // ── Item sprites (coins, ladders, holes) over walls via z-buffer ──
        renderSprites(g, width, height, world, px, py, dirX, dirY, planeX, planeY);
    }

    // ─────────────────────────────────────────────────────────────────
    // Unified sprite pass
    // ─────────────────────────────────────────────────────────────────

    private void renderSprites(Graphics2D g, int width, int height, World world,
                                double px, double py,
                                double dirX, double dirY,
                                double planeX, double planeY) {
        Floor floor = world.getCurrentFloor();

        // Collect COIN, LADDER_UP, HOLE_DOWN with squared player-distance
        // Entry: { worldX, worldY, dist², itemType.ordinal() }
        List<double[]> sprites = new ArrayList<>();
        for (Map.Entry<Point, ItemType> entry : floor.getItems().entrySet()) {
            ItemType type = entry.getValue();
            if (type != ItemType.COIN
                    && type != ItemType.LADDER_UP
                    && type != ItemType.HOLE_DOWN) continue;
            Point  p  = entry.getKey();   // x = col, y = row
            double sx = p.x + 0.5;
            double sy = p.y + 0.5;
            double rx = sx - px;
            double ry = sy - py;
            sprites.add(new double[]{sx, sy, rx * rx + ry * ry, type.ordinal()});
        }
        if (sprites.isEmpty()) return;

        sprites.sort((a, b) -> Double.compare(b[2], a[2])); // farthest first

        double invDet = 1.0 / (planeX * dirY - dirX * planeY);

        for (double[] sp : sprites) {
            double relX = sp[0] - px;
            double relY = sp[1] - py;

            double transformX = invDet * ( dirY   * relX - dirX   * relY);
            double transformY = invDet * (-planeY * relX + planeX * relY);

            if (transformY <= 0.1) continue;

            int screenX = (int)((width / 2.0) * (1.0 + transformX / transformY));

            ItemType type = ItemType.values()[(int) sp[3]];
            switch (type) {
                case COIN:      drawCoinSprite  (g, width, height, screenX, transformY); break;
                case LADDER_UP: drawLadderSprite(g, width, height, screenX, transformY); break;
                case HOLE_DOWN: drawHoleSprite  (g, width, height, screenX, transformY); break;
                default: break;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Coin: floating gold disc
    // ─────────────────────────────────────────────────────────────────

    private void drawCoinSprite(Graphics2D g, int width, int height,
                                 int screenX, double transformY) {
        int halfH = Math.max(2, (int)(height / transformY / 10));
        int halfW = halfH;

//        double coinWorldH = 0.62;
        double coinWorldH = 0.5;
        int centerY = (int)(height / 2.0 + (0.5 - coinWorldH) * height / transformY);

        int startX = Math.max(0,         screenX - halfW);
        int endX   = Math.min(width - 1, screenX + halfW);

        for (int stripe = startX; stripe <= endX; stripe++) {
            if (transformY >= zBuffer[stripe]) continue;

            double nx = (double)(stripe - screenX) / halfW;
            if (Math.abs(nx) > 1.0) continue;

            double nyMax  = Math.sqrt(1.0 - nx * nx);
            int    yTop   = Math.max(0,          (int)(centerY - nyMax * halfH));
            int    yBot   = Math.min(height - 1, (int)(centerY + nyMax * halfH));
            if (yTop >= yBot) continue;

            double nyIn   = nyMax * 0.5;
            int    yHiTop = Math.max(yTop, (int)(centerY - nyIn * halfH));
            int    yHiBot = Math.min(yBot, (int)(centerY + nyIn * halfH));

            if (yTop < yHiTop) {
                g.setColor(COIN_GOLD);
                g.drawLine(stripe, yTop, stripe, yHiTop - 1);
            }
            if (yHiTop <= yHiBot) {
                g.setColor(COIN_HIGHLIGHT);
                g.drawLine(stripe, yHiTop, stripe, yHiBot);
            }
            if (yHiBot < yBot) {
                g.setColor(COIN_GOLD);
                g.drawLine(stripe, yHiBot + 1, stripe, yBot);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Ladder: two wooden rails + evenly-spaced rungs
    // ─────────────────────────────────────────────────────────────────

    private void drawLadderSprite(Graphics2D g, int width, int height,
                                   int screenX, double transformY) {
        int wallH = Math.abs((int)(height / transformY));
        int halfH = wallH / 2;
        int halfW = Math.max(2, wallH / 5);
        if (halfH < 2) return;

        int centerY = height / 2;
        int topY    = Math.max(0,          centerY - halfH);
        int botY    = Math.min(height - 1, centerY + halfH);
        int totalH  = botY - topY;
        if (totalH < 2) return;

        int startX = Math.max(0,         screenX - halfW);
        int endX   = Math.min(width - 1, screenX + halfW);

        // Pre-compute rung spans (fixed count so gaps stay visible at any distance)
        int numRungs  = 5;
        int rungThick = Math.max(1, totalH / (numRungs * 16));
        int[] rungTop = new int[numRungs];
        int[] rungBot = new int[numRungs];
        for (int r = 0; r < numRungs; r++) {
            int cy     = topY + totalH * (r + 1) / (numRungs + 1);
            rungTop[r] = Math.max(topY, cy - rungThick);
            rungBot[r] = Math.min(botY, cy + rungThick);
        }

        for (int stripe = startX; stripe <= endX; stripe++) {
            if (transformY >= zBuffer[stripe]) continue;

            // Rails on the left and right edges only (4 px wide each side)
            if (stripe <= startX + 3 || stripe >= endX - 3) {
                g.setColor(LADDER_RAIL);
                g.drawLine(stripe, topY, stripe, botY);
            }
            // Rungs cross every stripe
            g.setColor(LADDER_RUNG);
            for (int r = 0; r < numRungs; r++) {
                g.drawLine(stripe, rungTop[r], stripe, rungBot[r]);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Hole: dark oval resting on the floor
    // ─────────────────────────────────────────────────────────────────

    private void drawHoleSprite(Graphics2D g, int width, int height,
                                 int screenX, double transformY) {
        // Very flat, wide oval — simulates a circle foreshortened on the floor
        int halfH = Math.max(2, (int)(height / transformY / 12));
        int halfW = (int)(halfH * 4.5);  // strongly foreshortened

        // Push centre below the floor line so it reads as a ground-level hole
        // at all distances (coefficient 0.72 > 0.5 so it sits below the horizon)
        int centerY = (int)(height / 2.0 + 0.72 * height / transformY);

        int startX = Math.max(0,         screenX - halfW);
        int endX   = Math.min(width - 1, screenX + halfW);

        for (int stripe = startX; stripe <= endX; stripe++) {
            if (transformY >= zBuffer[stripe]) continue;

            double nx = (double)(stripe - screenX) / halfW;
            if (Math.abs(nx) > 1.0) continue;

            double nyMax = Math.sqrt(1.0 - nx * nx);
            int yTop = Math.max(0,          (int)(centerY - nyMax * halfH));
            int yBot = Math.min(height - 1, (int)(centerY + nyMax * halfH));
            if (yTop >= yBot) continue;

            // Darker inner region, slightly lighter edge ring
            int inTop = Math.max(yTop, (int)(centerY - nyMax * halfH * 0.55));
            int inBot = Math.min(yBot, (int)(centerY + nyMax * halfH * 0.55));

            if (yTop < inTop) {
                g.setColor(HOLE_EDGE); g.drawLine(stripe, yTop, stripe, inTop - 1);
            }
            if (inTop <= inBot) {
                g.setColor(HOLE_DARK); g.drawLine(stripe, inTop, stripe, inBot);
            }
            if (inBot < yBot) {
                g.setColor(HOLE_EDGE); g.drawLine(stripe, inBot + 1, stripe, yBot);
            }
        }
    }
}
