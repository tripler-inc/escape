package com.escape.maze;

import com.escape.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MazeGenerator {
    private static final MazeGenerationAlgorithm[] ALGORITHMS = {
            new DepthFirstSearchMazeGenerator(),
            new RandomizedPrimMazeGenerator(),
            new WilsonMazeGenerator()
    };

    private MazeGenerator() {}

    public static Maze generate(Random rng) {
        Maze maze = selectAlgorithm(rng).generate(rng);
        addExtraOpenings(maze, rng, Config.MAZE_GENERATION.extraOpenings);
        return maze;
    }

    private static MazeGenerationAlgorithm selectAlgorithm(Random rng) {
        return switch (Config.MAZE_GENERATION.algorithm) {
            case DFS -> ALGORITHMS[0];
            case PRIM -> ALGORITHMS[1];
            case WILSON -> ALGORITHMS[2];
            case RANDOM -> ALGORITHMS[rng.nextInt(ALGORITHMS.length)];
        };
    }

    private static MazeGenerationAlgorithm createAlgorithm(Config.MazeAlgorithm algorithm) {
        return switch (algorithm) {
            case DFS -> new DepthFirstSearchMazeGenerator();
            case PRIM -> new RandomizedPrimMazeGenerator();
            case RANDOM -> throw new IllegalStateException("RANDOM is handled per-floor by selectAlgorithm");
            case WILSON -> new WilsonMazeGenerator();
        };
    }

    /** Chebyshev distance: openings must not sit in the same neighborhood. */
    private static final int MIN_LOOP_SEPARATION = 6;

    /**
     * Knocks out wall segments so two corridors meet after the configured
     * maze algorithm has finished carving the base maze.
     */
    private static void addExtraOpenings(Maze maze, Random rng, int extraOpenings) {
        if (extraOpenings <= 0) {
            return;
        }

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
            if (chosen.size() >= extraOpenings) break;
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
