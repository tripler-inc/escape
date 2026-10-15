package com.escape.maze;

import com.escape.Config;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class OpenMazeGenerator implements MazeGenerationAlgorithm {

    @Override
    public Maze generate(Random rng) {
        Maze maze = new Maze();
        openInterior(maze);

        int side = Maze.SIZE - 2;
        int interiorCells = side * side;
        int targetWalls = Math.round(interiorCells * (Config.MAZE_GENERATION.openWallPercent / 100.0f));

        List<int[]> candidates = new ArrayList<>(interiorCells);
        for (int row = 1; row <= Maze.SIZE - 2; row++) {
            for (int col = 1; col <= Maze.SIZE - 2; col++) {
                candidates.add(new int[]{row, col});
            }
        }
        Collections.shuffle(candidates, rng);

        int wallsPlaced = 0;
        for (int[] cell : candidates) {
            if (wallsPlaced >= targetWalls) {
                break;
            }
            if (tryPlaceWallWithoutDisconnecting(maze, cell[0], cell[1])) {
                wallsPlaced++;
            }
        }

        return maze;
    }

    private static void openInterior(Maze maze) {
        for (int row = 1; row <= Maze.SIZE - 2; row++) {
            for (int col = 1; col <= Maze.SIZE - 2; col++) {
                maze.setCell(row, col, Cell.PASSAGE);
            }
        }
    }

    private static boolean tryPlaceWallWithoutDisconnecting(Maze maze, int row, int col) {
        if (!maze.isPassage(row, col)) {
            return false;
        }

        maze.setCell(row, col, Cell.WALL);
        if (allPassagesConnected(maze)) {
            return true;
        }

        maze.setCell(row, col, Cell.PASSAGE);
        return false;
    }

    private static boolean allPassagesConnected(Maze maze) {
        int totalPassages = 0;
        int startRow = -1;
        int startCol = -1;

        for (int row = 1; row <= Maze.SIZE - 2; row++) {
            for (int col = 1; col <= Maze.SIZE - 2; col++) {
                if (maze.isPassage(row, col)) {
                    totalPassages++;
                    if (startRow < 0) {
                        startRow = row;
                        startCol = col;
                    }
                }
            }
        }

        if (totalPassages <= 1) {
            return totalPassages == 1;
        }

        boolean[][] seen = new boolean[Maze.SIZE][Maze.SIZE];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        seen[startRow][startCol] = true;

        int connected = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.remove();
            connected++;

            int row = cur[0];
            int col = cur[1];
            visitNeighbour(maze, seen, queue, row - 1, col);
            visitNeighbour(maze, seen, queue, row + 1, col);
            visitNeighbour(maze, seen, queue, row, col - 1);
            visitNeighbour(maze, seen, queue, row, col + 1);
        }

        return connected == totalPassages;
    }

    private static void visitNeighbour(Maze maze, boolean[][] seen, Queue<int[]> queue, int row, int col) {
        if (row < 1 || row > Maze.SIZE - 2 || col < 1 || col > Maze.SIZE - 2) {
            return;
        }
        if (!maze.isPassage(row, col) || seen[row][col]) {
            return;
        }

        seen[row][col] = true;
        queue.add(new int[]{row, col});
    }
}
