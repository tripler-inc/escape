package com.escape.maze;

import com.escape.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class MazeGenerationSupport {

    static final int[][] ROOM_STEPS = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

    private MazeGenerationSupport() {}

    static int[] randomRoomCell(Random rng) {
        int[] oddVals = Config.WORLD.odd;
        return new int[]{oddVals[rng.nextInt(oddVals.length)], oddVals[rng.nextInt(oddVals.length)]};
    }

    static List<int[]> roomNeighbours(int row, int col) {
        List<int[]> neighbours = new ArrayList<>();
        for (int[] step : ROOM_STEPS) {
            int nextRow = row + step[0];
            int nextCol = col + step[1];
            if (nextRow >= 1 && nextRow <= Maze.SIZE - 2 && nextCol >= 1 && nextCol <= Maze.SIZE - 2) {
                neighbours.add(new int[]{nextRow, nextCol});
            }
        }
        return neighbours;
    }

    static List<int[]> unvisitedRoomNeighbours(int row, int col, boolean[][] visited) {
        List<int[]> neighbours = new ArrayList<>();
        for (int[] step : ROOM_STEPS) {
            int nextRow = row + step[0];
            int nextCol = col + step[1];
            if (nextRow >= 1 && nextRow <= Maze.SIZE - 2 && nextCol >= 1 && nextCol <= Maze.SIZE - 2
                    && !visited[nextRow][nextCol]) {
                neighbours.add(new int[]{nextRow, nextCol});
            }
        }
        return neighbours;
    }

    static void carveBetween(Maze maze, int[] from, int[] to) {
        maze.setCell((from[0] + to[0]) / 2, (from[1] + to[1]) / 2, Cell.PASSAGE);
        maze.setCell(to[0], to[1], Cell.PASSAGE);
    }

    static List<int[]> allRoomCells() {
        List<int[]> cells = new ArrayList<>();
        for (int row = 1; row <= Maze.SIZE - 2; row += 2) {
            for (int col = 1; col <= Maze.SIZE - 2; col += 2) {
                cells.add(new int[]{row, col});
            }
        }
        return cells;
    }

    static int key(int row, int col) {
        return (row << 16) ^ col;
    }
}