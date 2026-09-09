package com.escape.maze;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class DepthFirstSearchMazeGenerator implements MazeGenerationAlgorithm {

    @Override
    public Maze generate(Random rng) {
        Maze maze = new Maze();
        boolean[][] visited = new boolean[Maze.SIZE][Maze.SIZE];

        int[] start = MazeGenerationSupport.randomRoomCell(rng);
        maze.setCell(start[0], start[1], Cell.PASSAGE);
        visited[start[0]][start[1]] = true;

        Stack<int[]> stack = new Stack<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            List<int[]> neighbours = MazeGenerationSupport.unvisitedRoomNeighbours(cur[0], cur[1], visited);

            if (!neighbours.isEmpty()) {
                Collections.shuffle(neighbours, rng);
                int[] next = neighbours.get(0);
                MazeGenerationSupport.carveBetween(maze, cur, next);
                visited[next[0]][next[1]] = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        return maze;
    }
}