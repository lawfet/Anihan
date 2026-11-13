package managers;

import farming.HarvestedCrop;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AssetManager loads, parses, and provides textures, sprite animations,
 * UI components, and synthesized audio for ANIHAN.
 */
public class AssetManager {

    private static AssetManager instance;

    // Res path root
    private final String resRoot;
    private final Map<String, BufferedImage> resourceImages = new java.util.TreeMap<>();
    private final java.util.List<String> brokenResources = new java.util.ArrayList<>();

    // Player animation strips
    // [action][frame]
    public enum Action {
        IDLE("idle", 9),
        WALK("walking", 8),
        RUN("run", 8),
        DIG("dig", 13),
        WATERING("watering", 5),
        AXE("axe", 10),
        CARRY("carry", 8),
        DOING("doing", 8),
        MINING("mining", 10),
        HAMMERING("hammering", 23),
        CASTING("casting", 15),
        WAITING("waiting", 9),
        REELING("reeling", 13),
        CAUGHT("caught", 10),
        JUMP("jump", 9),
        ROLL("roll", 10),
        SWIMMING("swimming", 12),
        ATTACK("attack", 10),
        HURT("hurt", 8),
        DEATH("death", 13);

        public final String name;
        public final int frameCount;

        Action(String name, int frameCount) {
            this.name = name;
            this.frameCount = frameCount;
        }
    }

    private final Map<Action, BufferedImage[]> baseAnims = new HashMap<>();
    private final Map<String, Map<Action, BufferedImage[]>> hairAnims = new HashMap<>();
    private final Map<Action, BufferedImage[]> toolAnims = new HashMap<>();
    private BufferedImage baseSingle;

    // Available hairstyles
    public static final String[] HAIR_STYLES = {
        "bowlhair", "curlyhair", "longhair", "mophair", "shorthair", "spikeyhair"
    };

    // UI assets
    // Menu buttons: [row: Play, Options, Quit, Credits][col: normal, hover, pressed]
    private BufferedImage[][] menuButtons = new BufferedImage[4][3];
    private BufferedImage statBars;
    private BufferedImage menuBoard;
    private BufferedImage optionsBoard;
    private BufferedImage pauseBoard;
    // Audio toggles: [row: unmuted, muted][col: normal, hover, pressed]
    private BufferedImage[][] audioToggle = new BufferedImage[2][3];
    // Nav buttons: [row: Resume(green), Return(yellow), Home(red)][col: normal, hover, pressed]
    private BufferedImage[][] navButtons = new BufferedImage[3][3];
    private BufferedImage volumeSlider;
    private BufferedImage titleBg;
    private final Map<String, BufferedImage> uiImages = new HashMap<>();
    private final Map<String, BufferedImage[]> scenery = new HashMap<>();
    private BufferedImage farmhouse;
    private BufferedImage shippingCrate;
    private BufferedImage shippingCrateTop;
    private BufferedImage rock;
    private BufferedImage coracleLand;
    private final Map<String, BufferedImage> buildings = new HashMap<>();

    // Crops and Soils
    private BufferedImage soilDry;
    private BufferedImage soilWatered;
    private BufferedImage soilDryAlt;
    private BufferedImage soilWateredAlt;
    private final Map<String, BufferedImage> cropSprites = new HashMap<>();
    private final Map<String, BufferedImage> materialSprites = new HashMap<>();
    private BufferedImage exampleScene;

    public BufferedImage getMaterialImage(farming.FarmProgress.Material material) {
        if (material == farming.FarmProgress.Material.MUSHROOM) {
            BufferedImage[] mushrooms = scenery.get("mushroom");
            return mushrooms != null && mushrooms.length > 0 ? mushrooms[0] : null;
        }
        if (material == farming.FarmProgress.Material.FLOUR) return getCropStageImage(HarvestedCrop.CropType.WHEAT, 5);
        return materialSprites.get(material.asset);
    }

    /** Small collectible icons used by fishing, gathering and the backpack ledger. */
    public BufferedImage getItemImage(String name) { return materialSprites.get(name); }

    // Animals & Plants
    private BufferedImage[] chickenFrames;
    private BufferedImage[] cowFrames;
    private BufferedImage[] sheepFrames;
    private BufferedImage[] pigFrames;
    private BufferedImage[] treeFrames;

    // Tileset
    private BufferedImage tileset;

    // Audio Engine
    private volatile float masterVolume = 0.8f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;
    private volatile boolean isMuted = false;
    private volatile Clip bgmClip;

    // Pre-synthesized sound clips
    private volatile Clip sfxClick;
    private volatile Clip sfxStep;
    private volatile Clip sfxDig;
    private volatile Clip sfxWater;
    private volatile Clip sfxHarvest;
    private volatile Clip sfxChop;
    private volatile Clip sfxCoin;
    private volatile Clip sfxRooster;

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    private AssetManager() {
        resRoot = resolveResourceRoot();
        loadResourceCatalog();
        loadPlayerSprites();
        loadUI();
        loadCropsAndSoil();
        loadAnimalsAndPlants();
        loadTileset();
        initAudio();
    }

    private String resolveResourceRoot() {
        try {
            File code = new File(AssetManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File parent = code.getParentFile();
            for (File candidate : new File[]{new File(parent, "res"), new File(code, "res"), new File("res")}) {
                if (candidate.isDirectory()) return candidate.getAbsolutePath();
            }
        } catch (java.net.URISyntaxException exception) {
            throw new IllegalStateException("Cannot resolve game location", exception);
        }
        throw new IllegalStateException("Game resources not found. Keep res beside bin.");
    }

    private void loadResourceCatalog() {
        java.nio.file.Path root = new File(resRoot).toPath();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(root)) {
            files.filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                    .sorted().forEach(path -> {
                        String key = root.relativize(path).toString().replace('\\', '/');
                        BufferedImage image = tryLoadImage(key);
                        if (image != null) {
                            resourceImages.put(key, image);
                        } else {
                            brokenResources.add(key);
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot index game artwork", exception);
        }
    }

    public java.util.List<String> getResourceNames() {
        return java.util.List.copyOf(resourceImages.keySet());
    }

    public java.util.List<String> getBrokenResourceNames() {
        return java.util.List.copyOf(brokenResources);
    }

    public BufferedImage getResourceImage(String name) { return resourceImages.get(name); }

    public java.io.InputStream openResource(String name) throws IOException {
        return new java.io.FileInputStream(new File(resRoot, name));
    }

    private BufferedImage loadImage(String relativePath) {
        BufferedImage image = tryLoadImage(relativePath);
        if (image == null) {
            File file = new File(resRoot, relativePath);
            throw new IllegalStateException("Cannot load game artwork: " + file.getAbsolutePath());
        }
        return image;
    }

    /**
     * Loads an optional image. Returns null instead of aborting the game when a
     * single decorative asset is missing or corrupt, so the rest of the game
     * can still run with a graceful fallback.
     */
    private BufferedImage tryLoadImage(String relativePath) {
        BufferedImage cached = resourceImages.get(relativePath);
        if (cached != null) return cached;
        File file = new File(resRoot, relativePath);
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                System.err.println("[AssetManager] Skipping unreadable artwork: " + file.getPath());
            }
            return image;
        } catch (IOException | RuntimeException e) {
            System.err.println("[AssetManager] Skipping unreadable artwork: " + file.getPath());
            return null;
        }
    }

    private BufferedImage[] splitStrip(BufferedImage strip, int frameCount) {
        if (strip == null || frameCount <= 0) return null;
        int frameW = strip.getWidth() / frameCount;
        int frameH = strip.getHeight();
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = strip.getSubimage(i * frameW, 0, frameW, frameH);
        }
        return frames;
    }

    private void loadPlayerSprites() {
        baseSingle = tryLoadImage("sprites/player/base.png");

        for (String hair : HAIR_STYLES) hairAnims.put(hair, new HashMap<Action, BufferedImage[]>());
        for (Action action : Action.values()) {
            String suffix = action == Action.WALK ? "walk" : action == Action.HAMMERING ? "hamering" : action.name;
            String ending = "_" + suffix + "_strip" + action.frameCount + ".png";
            loadBaseAction(action, "sprites/player/" + action.name + "_base" + ending);
            toolAnims.put(action, splitStrip(tryLoadImage("sprites/player/equipment/" + action.name + "_tools" + ending), action.frameCount));
            for (String hair : HAIR_STYLES) {
                hairAnims.get(hair).put(action, splitStrip(tryLoadImage("sprites/player/hair/" + action.name + "_" + hair + ending), action.frameCount));
            }
        }
    }

    private void loadBaseAction(Action action, String relPath) {
        BufferedImage strip = tryLoadImage(relPath);
        if (strip != null) {
            baseAnims.put(action, splitStrip(strip, action.frameCount));
        }
    }

    private void loadUI() {
        // Load menu buttons sheet (420x224: 4 rows x 3 columns)
        BufferedImage mb = tryLoadImage("ui/menu_buttons.png");
        if (mb != null) {
            int bw = mb.getWidth() / 3;
            int bh = mb.getHeight() / 4;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    menuButtons[r][c] = mb.getSubimage(c * bw, r * bh, bw, bh);
                }
            }
        }

        statBars = tryLoadImage("ui/stat_bars.png");
        menuBoard = tryLoadImage("ui/menu_board.png");
        optionsBoard = tryLoadImage("ui/options_board.png");
        pauseBoard = tryLoadImage("ui/pause_board.png");

        // Audio toggle (126x84: 2 rows x 3 columns)
        BufferedImage at = tryLoadImage("ui/audio_toggle.png");
        if (at != null) {
            int tw = at.getWidth() / 3;
            int th = at.getHeight() / 2;
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 3; c++) {
                    audioToggle[r][c] = at.getSubimage(c * tw, r * th, tw, th);
                }
            }
        }

        // Nav buttons (168x168: 3 rows x 3 columns)
        BufferedImage nb = tryLoadImage("ui/nav_buttons.png");
        if (nb != null) {
            int nw = nb.getWidth() / 3;
            int nh = nb.getHeight() / 3;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    navButtons[r][c] = nb.getSubimage(c * nw, r * nh, nw, nh);
                }
            }
        }

        volumeSlider = tryLoadImage("ui/volume_slider.png");
        titleBg = tryLoadImage("ui/title_bg.png");
        File[] images = new File(resRoot, "ui").listFiles((dir, name) -> name.endsWith(".png"));
        if (images != null) {
            for (File image : images) {
                String name = image.getName();
                BufferedImage loaded = tryLoadImage("ui/" + name);
                if (loaded != null) {
                    uiImages.put(name.substring(0, name.length() - 4), loaded);
                }
            }
        }
    }

    private void loadCropsAndSoil() {
        soilDry = tryLoadImage("sprites/crops/soil_00.png");
        soilWatered = tryLoadImage("sprites/crops/soil_01.png");
        soilDryAlt = tryLoadImage("sprites/crops/soil_03.png");
        soilWateredAlt = tryLoadImage("sprites/crops/soil_04.png");

        // Handy collectible / crafting icons used by fishing and gathering.
        String[] items = {"fish", "egg", "milk", "wood", "rock", "seeds_generic"};
        for (String item : items) {
            BufferedImage image = tryLoadImage("sprites/crops/" + item + ".png");
            if (image != null) materialSprites.put(item, image);
        }

        // Preload crops
        for (HarvestedCrop.CropType type : HarvestedCrop.CropType.values()) {
            String prefix = type.name().toLowerCase();
            for (int stage = 0; stage <= 5; stage++) {
                String fname = String.format("%s_%02d.png", prefix, stage);
                BufferedImage img = tryLoadImage("sprites/crops/" + fname);
                if (img != null) {
                    cropSprites.put(prefix + "_" + stage, img);
                }
            }
        }
    }

    private void loadAnimalsAndPlants() {
        chickenFrames = splitStrip(tryLoadImage("sprites/animals/spr_deco_chicken_01_strip4.png"), 4);
        cowFrames = splitStrip(tryLoadImage("sprites/animals/spr_deco_cow_strip4.png"), 4);
        sheepFrames = splitStrip(tryLoadImage("sprites/animals/spr_deco_sheep_01_strip4.png"), 4);
        pigFrames = splitStrip(tryLoadImage("sprites/animals/spr_deco_pig_01_strip4.png"), 4);
        treeFrames = splitStrip(tryLoadImage("sprites/plants/spr_deco_tree_01_strip4.png"), 4);
        scenery.put("tree", treeFrames);
        scenery.put("pine", splitStrip(tryLoadImage("sprites/plants/spr_deco_tree_02_strip4.png"), 4));
        scenery.put("mushroom", splitStrip(tryLoadImage("sprites/plants/spr_deco_mushroom_red_01_strip4.png"), 4));
        scenery.put("mushroom_blue", splitStrip(tryLoadImage("sprites/plants/spr_deco_mushroom_blue_01_strip4.png"), 4));
        scenery.put("mushroom_blue2", splitStrip(tryLoadImage("sprites/plants/spr_deco_mushroom_blue_02_strip4.png"), 4));
        scenery.put("mushroom_blue3", splitStrip(tryLoadImage("sprites/plants/spr_deco_mushroom_blue_03_strip4.png"), 4));
        scenery.put("windmill", splitStrip(tryLoadImage("sprites/other/spr_deco_windmill_withshadow_strip9.png"), 9));
        scenery.put("boat", splitStrip(tryLoadImage("sprites/other/spr_deco_coracle_strip4.png"), 4));
        scenery.put("duck", splitStrip(tryLoadImage("sprites/animals/spr_deco_duck_01_strip4.png"), 4));
        scenery.put("bird", splitStrip(tryLoadImage("sprites/animals/spr_deco_bird_01_strip4.png"), 4));
        scenery.put("blinking", splitStrip(tryLoadImage("sprites/animals/spr_deco_blinking_strip12.png"), 12));
        scenery.put("chimney", splitStrip(tryLoadImage("sprites/chimneysmoke_01_strip30.png"), 30));
        farmhouse = tryLoadImage("sprites/farmhouse.png");
        shippingCrate = tryLoadImage("sprites/crops/crate_base.png");
        shippingCrateTop = tryLoadImage("sprites/crops/crate_top.png");
        rock = tryLoadImage("sprites/crops/rock.png");
        coracleLand = tryLoadImage("sprites/other/spr_deco_coracle_land.png");
        exampleScene = tryLoadImage("sprites/Example Scene.png");
        String[] buildingNames = {
            "blue_cottage", "tall_house", "arch_hall", "watch_tower",
            "small_shed", "blue_house", "blue_tower", "arch_gate",
            "blue_cottage_alt", "blue_cottage_small", "wood_pile",
            // Complete, cleanly cropped buildings and props from res/maps/tileset.png.
            "ts_cottage_blue", "ts_cottage_green", "ts_cottage_orange",
            "ts_cottage_red", "ts_cottage_purple",
            "tower_blue", "house_blue2", "house_blue3", "house_blue4",
            "house_brown", "ladder", "plank_wide", "plank_thin", "stairs",
            "post", "beam", "awning_blue",
            "bush_green", "bush_berry_red", "bush_berry_orange",
            "rock_big", "rock_med", "rock_small",
            "crate_tall", "crate_small", "crate_lid"
        };
        for (String name : buildingNames) {
            buildings.put(name, tryLoadImage("sprites/buildings/" + name + ".png"));
        }
    }

    private void loadTileset() {
        tileset = tryLoadImage("maps/tileset.png");
    }

    // ==========================================
    // AUDIO SYNTHESIS & ENGINE
    // ==========================================
    private void initAudio() {
        // Run audio setup asynchronously in a daemon thread so it never blocks game startup
        Thread audioThread = new Thread(() -> {
            try {
                sfxClick = createToneClip(800, 70, 0.2f, "square");
                sfxStep = createNoiseClip(45, 0.15f);
                sfxDig = createToneSlideClip(260, 110, 120, 0.35f);
                sfxWater = createWaterClip(180, 0.35f);
                sfxHarvest = createArpeggioClip(new int[]{440, 554, 659, 880}, 90, 0.4f);
                sfxChop = createToneSlideClip(320, 90, 100, 0.35f);
                sfxCoin = createArpeggioClip(new int[]{988, 1319}, 80, 0.35f);
                sfxRooster = createToneSlideClip(350, 700, 350, 0.45f);

                // Synthesize ambient farm BGM loop
                bgmClip = createFarmBgmClip();
                if (bgmClip != null) {
                    bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                    setVolume(bgmClip, musicVolume * masterVolume);
                }
            } catch (Throwable t) {
                // Audio unavailable or headless environment
            }
        }, "AnihanAudioThread");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    public void playClick() { playClip(sfxClick, sfxVolume); }
    public void playStep() { playClip(sfxStep, sfxVolume * 0.4f); }
    public void playDig() { playClip(sfxDig, sfxVolume); }
    public void playWater() { playClip(sfxWater, sfxVolume); }
    public void playHarvest() { playClip(sfxHarvest, sfxVolume); }
    public void playChop() { playClip(sfxChop, sfxVolume); }
    public void playCoin() { playClip(sfxCoin, sfxVolume); }
    public void playRooster() { playClip(sfxRooster, sfxVolume); }

    private void playClip(Clip clip, float volume) {
        if (isMuted || clip == null) return;
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            setVolume(clip, volume * masterVolume);
            clip.start();
        } catch (Exception ignored) {}
    }

    private void setVolume(Clip clip, float vol) {
        if (clip == null) return;
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (isMuted || vol <= 0.001f) {
                gain.setValue(gain.getMinimum());
            } else {
                float dB = (float) (Math.log10(Math.max(0.0001f, vol)) * 20.0);
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }
        } catch (Exception ignored) {}
    }

    public void setMasterVolume(float vol) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, vol));
        if (bgmClip != null) setVolume(bgmClip, musicVolume * masterVolume);
    }

    public void toggleMute() {
        this.isMuted = !this.isMuted;
        if (bgmClip != null) setVolume(bgmClip, isMuted ? 0 : musicVolume * masterVolume);
    }

    public boolean isMuted() { return isMuted; }
    public float getMasterVolume() { return masterVolume; }

    // Synthesizer helper functions
    private Clip createToneClip(double freq, int ms, float amplitude, String type) throws Exception {
        int sampleRate = 44100;
        int numSamples = (sampleRate * ms) / 1000;
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double sample = 0;
            if ("square".equals(type)) {
                sample = Math.sin(2.0 * Math.PI * freq * t) > 0 ? 1.0 : -1.0;
            } else {
                sample = Math.sin(2.0 * Math.PI * freq * t);
            }
            // Linear decay envelope
            double env = 1.0 - ((double) i / numSamples);
            buffer[i] = (byte) (sample * env * amplitude * 127);
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip createToneSlideClip(double startFreq, double endFreq, int ms, float amplitude) throws Exception {
        int sampleRate = 44100;
        int numSamples = (sampleRate * ms) / 1000;
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double freq = startFreq + (endFreq - startFreq) * progress;
            double t = (double) i / sampleRate;
            double sample = Math.sin(2.0 * Math.PI * freq * t);
            double env = 1.0 - progress;
            buffer[i] = (byte) (sample * env * amplitude * 127);
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip createNoiseClip(int ms, float amplitude) throws Exception {
        int sampleRate = 44100;
        int numSamples = (sampleRate * ms) / 1000;
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double sample = (Math.random() * 2.0 - 1.0);
            double env = 1.0 - ((double) i / numSamples);
            buffer[i] = (byte) (sample * env * amplitude * 127);
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip createWaterClip(int ms, float amplitude) throws Exception {
        int sampleRate = 44100;
        int numSamples = (sampleRate * ms) / 1000;
        byte[] buffer = new byte[numSamples];
        double filter = 0;
        for (int i = 0; i < numSamples; i++) {
            double noise = (Math.random() * 2.0 - 1.0);
            filter += (noise - filter) * 0.15; // Low-pass filter for water bubble
            double env = Math.sin(Math.PI * ((double) i / numSamples));
            buffer[i] = (byte) (filter * env * amplitude * 127);
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip createArpeggioClip(int[] freqs, int noteDurationMs, float amplitude) throws Exception {
        int sampleRate = 44100;
        int totalSamples = (sampleRate * noteDurationMs * freqs.length) / 1000;
        byte[] buffer = new byte[totalSamples];
        int noteSamples = (sampleRate * noteDurationMs) / 1000;
        for (int n = 0; n < freqs.length; n++) {
            int freq = freqs[n];
            for (int i = 0; i < noteSamples; i++) {
                int index = n * noteSamples + i;
                if (index >= totalSamples) break;
                double t = (double) i / sampleRate;
                double sample = Math.sin(2.0 * Math.PI * freq * t);
                double env = 1.0 - ((double) i / noteSamples);
                buffer[index] = (byte) (sample * env * amplitude * 127);
            }
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip createFarmBgmClip() throws Exception {
        // Peaceful acoustic chord progression (C - G - Am - F) in 44100Hz
        int sampleRate = 22050; // Use 22kHz to keep buffer compact and smooth
        int[] chords = {
            261, 329, 392, 523, // C major
            246, 293, 392, 493, // G major
            220, 261, 329, 440, // A minor
            220, 261, 349, 440  // F major
        };
        int noteMs = 280;
        int totalNotes = chords.length * 2;
        int totalSamples = (sampleRate * noteMs * totalNotes) / 1000;
        byte[] buffer = new byte[totalSamples];
        int noteSamples = (sampleRate * noteMs) / 1000;

        for (int n = 0; n < totalNotes; n++) {
            int freq = chords[n % chords.length];
            for (int i = 0; i < noteSamples; i++) {
                int index = n * noteSamples + i;
                if (index >= totalSamples) break;
                double t = (double) i / sampleRate;
                // Soft sine wave with warm 2nd harmonic
                double sample = 0.7 * Math.sin(2.0 * Math.PI * freq * t) + 0.3 * Math.sin(4.0 * Math.PI * freq * t);
                double env = Math.exp(-3.0 * ((double) i / noteSamples));
                buffer[index] = (byte) (sample * env * 0.25f * 127);
            }
        }
        return getClipFromBytes(buffer, sampleRate);
    }

    private Clip getClipFromBytes(byte[] buffer, int sampleRate) throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        AudioInputStream ais = new AudioInputStream(bais, format, buffer.length);
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        return clip;
    }

    // ==========================================
    // GETTERS
    // ==========================================
    public BufferedImage getBaseFrame(Action action, int frameIndex) {
        BufferedImage[] frames = baseAnims.get(action);
        if (frames != null && frames.length > 0) {
            return frames[frameIndex % frames.length];
        }
        return baseSingle;
    }

    public BufferedImage getHairFrame(String hairStyle, Action action, int frameIndex) {
        Map<Action, BufferedImage[]> hairMap = hairAnims.get(hairStyle);
        if (hairMap != null) {
            BufferedImage[] frames = hairMap.get(action);
            if (frames != null && frames.length > 0) {
                return frames[frameIndex % frames.length];
            }
        }
        return null;
    }

    public BufferedImage getToolFrame(Action action, int frameIndex) {
        BufferedImage[] frames = toolAnims.get(action);
        if (frames != null && frames.length > 0) {
            return frames[frameIndex % frames.length];
        }
        return null;
    }

    public BufferedImage getBaseSingle() { return baseSingle; }

    public BufferedImage getMenuButton(int row, int col) {
        if (row >= 0 && row < 4 && col >= 0 && col < 3) {
            return menuButtons[row][col];
        }
        return null;
    }

    public BufferedImage getStatBars() { return statBars; }
    public BufferedImage getMenuBoard() { return menuBoard; }
    public BufferedImage getOptionsBoard() { return optionsBoard; }
    public BufferedImage getPauseBoard() { return pauseBoard; }

    public BufferedImage getAudioToggle(int row, int col) {
        if (row >= 0 && row < 2 && col >= 0 && col < 3) {
            return audioToggle[row][col];
        }
        return null;
    }

    public BufferedImage getNavButton(int row, int col) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            return navButtons[row][col];
        }
        return null;
    }

    public BufferedImage getVolumeSlider() { return volumeSlider; }
    public BufferedImage getTitleBg() { return titleBg; }
    public BufferedImage getExampleScene() { return exampleScene; }

    public BufferedImage getSoilDry() { return soilDry; }
    public BufferedImage getSoilWatered() { return soilWatered; }
    public BufferedImage getSoilDryAlt() { return soilDryAlt; }
    public BufferedImage getSoilWateredAlt() { return soilWateredAlt; }

    public BufferedImage getCropStageImage(HarvestedCrop.CropType type, int stage) {
        String key = type.name().toLowerCase() + "_" + Math.max(0, Math.min(5, stage));
        return cropSprites.get(key);
    }

    public BufferedImage[] getChickenFrames() { return chickenFrames; }
    public BufferedImage[] getCowFrames() { return cowFrames; }
    public BufferedImage[] getSheepFrames() { return sheepFrames; }
    public BufferedImage[] getPigFrames() { return pigFrames; }
    public BufferedImage[] getTreeFrames() { return treeFrames; }

    public BufferedImage getUiImage(String name) { return uiImages.get(name); }
    public BufferedImage[] getSceneryFrames(String name) { return scenery.get(name); }
    public BufferedImage getFarmhouse() { return farmhouse; }
    public BufferedImage getShippingCrate() { return shippingCrate; }
    public BufferedImage getShippingCrateTop() { return shippingCrateTop; }
    public BufferedImage getRockImage() { return rock; }
    public BufferedImage getCoracleLand() { return coracleLand; }
    public BufferedImage getBuilding(String name) { return buildings.get(name); }

    public void drawLabel(Graphics2D g, int x, int y, int width) {
        g.drawImage(uiImages.get("label_left"), x, y, 8, 26, null);
        g.drawImage(uiImages.get("label_middle"), x + 8, y, width - 16, 26, null);
        g.drawImage(uiImages.get("label_right"), x + width - 8, y, 8, 26, null);
    }

    public void drawPanel(Graphics2D g, int x, int y, int width, int height) {
        // Skip the baked-in MENU heading while retaining the original corner and frame pixels.
        int[] sx = {0, 30, 252, 282};
        int[] dx = {x, x + 30, x + width - 30, x + width};
        int[] dy = {y, y + 24, y + height - 30, y + height};
        int[] top = {0, 100, 376};
        int[] bottom = {24, 376, 406};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                g.drawImage(menuBoard, dx[col], dy[row], dx[col + 1], dy[row + 1],
                    sx[col], top[row], sx[col + 1], bottom[row], null);
            }
        }
    }

    public BufferedImage getTileset() { return tileset; }
}
