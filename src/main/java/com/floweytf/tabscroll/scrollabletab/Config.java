package com.floweytf.tabscroll.scrollabletab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;

public class Config {
    @SerializedName("scrollbar-bg")
    public int scrollbarBg = 0xffffff;

    @SerializedName("scrollbar-fg")
    public int scrollbarFg = 0xaaaaaa;

    @SerializedName("max-rows")
    public int maxRows = 20;

    @SerializedName("scrollbar-width")
    public int scrollbarWidth = 3;

    @SerializedName("scrollbar-increment")
    public int scrollbarIncrement = 3;

    private static Config INSTANCE = null;

    public static Config getInstance() {
        return INSTANCE;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final File CONFIG = FabricLoader.getInstance().getConfigDir().resolve("scrollbar.json").toFile();

    public static void load() {
        try {
            INSTANCE = GSON.fromJson(new FileReader(CONFIG), Config.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load config: ", e);
            INSTANCE = new Config();
            INSTANCE.save();
        }
    }

    public void save() {
        try {
            Files.writeString(CONFIG.toPath(), GSON.toJson(this));
        } catch (Exception e) {
            LOGGER.error("Failed to save config: ", e);
        }
    }
}
