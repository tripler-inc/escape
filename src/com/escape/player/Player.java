package com.escape.player;

public class Player {

    public float px;      // world-space x = column
    public float py;      // world-space y = row  (row 0 = north)
    public int   floor;
    public boolean hasKey;
    public boolean hasCompass;
    public int   coins;
    public float angle;   // radians; 0 = east, -PI/2 = north
    private final boolean[] discoveredMaps;

    public Player(float px, float py, int floor, int numFloors) {
        this.px    = px;
        this.py    = py;
        this.floor = floor;
        this.hasKey = false;
        this.hasCompass = false;
        this.coins  = 0;
        this.angle  = -(float) (Math.PI / 2.0); // face north at start
        this.discoveredMaps = new boolean[Math.max(1, numFloors)];
    }

    public boolean hasMapForFloor(int floorIndex) {
        return floorIndex >= 0
                && floorIndex < discoveredMaps.length
                && discoveredMaps[floorIndex];
    }

    public void discoverMapForFloor(int floorIndex) {
        if (floorIndex >= 0 && floorIndex < discoveredMaps.length) {
            discoveredMaps[floorIndex] = true;
        }
    }

    public int getGridRow() {
        return (int) py;
    }

    public int getGridCol() {
        return (int) px;
    }
}
