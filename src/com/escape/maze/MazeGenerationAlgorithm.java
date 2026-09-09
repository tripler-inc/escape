package com.escape.maze;

import java.util.Random;

public interface MazeGenerationAlgorithm {
    Maze generate(Random rng);
}