package com.escape.maze;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WilsonMazeGenerator implements MazeGenerationAlgorithm {

    @Override
    public Maze generate(Random rng) {
        Maze maze = new Maze();
        boolean[][] inTree = new boolean[Maze.SIZE][Maze.SIZE];

        int[] root = MazeGenerationSupport.randomRoomCell(rng);
        maze.setCell(root[0], root[1], Cell.PASSAGE);
        inTree[root[0]][root[1]] = true;

        List<int[]> rooms = MazeGenerationSupport.allRoomCells();
        int remaining = rooms.size() - 1;

        while (remaining > 0) {
            int[] start = pickUnvisitedRoom(rng, rooms, inTree);
            List<int[]> path = new ArrayList<>();
            Map<Integer, Integer> pathIndex = new HashMap<>();

            path.add(start);
            pathIndex.put(MazeGenerationSupport.key(start[0], start[1]), 0);

            int[] current = start;
            while (!inTree[current[0]][current[1]]) {
                List<int[]> neighbours = MazeGenerationSupport.roomNeighbours(current[0], current[1]);
                int[] next = neighbours.get(rng.nextInt(neighbours.size()));
                Integer loopIndex = pathIndex.get(MazeGenerationSupport.key(next[0], next[1]));

                if (loopIndex != null) {
                    for (int i = path.size() - 1; i > loopIndex; i--) {
                        int[] removed = path.remove(i);
                        pathIndex.remove(MazeGenerationSupport.key(removed[0], removed[1]));
                    }
                    current = path.get(path.size() - 1);
                    continue;
                }

                path.add(next);
                pathIndex.put(MazeGenerationSupport.key(next[0], next[1]), path.size() - 1);
                current = next;
            }

            for (int i = 0; i < path.size() - 1; i++) {
                MazeGenerationSupport.carveBetween(maze, path.get(i), path.get(i + 1));
                if (!inTree[path.get(i)[0]][path.get(i)[1]]) {
                    inTree[path.get(i)[0]][path.get(i)[1]] = true;
                    remaining--;
                }
            }

            int[] tail = path.get(path.size() - 1);
            if (!inTree[tail[0]][tail[1]]) {
                inTree[tail[0]][tail[1]] = true;
                remaining--;
            }
        }

        return maze;
    }

    private static int[] pickUnvisitedRoom(Random rng, List<int[]> rooms, boolean[][] inTree) {
        List<int[]> candidates = new ArrayList<>();
        for (int[] room : rooms) {
            if (!inTree[room[0]][room[1]]) {
                candidates.add(room);
            }
        }
        return candidates.get(rng.nextInt(candidates.size()));
    }
}