package com.qateralong.alcohol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("alcohol.json");
    private static Config current = new Config();

    public int permissionLevel = 2;
    public double speed = 1.0;
    public double decayPerMinute = 1.0;
    public double maxLevel = 10.0;
    public List<String> buriedBlocks = new ArrayList<>(List.of("#minecraft:dirt"));
    public List<Drink> drinks = new ArrayList<>();

    public static class Drink {
        public String id;
        public String name;
        public String color = "#C8A060";
        public List<String> lore = new ArrayList<>();
        public double strength = 1.0;
        public double agingMinutes = 10;
        public int output = 1;
        public List<String> requires = new ArrayList<>();
        public List<Ingredient> ingredients = new ArrayList<>();
        public List<Eff> effects = new ArrayList<>();
        public List<Fx> screenfx = new ArrayList<>();
    }

    public static class Ingredient {
        public String item;
        public String tag;
        public String potion;
        public String drink;
        public int count = 1;
        public String label = "";
    }

    public static class Eff {
        public String effect;
        public int seconds = 10;
        public int amplifier;
    }

    public static class Fx {
        public String effect;
        public float seconds = 20;
        public float intensity = 0.5F;
        public String color;
    }

    public static Config get() {
        return current;
    }

    public static void load() {
        Config loaded = null;
        try {
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    loaded = GSON.fromJson(reader, Config.class);
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to read alcohol.json", e);
        }
        if (loaded == null) {
            loaded = new Config();
            loaded.drinks = Defaults.drinks();
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(loaded, writer);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to write alcohol.json", e);
            }
        }
        current = loaded;
    }
}
