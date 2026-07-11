package com.escape.player;

public class Player {

    public float px;      // world-space x = column
    public float py;      // world-space y = row  (row 0 = north)
    public int   floor;
    public boolean hasKey;
    public int   coins;
    public float angle;   // radians; 0 = east, -PI/2 = north

    public Player(float px, float py, int floor) {
        this.px    = px;
        this.py    = py;
        this.floor = floor;
        this.hasKey = false;
        this.coins  = 0;
        this.angle  = -(float) (Math.PI / 2.0); // face north at start
    }

    public int getGridRow() {
        return (int) py;
    }

    public int getGridCol() {
        return (int) px;
    }
}
