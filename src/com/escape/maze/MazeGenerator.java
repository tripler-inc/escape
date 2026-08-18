package com.escape.maze;

import com.escape.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {

    /**
     * Generates a perfect maze in a 27x27 grid using depth-first search
     * with stack-based backtracking.
     *
     * Layout:
     *   - Outer boundary (row 0, row 26, col 0, col 26) remains WALL.
     *   - Room cells are at (r,c) where r,c are both odd in [1..25] → 13x13 = 169 rooms.
     *   - DFS carves the intermediate wall cell between two adjacent rooms.
     */
    public static Maze generate(Random rng) {
        Maze maze = new Maze(); // all WALL initially

        boolean[][] visited = new boolean[Maze.SIZE][Maze.SIZE];

        // Pick a random starting room cell
        int[] oddVals = Config.WORLD.odd;
        int startR = oddVals[rng.nextInt(oddVals.length)];
        int startC = oddVals[rng.nextInt(oddVals.length)];

        maze.setCell(startR, startC, Cell.PASSAGE);
        visited[startR][startC] = true;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startR, startC});

        // Directions: N, S, W, E (each step of 2)
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int r = cur[0];
            int c = cur[1];

            // Collect unvisited neighbours 2 steps away
            List<int[]> neighbours = new ArrayList<>();
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 1 && nr <= Maze.SIZE-2 && nc >= 1 && nc <= Maze.SIZE-2 && !visited[nr][nc]) {
                    neighbours.add(new int[]{nr, nc});
                }
            }

            if (!neighbours.isEmpty()) {
                Collections.shuffle(neighbours, rng);
                int[] next = neighbours.get(0);
                int nr = next[0];
                int nc = next[1];
                // Carve the wall cell between cur and next
                maze.setCell((r + nr) / 2, (c + nc) / 2, Cell.PASSAGE);
                // Open the next room cell
                maze.setCell(nr, nc, Cell.PASSAGE);
                visited[nr][nc] = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        knockOutLoops(maze, rng);
        return maze;
    }

    /** Extra openings per floor that join two corridors. */
    private static final int LOOPS_PER_FLOOR = 2;
    /** Chebyshev distance: openings must not sit in the same neighborhood. */
    private static final int MIN_LOOP_SEPARATION = 6;

    /**
     * Knocks out wall segments so two corridors meet.  Candidates are the
     * uncarved door cells between rooms: passages on opposite sides, walls
     * on the other axis.  Openings are rejected if they sit beside a
     * corridor corner/junction or too close to another opening.
     */
    private static void knockOutLoops(Maze maze, Random rng) {
        List<int[]> candidates = new ArrayList<>();
        for (int r = 1; r < Maze.SIZE - 1; r++) {
            for (int c = 1; c < Maze.SIZE - 1; c++) {
                if (isLoopCandidate(maze, r, c)) {
                    candidates.add(new int[]{r, c});
                }
            }
        }
        Collections.shuffle(candidates, rng);

        List<int[]> chosen = new ArrayList<>();
        for (int[] cell : candidates) {
            if (chosen.size() >= LOOPS_PER_FLOOR) break;
            if (!isLoopCandidate(maze, cell[0], cell[1])) continue;
            if (tooClose(cell, chosen)) continue;
            maze.setCell(cell[0], cell[1], Cell.PASSAGE);
            chosen.add(cell);
        }
    }

    /**
     * A knock-out must join two opposite corridors through a straight wall
     * slab and must not touch a corner or junction.
     */
    private static boolean isLoopCandidate(Maze maze, int r, int c) {
        if (!maze.isWall(r, c)) return false;
        if ((r + c) % 2 == 0) return false; // skip pillars; only door cells

        boolean joinNS = maze.isPassage(r - 1, c) && maze.isPassage(r + 1, c)
                && maze.isWall(r, c - 1) && maze.isWall(r, c + 1);
        boolean joinEW = maze.isPassage(r, c - 1) && maze.isPassage(r, c + 1)
                && maze.isWall(r - 1, c) && maze.isWall(r + 1, c);
        if (joinNS == joinEW) return false;

        if (joinNS) {
            return !isCornerPassage(maze, r - 1, c) && !isCornerPassage(maze, r + 1, c);
        }
        return !isCornerPassage(maze, r, c - 1) && !isCornerPassage(maze, r, c + 1);
    }

    /** True when a passage has both a vertical and a horizontal opening (L, T, or +). */
    private static boolean isCornerPassage(Maze maze, int r, int c) {
        boolean vertical   = maze.isPassage(r - 1, c) || maze.isPassage(r + 1, c);
        boolean horizontal = maze.isPassage(r, c - 1) || maze.isPassage(r, c + 1);
        return vertical && horizontal;
    }

    private static boolean tooClose(int[] cell, List<int[]> chosen) {
        for (int[] other : chosen) {
            int dr = Math.abs(cell[0] - other[0]);
            int dc = Math.abs(cell[1] - other[1]);
            if (Math.max(dr, dc) < MIN_LOOP_SEPARATION) return true;
        }
        return false;
    }
}
