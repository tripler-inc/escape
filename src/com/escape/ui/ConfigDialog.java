package com.escape.ui;

import com.escape.Config;
import com.escape.ApplicationRelauncher;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigDialog extends JDialog {

    private final Map<String, Map<String, Object>> editors = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> originalValues;
    private final Path savePath;

    private ConfigDialog(Window owner,
                         Map<String, Map<String, String>> configData,
                         Path savePath) {
        super(owner, "Configuration", ModalityType.APPLICATION_MODAL);
        this.originalValues = configData;
        this.savePath = savePath;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        add(buildTabs(configData), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        int maxHeight = resolveMaxDialogHeight(owner);
        int preferredHeight = Math.min(680, maxHeight);
        setMinimumSize(new java.awt.Dimension(920, Math.min(680, maxHeight)));
        setPreferredSize(new java.awt.Dimension(920, preferredHeight));
        pack();
        if (getHeight() > maxHeight) {
            setSize(getWidth(), maxHeight);
        }
        setLocationRelativeTo(owner);
    }

    private static int resolveMaxDialogHeight(Window owner) {
        int ownerHeight = owner == null ? 0 : owner.getHeight();
        if (ownerHeight <= 0) {
            ownerHeight = Config.GAME_PANEL.height;
        }
        return Math.max(0, ownerHeight - 24);
    }

    public static void showDialog(Component parent) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Map<String, Map<String, String>> configData;
        try {
            configData = Config.loadCurrentData();
        } catch (RuntimeException ex) {
            configData = Config.snapshotData();
        }
        ConfigDialog dialog = new ConfigDialog(owner, configData, Config.externalConfigPath());
        dialog.setVisible(true);
    }

    private JTabbedPane buildTabs(Map<String, Map<String, String>> configData) {
        JTabbedPane tabs = new JTabbedPane();
        for (Map.Entry<String, Map<String, String>> sectionEntry : configData.entrySet()) {
            tabs.addTab(sectionEntry.getKey(), wrapSection(sectionEntry.getKey(), sectionEntry.getValue()));
        }
        return tabs;
    }

    private JScrollPane wrapSection(String sectionName, Map<String, String> values) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 0, 6, 12);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);

        Map<String, Object> sectionEditors = new LinkedHashMap<>();
        int row = 0;
        for (Map.Entry<String, String> valueEntry : values.entrySet()) {
            labelConstraints.gridy = row;
            fieldConstraints.gridy = row;

            JLabel label = new JLabel(valueEntry.getKey());
            Object editor;
            if (isMazeAlgorithmField(sectionName, valueEntry.getKey())) {
                JComboBox<String> combo = new JComboBox<>(new String[] {"DFS", "PRIM", "RANDOM", "WILSON"});
                combo.setEditable(false);
                combo.setSelectedItem(normalizeAlgorithmValue(valueEntry.getValue()));
                combo.setToolTipText(valueEntry.getValue());
                editor = combo;
            } else {
                JTextField field = new JTextField(valueEntry.getValue(), 36);
                field.setToolTipText(valueEntry.getValue());
                editor = field;
            }

            panel.add(label, labelConstraints);
            panel.add((java.awt.Component) editor, fieldConstraints);
            sectionEditors.put(valueEntry.getKey(), editor);
            row++;
        }

        editors.put(sectionName, sectionEditors);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBorder(new EmptyBorder(0, 16, 16, 16));

        JButton save = new JButton("Save");
        save.addActionListener(e -> saveAndClose());

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> resetToDefaults());

        JButton close = new JButton("Close");
        close.addActionListener(e -> closeWithConfirmation());

        buttons.add(save);
        buttons.add(reset);
        buttons.add(close);
        getRootPane().setDefaultButton(save);
        return buttons;
    }

    private void resetToDefaults() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reset all configuration to default values?",
                "Reset Configuration",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (savePath != null) {
                Files.deleteIfExists(savePath);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not delete external config.ini:\n" + ex.getMessage(),
                    "Reset Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Window owner = getOwner();
        dispose();
        ConfigDialog.showDialog(owner);
    }

    private void saveAndClose() {
        try {
            writeConfigFile();
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Load the changed configuration and restart the game now?",
                    "Restart Game",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                if (ApplicationRelauncher.restart(this)) {
                    return;
                }
                return;
            }
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save config.ini:\n" + ex.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeWithConfirmation() {
        if (!isDirty()) {
            dispose();
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Save changes before closing?",
                "Save Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            saveAndClose();
        } else if (choice == JOptionPane.NO_OPTION) {
            dispose();
        }
    }

    private boolean isDirty() {
        for (Map.Entry<String, Map<String, Object>> sectionEntry : editors.entrySet()) {
            Map<String, String> originalSection = originalValues.get(sectionEntry.getKey());
            for (Map.Entry<String, Object> fieldEntry : sectionEntry.getValue().entrySet()) {
                String originalValue = originalSection == null ? null : originalSection.get(fieldEntry.getKey());
                String currentValue = getEditorValue(fieldEntry.getValue());
                if (originalValue == null ? currentValue != null && !currentValue.isEmpty() : !originalValue.equals(currentValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeConfigFile() throws IOException {
        Path target = savePath;
        if (target == null) {
            target = Path.of("config.ini");
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            boolean firstSection = true;
            for (Map.Entry<String, Map<String, Object>> sectionEntry : editors.entrySet()) {
                if (!firstSection) {
                    writer.newLine();
                }
                firstSection = false;
                writer.write("[" + sectionEntry.getKey() + "]");
                writer.newLine();
                for (Map.Entry<String, Object> fieldEntry : sectionEntry.getValue().entrySet()) {
                    writer.write(fieldEntry.getKey());
                    writer.write(" = ");
                    writer.write(quoteIniValue(getEditorValue(fieldEntry.getValue())));
                    writer.newLine();
                }
            }
        }
    }

    private static boolean isMazeAlgorithmField(String sectionName, String keyName) {
        return "MazeGeneration".equals(sectionName) && "ALGORITHM".equals(keyName);
    }

    private static String normalizeAlgorithmValue(String value) {
        if (value == null) {
            return "DFS";
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "DFS":
            case "PRIM":
            case "RANDOM":
            case "WILSON":
                return normalized;
            default:
                return "DFS";
        }
    }

    private static String getEditorValue(Object editor) {
        if (editor instanceof JTextField) {
            return ((JTextField) editor).getText();
        }
        if (editor instanceof JComboBox) {
            Object selected = ((JComboBox<?>) editor).getSelectedItem();
            return selected == null ? "" : selected.toString();
        }
        return "";
    }

    private static String quoteIniValue(String raw) {
        String value = raw == null ? "" : raw;
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '"') {
                out.append('\\');
            }
            out.append(ch);
        }
        out.append('"');
        return out.toString();
    }
}