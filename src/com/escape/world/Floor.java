package com.escape.world;

import com.escape.maze.Maze;

import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Floor {

    private final Maze maze;
    private final int floorIndex;
    private final Map<Point, ItemType> items;

    public Floor(Maze maze, int floorIndex) {
        this.maze       = maze;
        this.floorIndex = floorIndex;
        this.items      = new HashMap<>();
    }

    public Maze getMaze() {
        return maze;
    }

    public int getFloorIndex() {
        return floorIndex;
    }

    /**
     * Places an item at (row, col). Overwrites any existing item at that cell.
     */
    public void placeItem(int row, int col, ItemType type) {
        items.put(new Point(col, row), type);  // Point(x=col, y=row)
    }

    public void removeItem(int row, int col) {
        items.remove(new Point(col, row));
    }

    public ItemType getItemAt(int row, int col) {
        ItemType item = items.get(new Point(col, row));
        return (item == null) ? ItemType.NONE : item;
    }

    public boolean hasItem(int row, int col) {
        return items.containsKey(new Point(col, row));
    }

    /** Read-only view of all items on this floor. */
    public Map<Point, ItemType> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
