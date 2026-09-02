package com.escape.world;

import com.escape.Config;
import com.escape.maze.Cell;
import com.escape.maze.Maze;
import com.escape.maze.MazeGenerator;
import com.escape.player.Player;
import com.escape.audio.SoundPlayer;

import java.awt.Point;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class World {

    public enum GameState { PLAYING, WIN }

    private static final int NUM_FLOORS      = Config.WORLD.numFloors;
    private static final int COINS_PER_FLOOR = Config.WORLD.coinsPerFloor;
    private static final int ROWS = Config.WORLD.rows;
    private static final int COLS = Config.WORLD.cols;
    private static final int[] ODD = Config.WORLD.odd;
    private static final int COIN_SCORE = Config.WORLD.scoreCoin;
    private static final int KEY_SCORE = Config.WORLD.scoreKey;
    private static final int MAP_SCORE = Config.WORLD.scoreMap;
    private static final int BONUS_SECONDS_PER_FLOOR = Config.WORLD.bonusSecondsPerFloor;

    // ---------------------------------------------------------------
    private final Floor[]   floors;
    private final Player    player;
    private       GameState gameState;

    private final int     startCol;
    private final int     exitCol;
    private final Point[] ladders;   // connects floor 0 <-> 1 at this grid cell
    private final boolean[] seenUpLadders;
    private final boolean[] seenExits;

    private int lockedMessageTicks = 0;
    public  static final int LOCKED_MSG_DURATION = Config.WORLD.lockedMsgDuration;
    private int score = 0;
    private int exitBonus = 0;
    private boolean exitBonusApplied = false;
    private int totalCoinsPlaced = 0;
    private boolean allCoinsBonusApplied = false;

    public final SoundPlayer soundPlayer = new SoundPlayer();

    // ---------------------------------------------------------------
    public World() {
        Random rng = new Random();
        floors  = new Floor[NUM_FLOORS];
        ladders = new Point[NUM_FLOORS - 1];
        seenUpLadders = new boolean[NUM_FLOORS];
        seenExits = new boolean[NUM_FLOORS];

        // ── Phase 1: generate all floor mazes ──────────────────────
        for (int i = 0; i < NUM_FLOORS; i++) {
            floors[i] = new Floor(MazeGenerator.generate(rng), i);
        }

        // ── Start position: floor 0, row 23, random odd col ─────────
        startCol = ODD[rng.nextInt(ODD.length)];
        floors[0].getMaze().setCell(ROWS, startCol, Cell.PASSAGE);

        // ── Exit position: top floor, row 0 (north wall), random odd col
        // Guarantee cell (1, exitCol) on top floor is a passage so the
        // player can approach the door.
        exitCol = ODD[rng.nextInt(ODD.length)];
        floors[NUM_FLOORS-1].getMaze().setCell(1, exitCol, Cell.PASSAGE);
        floors[NUM_FLOORS-1].placeItem(0, exitCol, ItemType.EXIT);

        // ── Phase 2: place ladders ───────────────────────────────────
        for (int i = 0; i < NUM_FLOORS - 1; i++) {
            Set<Point> exclude = new HashSet<>();
            exclude.add(new Point(startCol, ROWS)); // avoid start cell
            ladders[i] = pickEmptyRoomCell(rng, floors[i], exclude);
            floors[i].placeItem(ladders[i].y, ladders[i].x, ItemType.LADDER_UP);
            floors[i+1].placeItem(ladders[i].y, ladders[i].x, ItemType.HOLE_DOWN);
        }

        // ── Phase 3: place coins per floor ──────────────────────────
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
            totalCoinsPlaced += placed;
        }

        // ── Phase 4: place key on the first floor (avoid start/ladders/coins)
        List<Point> keyCells = shuffledRoomCells(rng, floors[0].getMaze());
        for (Point p : keyCells) {
            if (isReserved(p, 0)) continue;
            if (floors[0].hasItem(p.y, p.x)) continue;
            floors[0].placeItem(p.y, p.x, ItemType.KEY);
            break;
        }

        // ── Phase 5: place one map on each floor
        for (int f = 0; f < NUM_FLOORS; f++) {
            List<Point> mapCells = shuffledRoomCells(rng, floors[f].getMaze());
            for (Point p : mapCells) {
                if (isReserved(p, f)) continue;
                if (floors[f].hasItem(p.y, p.x)) continue;
                floors[f].placeItem(p.y, p.x, ItemType.MAP);
                break;
            }
        }

        // ── Create player ────────────────────────────────────────────
        player    = new Player(startCol + 0.5f, ROWS + 0.5f, 0, NUM_FLOORS);
        gameState = GameState.PLAYING;
    }

    // ── helpers ─────────────────────────────────────────────────────

    /** Returns true for cells that should never hold a pickup item. */
    private boolean isReserved(Point p, int floor) {
        if (floor < NUM_FLOORS - 1 && p.equals(ladders[floor])) return true;
        if (floor == 0 && p.x == startCol && p.y == ROWS) return true;
        if (floor == NUM_FLOORS - 1 && p.x == exitCol  && p.y == 1)  return true;
        return false;
    }

    private Point pickRoomCell(Random rng, Maze maze, Set<Point> excluded) {
        List<Point> cells = shuffledRoomCells(rng, maze);
        for (Point p : cells) {
            if (!excluded.contains(p)) return p;
        }
        return cells.get(0); // should never be needed
    }

    private Point pickEmptyRoomCell(Random rng, Floor floor, Set<Point> excluded) {
        List<Point> cells = shuffledRoomCells(rng, floor.getMaze());
        for (Point p : cells) {
            if (excluded.contains(p)) continue;
            if (floor.hasItem(p.y, p.x)) continue;
            return p;
        }
        return pickRoomCell(rng, floor.getMaze(), excluded);
    }

    private List<Point> shuffledRoomCells(Random rng, Maze maze) {
        List<Point> list = new ArrayList<>();
        for (int r = 1; r <= ROWS; r += 2) {
            for (int c = 1; c <= COLS; c += 2) {
                if (maze.isPassage(r, c)) {
                    list.add(new Point(c, r)); // x=col, y=row
                }
            }
        }
        Collections.shuffle(list, rng);
        return list;
    }

    // ── public accessors ────────────────────────────────────────────

    public Floor       getFloor(int index)  { return floors[index]; }
    public Floor       getCurrentFloor()    { return floors[player.floor]; }
    public int         getNumFloors()       { return NUM_FLOORS; }
    public Player      getPlayer()          { return player; }
    public GameState   getGameState()       { return gameState; }
    public int         getExitCol()         { return exitCol; }
    public int         getStartCol()        { return startCol; }
    public int         getScore()           { return score; }
    public int         getExitBonus()       { return exitBonus; }
    public int         getAllCoinsBonus()   { return totalCoinsPlaced; }
    public boolean     hasAllCoinsBonusAwarded() { return allCoinsBonusApplied; }
    public boolean     hasExitBonusCalculated() { return exitBonusApplied; }
    public boolean     hasMapForCurrentFloor() { return player.hasMapForFloor(player.floor); }
    public boolean     hasSeenUpLadder(int floorIndex) {
        return floorIndex >= 0
                && floorIndex < seenUpLadders.length
                && seenUpLadders[floorIndex];
    }
    public boolean     hasSeenExit(int floorIndex) {
        return floorIndex >= 0
                && floorIndex < seenExits.length
                && seenExits[floorIndex];
    }

    public void markCurrentFloorUpLadderSeen() {
        if (player.floor >= 0 && player.floor < seenUpLadders.length) {
            seenUpLadders[player.floor] = true;
        }
    }

    public void markCurrentFloorExitSeen() {
        if (player.floor >= 0 && player.floor < seenExits.length) {
            seenExits[player.floor] = true;
        }
    }

    public boolean isLockedMessageVisible() { return lockedMessageTicks > 0; }
    public void    tickLockedMessage()      { if (lockedMessageTicks > 0) lockedMessageTicks--; }

    public void applyExitTimeBonus(long elapsedMs) {
        if (exitBonusApplied) return;

        long elapsedSeconds = Math.max(0L, elapsedMs / 1000L);
        long bonusCapSeconds = (long) NUM_FLOORS * BONUS_SECONDS_PER_FLOOR;
        long bonus = bonusCapSeconds - elapsedSeconds;

        if (bonus > Integer.MAX_VALUE) {
            exitBonus = Integer.MAX_VALUE;
        } else if (bonus < Integer.MIN_VALUE) {
            exitBonus = Integer.MIN_VALUE;
        } else {
            exitBonus = (int) bonus;
        }
        score += exitBonus;
        exitBonusApplied = true;
    }

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
        if (player.floor == (NUM_FLOORS - 1) && newRow == 0 && newCol == exitCol) {
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

    /** Collect pickup items at the player's current grid cell. */
    public void collectItems() {
        if (gameState != GameState.PLAYING) return;
        int      row  = player.getGridRow();
        int      col  = player.getGridCol();
        Floor    f    = getCurrentFloor();
        ItemType item = f.getItemAt(row, col);
        if (item == ItemType.COIN) {
            player.coins++;
            score += COIN_SCORE;
            if (!allCoinsBonusApplied && totalCoinsPlaced > 0 && player.coins >= totalCoinsPlaced) {
                score += totalCoinsPlaced;
                allCoinsBonusApplied = true;
            }
            f.removeItem(row, col);
            soundPlayer.playCoinClink();
        } else if (item == ItemType.MAP) {
            player.discoverMapForFloor(player.floor);
            score += MAP_SCORE;
            f.removeItem(row, col);
            soundPlayer.playMapPickup();
        } else if (item == ItemType.KEY) {
            player.hasKey = true;
            score += KEY_SCORE;
            f.removeItem(row, col);
            soundPlayer.playKeyPickup();
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
            soundPlayer.playLadderUp();
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
            soundPlayer.playLadderDown();
            player.px = col + 0.5f;
            player.py = row + 0.5f;
            return true;
        }
        return false;
    }
}
