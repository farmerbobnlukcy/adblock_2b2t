package info.n00bs.adblock_2b2t.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for the 2b2t AdBlock mod.
 * Manages filter settings.
 */
public class FilterConfig {
    private static final String CONFIG_DIR = "config/adblock_2b2t";
    private static final String FILTERS_DIR = CONFIG_DIR + "/filters";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String remoteUrl = "https://2b2t.info/adblock_2b2t/filter.txt";
    private boolean useRemoteFilters = true;
    private boolean useCustomFilters = true;
    private boolean debugMode = false; // Debug mode to show blocked messages
    private boolean autoRefreshEnabled = true; // Auto refresh remote filters
    private int autoRefreshDelay = 5; // Auto refresh delay in minutes
    private boolean highlightFavorites = true; // Highlight messages from favorite users

    public FilterConfig() {
        // Create config directories if they don't exist
        createDirectories();
    }

    private void createDirectories() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            Path filtersPath = Paths.get(FILTERS_DIR);

            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
            }

            if (!Files.exists(filtersPath)) {
                Files.createDirectories(filtersPath);
            }

            // Create remote filters file if it doesn't exist
            Path remoteFile = Paths.get(FILTERS_DIR, FilterCategory.REMOTE_FILTERS_FILENAME);
            if (!Files.exists(remoteFile)) {
                Files.createFile(remoteFile);
                Files.writeString(remoteFile, "# " + FilterCategory.REMOTE_FILTERS_DISPLAY_NAME + "\n" +
                        "# One pattern per line. Lines starting with # are comments.\n" +
                        "# This file is automatically updated from the remote URL.\n");
            }

            // Create custom filters file if it doesn't exist
            Path customFile = Paths.get(FILTERS_DIR, FilterCategory.CUSTOM_FILTERS_FILENAME);
            if (!Files.exists(customFile)) {
                Files.createFile(customFile);
                Files.writeString(customFile, "# " + FilterCategory.CUSTOM_FILTERS_DISPLAY_NAME + "\n" +
                        "# One pattern per line. Lines starting with # are comments.\n" +
                        "# Example: buy.*items\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to create config directories: " + e.getMessage());
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        saveConfig();
    }

    public boolean isUseRemoteFilters() {
        return useRemoteFilters;
    }

    public void setUseRemoteFilters(boolean useRemoteFilters) {
        this.useRemoteFilters = useRemoteFilters;
        saveConfig();
    }

    public boolean isUseCustomFilters() {
        return useCustomFilters;
    }

    public void setUseCustomFilters(boolean useCustomFilters) {
        this.useCustomFilters = useCustomFilters;
        saveConfig();
    }

    public String getFiltersDirectory() {
        return FILTERS_DIR;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        saveConfig();
    }

    public boolean isAutoRefreshEnabled() {
        return autoRefreshEnabled;
    }

    public void setAutoRefreshEnabled(boolean autoRefreshEnabled) {
        this.autoRefreshEnabled = autoRefreshEnabled;
        saveConfig();
    }

    public int getAutoRefreshDelay() {
        return autoRefreshDelay;
    }

    public void setAutoRefreshDelay(int autoRefreshDelay) {
        if (autoRefreshDelay < 1) {
            autoRefreshDelay = 1; // Minimum 1 minute
        }
        this.autoRefreshDelay = autoRefreshDelay;
        saveConfig();
    }

    public boolean isHighlightFavorites() {
        return highlightFavorites;
    }

    public void setHighlightFavorites(boolean highlightFavorites) {
        this.highlightFavorites = highlightFavorites;
        saveConfig();
    }

    /**
     * Saves the current configuration to a JSON file.
     */
    public void saveConfig() {
        try {
            // Create config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Create JSON object with all settings
            JsonObject config = new JsonObject();
            config.addProperty("enabled", enabled);
            config.addProperty("remoteUrl", remoteUrl);
            config.addProperty("useRemoteFilters", useRemoteFilters);
            config.addProperty("useCustomFilters", useCustomFilters);
            config.addProperty("debugMode", debugMode);
            config.addProperty("autoRefreshEnabled", autoRefreshEnabled);
            config.addProperty("autoRefreshDelay", autoRefreshDelay);
            config.addProperty("highlightFavorites", highlightFavorites);

            // Write to file
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }

            System.out.println("AdBlock configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * Loads the configuration from a JSON file.
     * If the file doesn't exist or can't be read, default settings are used.
     */
    public void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            System.out.println("No configuration file found, using defaults");
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject config = GSON.fromJson(reader, JsonObject.class);

            // Load basic settings
            if (config.has("enabled")) {
                enabled = config.get("enabled").getAsBoolean();
            }
            if (config.has("remoteUrl")) {
                remoteUrl = config.get("remoteUrl").getAsString();
            }
            if (config.has("useRemoteFilters")) {
                useRemoteFilters = config.get("useRemoteFilters").getAsBoolean();
            }

            // Handle both old and new config formats
            if (config.has("useCustomFilters")) {
                useCustomFilters = config.get("useCustomFilters").getAsBoolean();
            } else if (config.has("useLocalFilters")) {
                useCustomFilters = config.get("useLocalFilters").getAsBoolean();
            }

            if (config.has("debugMode")) {
                debugMode = config.get("debugMode").getAsBoolean();
            }

            if (config.has("autoRefreshEnabled")) {
                autoRefreshEnabled = config.get("autoRefreshEnabled").getAsBoolean();
            }

            if (config.has("autoRefreshDelay")) {
                autoRefreshDelay = config.get("autoRefreshDelay").getAsInt();
                if (autoRefreshDelay < 1) {
                    autoRefreshDelay = 1; // Ensure minimum 1 minute
                }
            }

            if (config.has("highlightFavorites")) {
                highlightFavorites = config.get("highlightFavorites").getAsBoolean();
            }

            System.out.println("AdBlock configuration loaded from " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }

    public static FilterConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final FilterConfig INSTANCE = new FilterConfig();
    }
}
