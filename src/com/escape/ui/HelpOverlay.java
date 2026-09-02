package com.escape.ui;

import com.escape.Config;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Draws a full-screen translucent help panel loaded from an editable text file.
 */
public class HelpOverlay {

    private static final Path HELP_FILE = Paths.get("help.txt");

    private static final Color DIM_BG = new Color(0, 0, 0, 180);
    private static final Color PANEL_BG = new Color(15, 18, 26, 225);
    private static final Color PANEL_BORDER = new Color(220, 220, 220, 210);
    private static final Color TEXT_COLOR = new Color(245, 245, 245);
    private static final Font TEXT_FONT = new Font("Monospaced", Font.PLAIN, 18);

    private static final int OUTER_MARGIN = 28;
    private static final int PANEL_PADDING_X = 20;
    private static final int PANEL_PADDING_Y = 20;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b");
    private static final Map<String, String> PLACEHOLDER_VALUES = buildPlaceholderValues();

    private List<String> lines;

    public HelpOverlay() {
        lines = loadLines();
    }

    public void reload() {
        lines = loadLines();
    }

    public void draw(Graphics2D g, int width, int height) {
        g.setColor(DIM_BG);
        g.fillRect(0, 0, width, height);

        g.setFont(TEXT_FONT);
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();

        int maxTextWidth = 0;
        for (String line : lines) {
            maxTextWidth = Math.max(maxTextWidth, fm.stringWidth(line));
        }

        int panelW = Math.min(width - OUTER_MARGIN * 2, maxTextWidth + PANEL_PADDING_X * 2);
        int maxLines = Math.max(1, (height - OUTER_MARGIN * 2 - PANEL_PADDING_Y * 2) / lineH);
        int visibleLines = Math.min(maxLines, Math.max(1, lines.size()));
        int panelH = visibleLines * lineH + PANEL_PADDING_Y * 2;

        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        g.setColor(PANEL_BG);
        g.fillRoundRect(panelX, panelY, panelW, panelH, 16, 16);
        g.setColor(PANEL_BORDER);
        g.drawRoundRect(panelX, panelY, panelW, panelH, 16, 16);

        g.setColor(TEXT_COLOR);
        int textX = panelX + PANEL_PADDING_X;
        int textY = panelY + PANEL_PADDING_Y + fm.getAscent();
        for (int i = 0; i < visibleLines; i++) {
            g.drawString(lines.get(i), textX, textY);
            textY += lineH;
        }
    }

    private static List<String> loadLines() {
        if (Files.exists(HELP_FILE)) {
            try {
                List<String> fileLines = Files.readAllLines(HELP_FILE, StandardCharsets.UTF_8);
                if (!fileLines.isEmpty()) {
                    return substitutePlaceholders(fileLines);
                }
            } catch (IOException ex) {
                return substitutePlaceholders(Arrays.asList(
                        "HELP FILE ERROR",
                        "Could not read help.txt",
                        ex.getMessage() == null ? "" : ex.getMessage()));
            }
        }

        List<String> fallback = new ArrayList<>();
        fallback.add("ESCAPE - HELP");
        fallback.add("");
        fallback.add("Create a help.txt file in the game folder to edit this screen.");
        fallback.add("Press Esc to close help.");
        return substitutePlaceholders(fallback);
    }

    private static List<String> substitutePlaceholders(List<String> sourceLines) {
        List<String> resolved = new ArrayList<>(sourceLines.size());
        for (String line : sourceLines) {
            resolved.add(substitutePlaceholders(line));
        }
        return resolved;
    }

    private static String substitutePlaceholders(String line) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(line);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            String replacement = PLACEHOLDER_VALUES.get(token);
            if (replacement == null) {
                replacement = token;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static Map<String, String> buildPlaceholderValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("NUM_FLOORS", Integer.toString(Config.WORLD.numFloors));
        values.put("COINS_PER_FLOOR", Integer.toString(Config.WORLD.coinsPerFloor));
        values.put("ROWS", Integer.toString(Config.WORLD.rows));
        values.put("COLS", Integer.toString(Config.WORLD.cols));
        values.put("LOCKED_MSG_DURATION", Integer.toString(Config.WORLD.lockedMsgDuration));
        values.put("SCORE_COIN", Integer.toString(Config.WORLD.scoreCoin));
        values.put("SCORE_KEY", Integer.toString(Config.WORLD.scoreKey));
        values.put("SCORE_MAP", Integer.toString(Config.WORLD.scoreMap));
        values.put("BONUS_SECONDS_PER_FLOOR", Integer.toString(Config.WORLD.bonusSecondsPerFloor));
        // Compatibility alias for typo variants used in help text.
        values.put("BINUS_SECONDS_PER_FLOOR", Integer.toString(Config.WORLD.bonusSecondsPerFloor));
        return Collections.unmodifiableMap(values);
    }
}
