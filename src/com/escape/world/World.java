package com.escape.world;

import com.escape.maze.Cell;
import com.escape.maze.Maze;
import com.escape.maze.MazeGenerator;
import com.escape.player.Player;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class World {

    public enum GameState { PLAYING, WIN }

    private static final int NUM_FLOORS      = 3;
    private static final int COINS_PER_FLOOR = 5;
    private static final int[] ODD = {1,3,5,7,9,11,13,15,17,19,21,23};

    // ---------------------------------------------------------------
    private final Floor[]  floors;
    private final Player   player;
    private       GameState gameState;

    private final int startCol;
    private final int exitCol;
    private final Point ladderA;   // connects floor 0 <-> 1 at this grid cell
    private final Point ladderB;   // connects floor 1 <-> 2 at this grid cell

    private int lockedMessageTicks = 0;
    public  static final int LOCKED_MSG_DURATION = 120; // 2 s @ 60 fps

    // ---------------------------------------------------------------
    public World() {
        Random rng = new Random();
        floors = new Floor[NUM_FLOORS];

        // ── Phase 1: generate all three mazes ──────────────────────
        for (int i = 0; i < NUM_FLOORS; i++) {
            floors[i] = new Floor(MazeGenerator.generate(rng), i);
        }

        // ── Start position: floor 0, row 23, random odd col ─────────
        startCol = ODD[rng.nextInt(ODD.length)];
        floors[0].getMaze().setCell(23, startCol, Cell.PASSAGE);

        // ── Exit position: floor 2, row 0 (north wall), random odd col
        // Guarantee cell (1, exitCol) on floor 2 is a passage so the
        // player can approach the door.
        exitCol = ODD[rng.nextInt(ODD.length)];
        floors[2].getMaze().setCell(1, exitCol, Cell.PASSAGE);
        floors[2].placeItem(0, exitCol, ItemType.EXIT);

        // ── Phase 2: place ladders ───────────────────────────────────
        Set<Point> exclude0 = new HashSet<>();
        exclude0.add(new Point(startCol, 23)); // avoid start cell
        ladderA = pickRoomCell(rng, floors[0].getMaze(), exclude0);
        floors[0].placeItem(ladderA.y, ladderA.x, ItemType.LADDER_UP);
        floors[1].placeItem(ladderA.y, ladderA.x, ItemType.HOLE_DOWN);

        Set<Point> exclude1 = new HashSet<>();
        exclude1.add(ladderA);                         // can't reuse same position
        exclude1.add(new Point(exitCol, 1));            // keep exit approach clear
        ladderB = pickRoomCell(rng, floors[1].getMaze(), exclude1);
        floors[1].placeItem(ladderB.y, ladderB.x, ItemType.LADDER_UP);
        floors[2].placeItem(ladderB.y, ladderB.x, ItemType.HOLE_DOWN);

        // ── Phase 3: place 5 coins per floor ────────────────────────
        for (int f = 0; f < NUM_FLOORS; f++) {
            int placed = 0;
            List<Point> cells = shuffledRoomCells(rng, floors[f].getMaze());
            for (Point p : cells) {
                if (placed >= COINS_PER_FLOOR) break;
                if (isReserved(p, f)) continue;
                if (floors[f].hasItem(p.y, p.x)) continue;
                floors[f].placeItem(p.y, p.x, ItemType.COIN);
                placed++;
            }
        }

        // ── Phase 4: place key (any floor, avoiding start/ladders/coins)
        List<Integer> floorOrder = new ArrayList<>(Arrays.asList(0, 1, 2));
        Collections.shuffle(floorOrder, rng);
        outer:
        for (int f : floorOrder) {
            List<Point> cells = shuffledRoomCells(rng, floors[f].getMaze());
            for (Point p : cells) {
                if (isReserved(p, f)) continue;
                if (floors[f].hasItem(p.y, p.x)) continue;
                floors[f].placeItem(p.y, p.x, ItemType.KEY);
                break outer;
            }
        }

        // ── Create player ────────────────────────────────────────────
        player    = new Player(startCol + 0.5f, 23.5f, 0);
        gameState = GameState.PLAYING;
    }

    // ── helpers ─────────────────────────────────────────────────────

    /** Returns true for cells that should never hold a coin/key. */
    private boolean isReserved(Point p, int floor) {
        if (p.equals(ladderA) || p.equals(ladderB)) return true;
        if (floor == 0 && p.x == startCol && p.y == 23) return true;
        if (floor == 2 && p.x == exitCol  && p.y == 1)  return true;
        return false;
    }

    private Point pickRoomCell(Random rng, Maze maze, Set<Point> excluded) {
        List<Point> cells = shuffledRoomCells(rng, maze);
        for (Point p : cells) {
            if (!excluded.contains(p)) return p;
        }
        return cells.get(0); // should never be needed
    }

    private List<Point> shuffledRoomCells(Random rng, Maze maze) {
        List<Point> list = new ArrayList<>();
        for (int r = 1; r <= 23; r += 2) {
            for (int c = 1; c <= 23; c += 2) {
                if (maze.isPassage(r, c)) {
                    list.add(new Point(c, r)); // x=col, y=row
                }
            }
        }
        Collections.shuffle(list, rng);
        return list;
    }

    // ── public accessors ────────────────────────────────────────────

    public Floor     getFloor(int index)   { return floors[index]; }
    public Floor     getCurrentFloor()     { return floors[player.floor]; }
    public Player    getPlayer()           { return player; }
    public GameState getGameState()        { return gameState; }
    public int       getExitCol()          { return exitCol; }
    public int       getStartCol()         { return startCol; }

    public boolean isLockedMessageVisible() { return lockedMessageTicks > 0; }
    public void    tickLockedMessage()      { if (lockedMessageTicks > 0) lockedMessageTicks--; }

    // ── game actions ─────────────────────────────────────────────────

    /**
     * Attempts to move the player by (dx, dy).
     * Uses per-axis slide collision so the player glides along walls.
     * Returns true only when the win condition is triggered.
     */
    public boolean tryMove(float dx, float dy) {
        if (gameState != GameState.PLAYING) return false;

        Maze  maze  = getCurrentFloor().getMaze();
        float newPx = player.px + dx;
        float newPy = player.py + dy;

        // Safe integer grid indices
        int newRow = Math.max(0, Math.min(Maze.SIZE - 1, (int) newPy));
        int newCol = Math.max(0, Math.min(Maze.SIZE - 1, (int) newPx));

        // Exit cell intercept (floor 2, row 0)
        if (player.floor == 2 && newRow == 0 && newCol == exitCol) {
            if (player.hasKey) {
                gameState = GameState.WIN;
                return true;
            } else {
                lockedMessageTicks = LOCKED_MSG_DURATION;
                return false;
            }
        }

        // Slide collision: test each axis independently
        boolean canMoveX = !maze.isWall((int) player.py, newCol);
        boolean canMoveY = !maze.isWall(newRow, (int) player.px);

        if (canMoveX) player.px = newPx;
        if (canMoveY) player.py = newPy;

        return false;
    }

    /** Collect COIN or KEY at the player's current grid cell. */
    public void collectItems() {
        if (gameState != GameState.PLAYING) return;
        int      row  = player.getGridRow();
        int      col  = player.getGridCol();
        Floor    f    = getCurrentFloor();
        ItemType item = f.getItemAt(row, col);
        if (item == ItemType.COIN) {
            player.coins++;
            f.removeItem(row, col);
        } else if (item == ItemType.KEY) {
            player.hasKey = true;
            f.removeItem(row, col);
        }
    }

    /** Move player up one floor via a LADDER_UP cell. */
    public boolean tryClimb() {
        if (gameState != GameState.PLAYING) return false;
        int row = player.getGridRow();
        int col = player.getGridCol();
        if (getCurrentFloor().getItemAt(row, col) == ItemType.LADDER_UP
                && player.floor < NUM_FLOORS - 1) {
            player.floor++;
            player.px = col + 0.5f;
            player.py = row + 0.5f;
            return true;
        }
        return false;
    }

    /** Move player down one floor via a HOLE_DOWN cell. */
    public boolean tryDescend() {
        if (gameState != GameState.PLAYING) return false;
        int row = player.getGridRow();
        int col = player.getGridCol();
        if (getCurrentFloor().getItemAt(row, col) == ItemType.HOLE_DOWN
                && player.floor > 0) {
            player.floor--;
            player.px = col + 0.5f;
            player.py = row + 0.5f;
            return true;
        }
        return false;
    }
}
