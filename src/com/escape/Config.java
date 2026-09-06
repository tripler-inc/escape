package com.escape;

import java.awt.Color;
import java.awt.Font;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code config.ini} once at startup and exposes each section as
 * typed fields.  Values are the single source of truth for tunables.
 */
public final class Config {

    private static final String CONFIG_FILE_NAME = "config.ini";

    private static final Map<String, Map<String, String>> DATA = loadIni();

    public static final WorldSection WORLD = new WorldSection();
    public static final GamePanelSection GAME_PANEL = new GamePanelSection();
    public static final MapRendererSection MAP = new MapRendererSection();
    public static final FirstPersonRendererSection FP = new FirstPersonRendererSection();
    public static final FirstPersonItemsSection FPI = new FirstPersonItemsSection();
    public static final HUDRendererSection HUD = new HUDRendererSection();
    public static final HelpOverlaySection HELP = new HelpOverlaySection();
    public static final SoundPlayerSection SOUND = new SoundPlayerSection();
    public static final AudioPlayerSection AUDIO = new AudioPlayerSection();
    public static final ConfettiSection CONFETTI = new ConfettiSection();

    private Config() {}

    public static final class WorldSection {
        public final int numFloors;
        public final int coinsPerFloor;
        public final int rows;
        public final int cols;
        public final int[] odd;
        public final int lockedMsgDuration;
        public final int scoreCoin;
        public final int scoreKey;
        public final int scoreMap;
        public final int bonusSecondsPerFloor;

        private WorldSection() {
            numFloors          = getInt("World", "NUM_FLOORS");
            coinsPerFloor      = getInt("World", "COINS_PER_FLOOR");
            rows               = getInt("World", "ROWS");
            cols               = getInt("World", "COLS");
            odd                = getIntArray("World", "ODD");
            lockedMsgDuration  = getInt("World", "LOCKED_MSG_DURATION");
            scoreCoin          = getInt("World", "SCORE_COIN");
            scoreKey           = getInt("World", "SCORE_KEY");
            scoreMap           = getInt("World", "SCORE_MAP");
            bonusSecondsPerFloor = getInt("World", "BONUS_SECONDS_PER_FLOOR");
        }
    }

    public static final class GamePanelSection {
        public final int width;
        public final int height;
        public final float moveSpeed;
        public final float rotSpeed;
        public final float maxMoveSpeed;
        public final float maxRotSpeed;
        public final float moveAccel;
        public final float rotAccel;
        public final int tickMs;
        public final int mapHintDurationMs;
        public final Color backgroundColor;

        private GamePanelSection() {
            width            = getInt("GamePanel", "WIDTH");
            height           = getInt("GamePanel", "HEIGHT");
            moveSpeed        = getFloat("GamePanel", "MOVE_SPEED");
            rotSpeed         = getFloat("GamePanel", "ROT_SPEED");
            maxMoveSpeed     = getFloat("GamePanel", "MAX_MOVE_SPEED");
            maxRotSpeed      = getFloat("GamePanel", "MAX_ROT_SPEED");
            moveAccel        = getFloat("GamePanel", "MOVE_ACCEL");
            rotAccel         = getFloat("GamePanel", "ROT_ACCEL");
            tickMs           = getInt("GamePanel", "TICK_MS");
            mapHintDurationMs = getInt("GamePanel", "MAP_HINT_DURATION_MS");
            backgroundColor  = getColor("GamePanel", "BACKGROUND_COLOR");
        }
    }

    public static final class MapRendererSection {
        public final Color backgroundColor;
        public final Color border;
        public final Color wall;
        public final Color passage;
        public final Color player;
        public final Color ladderUpSeen;
        public final Color ladderDown;
        public final Color markerOutline;
        public final Color exitSeen;
        public final String labelFloor;
        public final String labelTime;
        public final String labelScore;
        public final String labelTimeBonus;
        public final String labelAllCoinsBonus;
        public final Font labelFont;

        private MapRendererSection() {
            backgroundColor = getColor("MapRenderer", "BACKGROUND_COLOR");
            border          = getColor("MapRenderer", "COL_BORDER");
            wall            = getColor("MapRenderer", "COL_WALL");
            passage         = getColor("MapRenderer", "COL_PASSAGE");
            player          = getColor("MapRenderer", "COL_PLAYER");
            ladderUpSeen    = getColor("MapRenderer", "COL_LADDER_UP_SEEN");
            ladderDown      = getColor("MapRenderer", "COL_LADDER_DOWN");
            markerOutline   = getColor("MapRenderer", "COL_MARKER_OUTLINE");
            exitSeen        = getColor("MapRenderer", "COL_EXIT_SEEN");
            labelFloor      = getString("MapRenderer", "LABEL_FLOOR");
            labelTime       = getString("MapRenderer", "LABEL_TIME");
            labelScore      = getString("MapRenderer", "LABEL_SCORE");
            labelTimeBonus  = getString("MapRenderer", "LABEL_TIME_BONUS");
            labelAllCoinsBonus = getString("MapRenderer", "LABEL_ALL_COINS_BONUS");
            labelFont       = getFont("MapRenderer", "LABEL_FONT");
        }
    }

    public static final class FirstPersonRendererSection {
        public final Color ceiling;
        public final Color floor;
        public final Color coinGold;
        public final Color coinHighlight;
        public final Color keyGold;
        public final Color keyHighlight;
        public final Color ladderRail;
        public final Color ladderRung;
        public final Color holeDark;
        public final Color holeEdge;
        public final Color mapPaper;
        public final Color mapFold;
        public final Color mapInk;
        public final int baseNs;
        public final int baseEw;
        public final double cameraPlaneScale;

        private FirstPersonRendererSection() {
            ceiling           = getColor("FirstPersonRenderer", "COL_CEILING");
            floor             = getColor("FirstPersonRenderer", "COL_FLOOR");
            coinGold          = getColor("FirstPersonRenderer", "COL_COIN_GOLD");
            coinHighlight     = getColor("FirstPersonRenderer", "COL_COIN_HLIGHT");
            keyGold           = getColor("FirstPersonRenderer", "COL_KEY_GOLD");
            keyHighlight      = getColor("FirstPersonRenderer", "COL_KEY_HLIGHT");
            ladderRail        = getColor("FirstPersonRenderer", "COL_LADDER_RAIL");
            ladderRung        = getColor("FirstPersonRenderer", "COL_LADDER_RUNG");
            holeDark          = getColor("FirstPersonRenderer", "COL_HOLE_DARK");
            holeEdge          = getColor("FirstPersonRenderer", "COL_HOLE_EDGE");
            mapPaper          = getColor("FirstPersonRenderer", "COL_MAP_PAPER");
            mapFold           = getColor("FirstPersonRenderer", "COL_MAP_FOLD");
            mapInk            = getColor("FirstPersonRenderer", "COL_MAP_INK");
            baseNs            = getInt("FirstPersonRenderer", "BASE_NS");
            baseEw            = getInt("FirstPersonRenderer", "BASE_EW");
            cameraPlaneScale  = getDouble("FirstPersonRenderer", "CAMERA_PLANE_SCALE");
        }
    }

    public static final class FirstPersonItemsSection {
        public final double coinWorldHeight;
        public final double keyWorldHeight;
        public final double mapWorldHeight;
        public final double holeWorldHeightOffset;

        private FirstPersonItemsSection() {
            coinWorldHeight      = getDouble("FirstPersonItems", "COIN_WORLD_HEIGHT");
            keyWorldHeight       = getDouble("FirstPersonItems", "KEY_WORLD_HEIGHT");
            mapWorldHeight       = getDouble("FirstPersonItems", "MAP_WORLD_HEIGHT");
            holeWorldHeightOffset = getDouble("FirstPersonItems", "HOLE_WORLD_HEIGHT_OFFSET");
        }
    }

    public static final class HUDRendererSection {
        public final Color coin;
        public final Color coinBorder;
        public final Color coinShine;
        public final Color coinText;
        public final Color keyColor;
        public final Color keyBorder;
        public final Color keyHole;
        public final Color keyShaft;
        public final Color keyShaftBorder;
        public final String hintUp;
        public final String hintDown;
        public final Font hintFont;
        public final Color hintFill;
        public final Color hintText;
        public final String hintLocked;
        public final Font lockedFont;
        public final Color lockedFill;
        public final Color lockedText;
        public final String hintMapMissing;
        public final Font mapHintFont;
        public final Color mapHintFill;
        public final Color mapHintText;

        private HUDRendererSection() {
            coin            = getColor("HUDRenderer", "COL_COIN");
            coinBorder      = getColor("HUDRenderer", "COL_COIN_BRDR");
            coinShine       = getColor("HUDRenderer", "COL_COIN_SHINE");
            coinText        = getColor("HUDRenderer", "COL_COIN_TEXT");
            keyColor        = getColor("HUDRenderer", "COL_KEY_COLOR");
            keyBorder       = getColor("HUDRenderer", "COL_KEY_BORDER");
            keyHole         = getColor("HUDRenderer", "COL_KEY_HOLE");
            keyShaft        = getColor("HUDRenderer", "COL_KEY_SHAFT");
            keyShaftBorder  = getColor("HUDRenderer", "COL_KEY_SHAFT_BRDR");
            hintUp          = getString("HUDRenderer", "HINT_UP");
            hintDown        = getString("HUDRenderer", "HINT_DOWN");
            hintFont        = getFont("HUDRenderer", "HINT_FONT");
            hintFill        = getColor("HUDRenderer", "COL_HINT_FILL");
            hintText        = getColor("HUDRenderer", "COL_HINT_TEXT");
            hintLocked      = getString("HUDRenderer", "HINT_LOCKED");
            lockedFont      = getFont("HUDRenderer", "LOCKED_FONT");
            lockedFill      = getColor("HUDRenderer", "COL_LOCKED_FILL");
            lockedText      = getColor("HUDRenderer", "COL_LOCKED_TEXT");
            hintMapMissing  = getString("HUDRenderer", "HINT_MAP_MISSING");
            mapHintFont     = getFont("HUDRenderer", "MAP_HINT_FONT");
            mapHintFill     = getColor("HUDRenderer", "COL_MAP_HINT_FILL");
            mapHintText     = getColor("HUDRenderer", "COL_MAP_HINT_TEXT");
        }
    }

    public static final class SoundPlayerSection {
        public final float sampleRate;
        public final int lineBufferSize;
        public final double envelopeAttackRatio;
        public final double envelopeDecayPower;
        public final int footstepIntervalMs;
        public final double[] footstepFreqs;
        public final int footstepDurationMs;
        public final int footstepGapMs;
        public final double footstepVolume;
        public final double[] keyPickupFreqs;
        public final int keyPickupDurationMs;
        public final int keyPickupGapMs;
        public final double keyPickupVolume;
        public final int mapShuffleDurationMs;
        public final double mapShuffleVolume;
        public final double mapShuffleHiPassHz;
        public final double[] coinClinkFreqs;
        public final int coinClinkDurationMs;
        public final int coinClinkGapMs;
        public final double coinClinkVolume;
        public final double[] ladderUpFreqs;
        public final int ladderUpDurationMs;
        public final int ladderUpGapMs;
        public final double ladderUpVolume;
        public final double[] ladderDownFreqs;
        public final int ladderDownDurationMs;
        public final int ladderDownGapMs;
        public final double ladderDownVolume;

        private SoundPlayerSection() {
            sampleRate            = getFloat("SoundPlayer", "SAMPLE_RATE");
            lineBufferSize        = getInt("SoundPlayer", "LINE_BUFFER_SIZE");
            envelopeAttackRatio   = getDouble("SoundPlayer", "ENVELOPE_ATTACK_RATIO");
            envelopeDecayPower    = getDouble("SoundPlayer", "ENVELOPE_DECAY_POWER");
            footstepIntervalMs    = getInt("SoundPlayer", "FOOTSTEP_INTERVAL_MS");
            footstepFreqs         = getDoubleArray("SoundPlayer", "FOOTSTEP_FREQS");
            footstepDurationMs    = getInt("SoundPlayer", "FOOTSTEP_DURATION_MS");
            footstepGapMs         = getInt("SoundPlayer", "FOOTSTEP_GAP_MS");
            footstepVolume        = getDouble("SoundPlayer", "FOOTSTEP_VOLUME");
            keyPickupFreqs        = getDoubleArray("SoundPlayer", "KEY_PICKUP_FREQS");
            keyPickupDurationMs   = getInt("SoundPlayer", "KEY_PICKUP_DURATION_MS");
            keyPickupGapMs        = getInt("SoundPlayer", "KEY_PICKUP_GAP_MS");
            keyPickupVolume       = getDouble("SoundPlayer", "KEY_PICKUP_VOLUME");
            mapShuffleDurationMs  = getInt("SoundPlayer", "MAP_SHUFFLE_DURATION_MS");
            mapShuffleVolume      = getDouble("SoundPlayer", "MAP_SHUFFLE_VOLUME");
            mapShuffleHiPassHz    = getDouble("SoundPlayer", "MAP_SHUFFLE_HIPASS_HZ");
            coinClinkFreqs        = getDoubleArray("SoundPlayer", "COIN_CLINK_FREQS");
            coinClinkDurationMs   = getInt("SoundPlayer", "COIN_CLINK_DURATION_MS");
            coinClinkGapMs        = getInt("SoundPlayer", "COIN_CLINK_GAP_MS");
            coinClinkVolume       = getDouble("SoundPlayer", "COIN_CLINK_VOLUME");
            ladderUpFreqs         = getDoubleArray("SoundPlayer", "LADDER_UP_FREQS");
            ladderUpDurationMs    = getInt("SoundPlayer", "LADDER_UP_DURATION_MS");
            ladderUpGapMs         = getInt("SoundPlayer", "LADDER_UP_GAP_MS");
            ladderUpVolume        = getDouble("SoundPlayer", "LADDER_UP_VOLUME");
            ladderDownFreqs       = getDoubleArray("SoundPlayer", "LADDER_DOWN_FREQS");
            ladderDownDurationMs  = getInt("SoundPlayer", "LADDER_DOWN_DURATION_MS");
            ladderDownGapMs       = getInt("SoundPlayer", "LADDER_DOWN_GAP_MS");
            ladderDownVolume      = getDouble("SoundPlayer", "LADDER_DOWN_VOLUME");
        }
    }

    public static final class HelpOverlaySection {
        public final Color dimBg;
        public final Color panelBg;
        public final Color panelBorder;
        public final Color textColor;
        public final Font textFont;
        public final int outerMargin;
        public final int panelPaddingX;
        public final int panelPaddingY;
        public final String startupPromptText;
        public final Font startupPromptFont;
        public final Color startupPromptBg;
        public final Color startupPromptBorder;
        public final Color startupPromptTextColor;
        public final int startupPromptMinAlpha;
        public final int startupPromptMaxAlpha;
        public final int startupPromptPulseMs;
        public final int startupPromptPadX;
        public final int startupPromptPadY;
        public final int startupPromptRadius;
        public final int startupPromptBaselineOffset;

        private HelpOverlaySection() {
            dimBg                      = getColor("HelpOverlay", "COL_DIM_BG");
            panelBg                    = getColor("HelpOverlay", "COL_PANEL_BG");
            panelBorder                = getColor("HelpOverlay", "COL_PANEL_BORDER");
            textColor                  = getColor("HelpOverlay", "COL_TEXT");
            textFont                   = getFont("HelpOverlay", "TEXT_FONT");
            outerMargin                = getInt("HelpOverlay", "OUTER_MARGIN");
            panelPaddingX              = getInt("HelpOverlay", "PANEL_PADDING_X");
            panelPaddingY              = getInt("HelpOverlay", "PANEL_PADDING_Y");
            startupPromptText          = getString("HelpOverlay", "START_PROMPT_TEXT");
            startupPromptFont          = getFont("HelpOverlay", "START_PROMPT_FONT");
            startupPromptBg            = getColor("HelpOverlay", "COL_START_PROMPT_BG");
            startupPromptBorder        = getColor("HelpOverlay", "COL_START_PROMPT_BORDER");
            startupPromptTextColor     = getColor("HelpOverlay", "COL_START_PROMPT_TEXT");
            startupPromptMinAlpha      = getInt("HelpOverlay", "START_PROMPT_MIN_ALPHA");
            startupPromptMaxAlpha      = getInt("HelpOverlay", "START_PROMPT_MAX_ALPHA");
            startupPromptPulseMs       = getInt("HelpOverlay", "START_PROMPT_PULSE_MS");
            startupPromptPadX          = getInt("HelpOverlay", "START_PROMPT_PAD_X");
            startupPromptPadY          = getInt("HelpOverlay", "START_PROMPT_PAD_Y");
            startupPromptRadius        = getInt("HelpOverlay", "START_PROMPT_RADIUS");
            startupPromptBaselineOffset = getInt("HelpOverlay", "START_PROMPT_BASELINE_OFFSET");
        }
    }

    public static final class AudioPlayerSection {
        public final boolean loopContinuous;
        public final int victoryPpq;
        public final int victoryProgram;
        public final int victoryTempoUsPerBeat;
        public final int eotTick;
        public final int[][] victoryMelody;

        private AudioPlayerSection() {
            loopContinuous          = getInt("AudioPlayer", "LOOP_CONTINUOUS") != 0;
            victoryPpq              = getInt("AudioPlayer", "VICTORY_PPQ");
            victoryProgram          = getInt("AudioPlayer", "VICTORY_PROGRAM");
            victoryTempoUsPerBeat   = getInt("AudioPlayer", "VICTORY_TEMPO_US_PER_BEAT");
            eotTick                 = getInt("AudioPlayer", "EOT_TICK");
            victoryMelody           = getIntMatrix("AudioPlayer", "VICTORY_MELODY");
        }
    }

    public static final class ConfettiSection {
        public final int numParticles;
        public final float spawnHeight;
        public final float vxRange;
        public final float vyBase;
        public final float vyRange;
        public final float angVelRange;
        public final int sizeMin;
        public final int sizeRange;
        public final float gravity;
        public final int offscreenMargin;
        public final Color[] palette;
        public final String bannerText;
        public final Font bannerFont;
        public final Color bannerBg;
        public final Color bannerShadow;
        public final Color bannerTextColor;
        public final int bannerPadX;
        public final int bannerBoxTopOffset;
        public final int bannerBoxHeight;
        public final int bannerRadius;
        public final int bannerTextYOffset;
        public final int bannerShadowOffset;

        private ConfettiSection() {
            numParticles        = getInt("ConfettiAnimation", "NUM_PARTICLES");
            spawnHeight         = getFloat("ConfettiAnimation", "SPAWN_HEIGHT");
            vxRange             = getFloat("ConfettiAnimation", "VX_RANGE");
            vyBase              = getFloat("ConfettiAnimation", "VY_BASE");
            vyRange             = getFloat("ConfettiAnimation", "VY_RANGE");
            angVelRange         = getFloat("ConfettiAnimation", "ANG_VEL_RANGE");
            sizeMin             = getInt("ConfettiAnimation", "SIZE_MIN");
            sizeRange           = getInt("ConfettiAnimation", "SIZE_RANGE");
            gravity             = getFloat("ConfettiAnimation", "GRAVITY");
            offscreenMargin     = getInt("ConfettiAnimation", "OFFSCREEN_MARGIN");
            palette             = getColorArray("ConfettiAnimation", "PALETTE");
            bannerText          = getString("ConfettiAnimation", "BANNER_TEXT");
            bannerFont          = getFont("ConfettiAnimation", "BANNER_FONT");
            bannerBg            = getColor("ConfettiAnimation", "BANNER_BG");
            bannerShadow        = getColor("ConfettiAnimation", "BANNER_SHADOW");
            bannerTextColor     = getColor("ConfettiAnimation", "BANNER_TEXT_COLOR");
            bannerPadX          = getInt("ConfettiAnimation", "BANNER_PAD_X");
            bannerBoxTopOffset  = getInt("ConfettiAnimation", "BANNER_BOX_TOP_OFFSET");
            bannerBoxHeight     = getInt("ConfettiAnimation", "BANNER_BOX_HEIGHT");
            bannerRadius        = getInt("ConfettiAnimation", "BANNER_RADIUS");
            bannerTextYOffset   = getInt("ConfettiAnimation", "BANNER_TEXT_Y_OFFSET");
            bannerShadowOffset  = getInt("ConfettiAnimation", "BANNER_SHADOW_OFFSET");
        }
    }

    // ── Typed readers ────────────────────────────────────────────────

    private static String getString(String section, String key) {
        Map<String, String> values = DATA.get(section);
        if (values == null || !values.containsKey(key)) {
            throw new IllegalStateException("Missing config value [" + section + "] " + key);
        }
        return values.get(key);
    }

    private static int getInt(String section, String key) {
        try {
            return Integer.parseInt(getString(section, key));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid int [" + section + "] " + key, e);
        }
    }

    private static float getFloat(String section, String key) {
        try {
            return Float.parseFloat(getString(section, key));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid float [" + section + "] " + key, e);
        }
    }

    private static double getDouble(String section, String key) {
        try {
            return Double.parseDouble(getString(section, key));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid double [" + section + "] " + key, e);
        }
    }

    private static int[] getIntArray(String section, String key) {
        String[] parts = splitCsv(getString(section, key));
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid int array [" + section + "] " + key, e);
            }
        }
        return values;
    }

    private static double[] getDoubleArray(String section, String key) {
        String[] parts = splitCsv(getString(section, key));
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid double array [" + section + "] " + key, e);
            }
        }
        return values;
    }

    private static int[][] getIntMatrix(String section, String key) {
        String raw = getString(section, key);
        String[] rows = raw.split(";");
        int[][] matrix = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String[] parts = splitCsv(rows[r]);
            matrix[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) {
                try {
                    matrix[r][c] = Integer.parseInt(parts[c]);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Invalid int matrix [" + section + "] " + key, e);
                }
            }
        }
        return matrix;
    }

    private static Color getColor(String section, String key) {
        return parseColor(getString(section, key), section, key);
    }

    private static Color[] getColorArray(String section, String key) {
        String[] parts = getString(section, key).split("\\|");
        Color[] colors = new Color[parts.length];
        for (int i = 0; i < parts.length; i++) {
            colors[i] = parseColor(parts[i].trim(), section, key);
        }
        return colors;
    }

    private static Font getFont(String section, String key) {
        String spec = getString(section, key);
        int last = spec.lastIndexOf('-');
        int prev = last < 0 ? -1 : spec.lastIndexOf('-', last - 1);
        if (last < 0 || prev < 0) {
            throw new IllegalStateException(
                    "Invalid font [" + section + "] " + key + " (expected Family-STYLE-size)");
        }
        String family = spec.substring(0, prev);
        String styleName = spec.substring(prev + 1, last);
        int size;
        try {
            size = Integer.parseInt(spec.substring(last + 1));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid font size [" + section + "] " + key, e);
        }
        int style;
        switch (styleName.toUpperCase()) {
            case "BOLD":        style = Font.BOLD; break;
            case "ITALIC":      style = Font.ITALIC; break;
            case "BOLDITALIC":  style = Font.BOLD | Font.ITALIC; break;
            case "PLAIN":       style = Font.PLAIN; break;
            default:
                throw new IllegalStateException("Invalid font style [" + section + "] " + key);
        }
        return new Font(family, style, size);
    }

    private static Color parseColor(String raw, String section, String key) {
        String[] parts = splitCsv(raw);
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalStateException("Invalid color [" + section + "] " + key);
        }
        try {
            int r = Integer.parseInt(parts[0]);
            int g = Integer.parseInt(parts[1]);
            int b = Integer.parseInt(parts[2]);
            if (parts.length == 4) {
                return new Color(r, g, b, Integer.parseInt(parts[3]));
            }
            return new Color(r, g, b);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid color [" + section + "] " + key, e);
        }
    }

    private static String[] splitCsv(String raw) {
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static String parseIniValue(String rawValue) {
        String trimmed = rawValue.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return unescapeQuotedIni(trimmed.substring(1, trimmed.length() - 1));
        }
        return trimmed;
    }

    private static String unescapeQuotedIni(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                if (ch == '\\' || ch == '"') {
                    out.append(ch);
                } else {
                    out.append('\\').append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                out.append(ch);
            }
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }

    // ── INI loader ───────────────────────────────────────────────────

    private static Map<String, Map<String, String>> loadIni() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(openConfigStream(), StandardCharsets.UTF_8))) {
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            String current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith(";")) {
                    continue;
                }
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    current = trimmedLine.substring(1, trimmedLine.length() - 1).trim();
                    sections.putIfAbsent(current, new LinkedHashMap<>());
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0 || current == null) {
                    throw new IllegalStateException("Invalid config line: " + trimmedLine);
                }
                String key = line.substring(0, eq).trim();
                String value = parseIniValue(line.substring(eq + 1));
                sections.get(current).put(key, value);
            }
            return sections;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read config.ini", e);
        }
    }

    private static InputStream openConfigStream() throws IOException {
        Path externalConfig = resolveExternalConfigPath();
        if (externalConfig != null && Files.isRegularFile(externalConfig)) {
            return Files.newInputStream(externalConfig);
        }

        InputStream resource = Config.class.getResourceAsStream(CONFIG_FILE_NAME);
        if (resource != null) {
            return resource;
        }

        throw new IOException(CONFIG_FILE_NAME + " not found next to runtime artifact or in classpath resource");
    }

    private static Path resolveExternalConfigPath() {
        try {
            URL location = Config.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            Path codeSource = Paths.get(location.toURI());

            if (Files.isDirectory(codeSource)) {
                String packagePath = Config.class.getPackageName().replace('.', java.io.File.separatorChar);
                Path classDirConfig = codeSource.resolve(packagePath).resolve(CONFIG_FILE_NAME);
                if (Files.isRegularFile(classDirConfig)) {
                    return classDirConfig;
                }
                return codeSource.resolve(CONFIG_FILE_NAME);
            }

            Path jarParent = codeSource.getParent();
            return jarParent == null ? null : jarParent.resolve(CONFIG_FILE_NAME);
        } catch (URISyntaxException | SecurityException | NullPointerException ex) {
            return null;
        }
    }
}
