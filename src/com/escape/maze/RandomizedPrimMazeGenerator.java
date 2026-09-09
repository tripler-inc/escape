package com.escape.maze;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomizedPrimMazeGenerator implements MazeGenerationAlgorithm {

    @Override
    public Maze generate(Random rng) {
        Maze maze = new Maze();
        boolean[][] visited = new boolean[Maze.SIZE][Maze.SIZE];

        int[] start = MazeGenerationSupport.randomRoomCell(rng);
        maze.setCell(start[0], start[1], Cell.PASSAGE);
        visited[start[0]][start[1]] = true;

        List<int[]> frontier = new ArrayList<>();
        addFrontierEdges(frontier, visited, start);

        while (!frontier.isEmpty()) {
            int index = rng.nextInt(frontier.size());
            int[] edge = frontier.remove(index);
            int fromRow = edge[0];
            int fromCol = edge[1];
            int toRow = edge[2];
            int toCol = edge[3];

            if (visited[toRow][toCol]) {
                continue;
            }

            MazeGenerationSupport.carveBetween(maze, new int[]{fromRow, fromCol}, new int[]{toRow, toCol});
            visited[toRow][toCol] = true;
            addFrontierEdges(frontier, visited, new int[]{toRow, toCol});
        }

        return maze;
    }

    private static void addFrontierEdges(List<int[]> frontier, boolean[][] visited, int[] from) {
        for (int[] neighbour : MazeGenerationSupport.roomNeighbours(from[0], from[1])) {
            if (!visited[neighbour[0]][neighbour[1]]) {
                frontier.add(new int[]{from[0], from[1], neighbour[0], neighbour[1]});
            }
        }
    }
}