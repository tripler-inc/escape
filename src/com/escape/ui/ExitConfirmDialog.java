package com.escape.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

public class ExitConfirmDialog {

    /** Shows a modal "Quit game?" dialog. Returns true if the player chose Yes. */
    public static boolean confirm(Component parent) {
        int result = JOptionPane.showConfirmDialog(
                parent,
                "Quit the game?",
                "Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
