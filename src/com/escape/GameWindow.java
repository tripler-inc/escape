package com.escape;

import javax.swing.JFrame;

public class GameWindow extends JFrame {

    public GameWindow() {
        super("Maze Escape");

        GamePanel panel = new GamePanel();
        add(panel);
        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        panel.requestFocusInWindow();
    }
}
