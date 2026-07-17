package com.escape.maze;

public class Maze {

    public static final int SIZE = 27;

    private final Cell[][] grid;

    public Maze() {
        grid = new Cell[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = Cell.WALL;
            }
        }
    }

    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    public void setCell(int row, int col, Cell cell) {
        grid[row][col] = cell;
    }

    public boolean isPassage(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return false;
        return grid[row][col] == Cell.PASSAGE;
    }

    public boolean isWall(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return true;
        return grid[row][col] == Cell.WALL;
    }
}
