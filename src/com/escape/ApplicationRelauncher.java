package com.escape;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class ApplicationRelauncher {

    private ApplicationRelauncher() {}

    public static boolean restart(Component parent) {
        try {
            launchNewProcess();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not restart the game:\n" + ex.getMessage(),
                    "Restart Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
        return true;
    }

    private static void launchNewProcess() throws IOException {
        String javaHome = System.getProperty("java.home");
        String executableName = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        Path javaExecutable = Path.of(javaHome, "bin", executableName);

        String classPath = System.getProperty("java.class.path");
        ProcessBuilder builder = new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                classPath,
                Main.class.getName());
        builder.directory(new File(System.getProperty("user.dir")));
        builder.start();
    }
}