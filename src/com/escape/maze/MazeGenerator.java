package com.escape.maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {

    /**
     * Generates a perfect maze in a 26x26 grid using depth-first search
     * with stack-based backtracking.
     *
     * Layout:
     *   - Outer boundary (row 0, row 25, col 0, col 25) remains WALL.
     *   - Room cells are at (r,c) where r,c are both odd in [1..23] → 12x12 = 144 rooms.
     *   - DFS carves the intermediate wall cell between two adjacent rooms.
     */
    public static Maze generate(Random rng) {
        Maze maze = new Maze(); // all WALL initially

        boolean[][] visited = new boolean[Maze.SIZE][Maze.SIZE];

        // Pick a random starting room cell
        int[] oddVals = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23};
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
                if (nr >= 1 && nr <= 23 && nc >= 1 && nc <= 23 && !visited[nr][nc]) {
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

        return maze;
    }
}
