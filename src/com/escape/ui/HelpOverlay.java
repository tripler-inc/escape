package com.escape.ui;

import com.escape.Config;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
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

    private static final String HELP_FILE_NAME = "help.txt";
    private static final Path DEV_FALLBACK_HELP = Paths.get(HELP_FILE_NAME);

    private static final Color DIM_BG = Config.HELP.dimBg;
    private static final Color PANEL_BG = Config.HELP.panelBg;
    private static final Color PANEL_BORDER = Config.HELP.panelBorder;
    private static final Color TEXT_COLOR = Config.HELP.textColor;
    private static final Font TEXT_FONT = Config.HELP.textFont;
    private static final String START_PROMPT = Config.HELP.startupPromptText;
    private static final Font START_PROMPT_FONT = Config.HELP.startupPromptFont;
    private static final Color START_PROMPT_BG = Config.HELP.startupPromptBg;
    private static final Color START_PROMPT_BORDER = Config.HELP.startupPromptBorder;
    private static final Color START_PROMPT_TEXT = Config.HELP.startupPromptTextColor;
    private static final int START_PROMPT_MIN_ALPHA = Config.HELP.startupPromptMinAlpha;
    private static final int START_PROMPT_MAX_ALPHA = Config.HELP.startupPromptMaxAlpha;
    private static final int START_PROMPT_PULSE_MS = Math.max(1, Config.HELP.startupPromptPulseMs);

    private static final int OUTER_MARGIN = Config.HELP.outerMargin;
    private static final int PANEL_PADDING_X = Config.HELP.panelPaddingX;
    private static final int PANEL_PADDING_Y = Config.HELP.panelPaddingY;
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

    public void drawStartupPrompt(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(START_PROMPT_FONT);
        FontMetrics fm = g.getFontMetrics();

        long now = System.currentTimeMillis();
        double cycle = (now % START_PROMPT_PULSE_MS) / (double) START_PROMPT_PULSE_MS;
        double wave = 0.5 + 0.5 * Math.sin(cycle * Math.PI * 2.0);
        int minAlpha = Math.max(0, Math.min(255, START_PROMPT_MIN_ALPHA));
        int maxAlpha = Math.max(minAlpha, Math.min(255, START_PROMPT_MAX_ALPHA));
        int alphaRange = maxAlpha - minAlpha;
        int animatedAlpha = minAlpha + (int) Math.round(alphaRange * wave);

        int textW = fm.stringWidth(START_PROMPT);
        int textH = fm.getAscent();
        int padX = Config.HELP.startupPromptPadX;
        int padY = Config.HELP.startupPromptPadY;
        int boxW = textW + padX * 2;
        int boxH = textH + padY * 2;
        int boxX = (width - boxW) / 2;
        int boxY = (height - boxH) / 2;
        int textX = boxX + padX;
        int textY = boxY + padY + fm.getAscent() + Config.HELP.startupPromptBaselineOffset;

        g.setColor(START_PROMPT_BG);
        g.fillRoundRect(boxX, boxY, boxW, boxH, Config.HELP.startupPromptRadius, Config.HELP.startupPromptRadius);
        g.setColor(withAlpha(START_PROMPT_BORDER, Math.min(255, animatedAlpha + 10)));
        g.drawRoundRect(boxX, boxY, boxW, boxH, Config.HELP.startupPromptRadius, Config.HELP.startupPromptRadius);
        g.setColor(withAlpha(START_PROMPT_TEXT, animatedAlpha));
        g.drawString(START_PROMPT, textX, textY);
    }

    private static Color withAlpha(Color base, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamped);
    }

    private static List<String> loadLines() {
        try {
            // Prefer a user-editable file next to the running artifact so the game can be
            // customized without rebuilding the jar. Only fall back to the packaged resource
            // when no external file exists.
            Path externalHelp = resolveExternalHelpPath();
            if (externalHelp != null && Files.isRegularFile(externalHelp)) {
                List<String> fileLines = Files.readAllLines(externalHelp, StandardCharsets.UTF_8);
                if (!fileLines.isEmpty()) {
                    return substitutePlaceholders(fileLines);
                }
            }

            InputStream resource = HelpOverlay.class.getClassLoader().getResourceAsStream(HELP_FILE_NAME);
            if (resource != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                    List<String> resourceLines = reader.lines().toList();
                    if (!resourceLines.isEmpty()) {
                        return substitutePlaceholders(resourceLines);
                    }
                }
            }

            if (Files.isRegularFile(DEV_FALLBACK_HELP)) {
                List<String> devLines = Files.readAllLines(DEV_FALLBACK_HELP, StandardCharsets.UTF_8);
                if (!devLines.isEmpty()) {
                    return substitutePlaceholders(devLines);
                }
            }
        } catch (IOException ex) {
            return substitutePlaceholders(Arrays.asList(
                    "HELP FILE ERROR",
                    "Could not read help.txt",
                    ex.getMessage() == null ? "" : ex.getMessage()));
        }

        List<String> fallback = new ArrayList<>();
        fallback.add("ESCAPE - HELP");
        fallback.add("");
        fallback.add("Create a help.txt file in the game folder to edit this screen.");
        fallback.add("Press Esc to close help.");
        return substitutePlaceholders(fallback);
    }

    private static Path resolveExternalHelpPath() {
        try {
            URL location = HelpOverlay.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            Path codeSource = Paths.get(location.toURI());

            if (Files.isDirectory(codeSource)) {
                String packagePath = HelpOverlay.class.getPackageName().replace('.', java.io.File.separatorChar);
                Path classDirHelp = codeSource.resolve(packagePath).resolve(HELP_FILE_NAME);
                if (Files.isRegularFile(classDirHelp)) {
                    return classDirHelp;
                }
                return codeSource.resolve(HELP_FILE_NAME);
            }

            Path jarParent = codeSource.getParent();
            return jarParent == null ? null : jarParent.resolve(HELP_FILE_NAME);
        } catch (URISyntaxException | SecurityException | NullPointerException ex) {
            return null;
        }
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
