package info.n00bs.adblock_2b2t.client.favorites;

import info.n00bs.adblock_2b2t.client.config.FilterConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages the favorites list - a list of usernames whose messages should be highlighted.
 */
public class FavoritesManager {
    private static final FavoritesManager INSTANCE = new FavoritesManager();
    public static final String FAVORITES_FILENAME = "favorites.txt";

    private final List<String> favorites = new CopyOnWriteArrayList<>();
    private volatile boolean isInitialized = false;

    private FavoritesManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of the FavoritesManager.
     * @return The FavoritesManager instance
     */
    public static FavoritesManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the favorites manager by loading favorites from file.
     */
    public void initialize() {
        loadFavorites();
        isInitialized = true;
    }

    /**
     * Loads favorites from the favorites file.
     */
    public void loadFavorites() {
        favorites.clear();

        FilterConfig config = FilterConfig.getInstance();
        String filtersDir = config.getFiltersDirectory();
        Path filePath = Paths.get(filtersDir, FAVORITES_FILENAME);

        try {
            if (!Files.exists(filePath)) {
                // Create the file with a header
                Files.writeString(filePath, "# Favorites List\n" +
                        "# One username per line. Lines starting with # are comments.\n" +
                        "# Messages from these users will be highlighted in chat.\n");
            } else {
                List<String> lines = Files.readAllLines(filePath);
                List<String> usernames = lines.stream()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList());
                favorites.addAll(usernames);
                System.out.println("Loaded " + favorites.size() + " favorites");
            }
        } catch (IOException e) {
            System.err.println("Failed to load favorites file: " + e.getMessage());
        }
    }

    /**
     * Saves favorites to the favorites file.
     */
    public void saveFavorites() {
        FilterConfig config = FilterConfig.getInstance();
        String filtersDir = config.getFiltersDirectory();
        Path filePath = Paths.get(filtersDir, FAVORITES_FILENAME);

        try {
            List<String> lines = new ArrayList<>();
            lines.add("# Favorites List");
            lines.add("# One username per line. Lines starting with # are comments.");
            lines.add("# Messages from these users will be highlighted in chat.");
            lines.addAll(favorites);

            Files.write(filePath, lines);
            System.out.println("Saved " + favorites.size() + " favorites");
        } catch (IOException e) {
            System.err.println("Failed to save favorites file: " + e.getMessage());
        }
    }

    /**
     * Adds a username to the favorites list.
     * @param username The username to add
     * @return true if added, false if already exists
     */
    public boolean addFavorite(String username) {
        if (!isInitialized) {
            initialize();
        }

        String normalizedUsername = username.trim();
        if (normalizedUsername.isEmpty()) {
            return false;
        }

        // Check if already exists (case-insensitive)
        boolean exists = favorites.stream()
                .anyMatch(fav -> fav.equalsIgnoreCase(normalizedUsername));

        if (!exists) {
            favorites.add(normalizedUsername);
            saveFavorites();
            return true;
        }
        return false;
    }

    /**
     * Removes a username from the favorites list.
     * @param username The username to remove
     * @return true if removed, false if not found
     */
    public boolean removeFavorite(String username) {
        if (!isInitialized) {
            initialize();
        }

        String normalizedUsername = username.trim();
        boolean removed = favorites.removeIf(fav -> fav.equalsIgnoreCase(normalizedUsername));

        if (removed) {
            saveFavorites();
        }
        return removed;
    }

    /**
     * Checks if a username is in the favorites list.
     * @param username The username to check
     * @return true if the username is a favorite
     */
    public boolean isFavorite(String username) {
        if (!isInitialized) {
            initialize();
        }

        String normalizedUsername = username.trim();
        return favorites.stream()
                .anyMatch(fav -> fav.equalsIgnoreCase(normalizedUsername));
    }

    /**
     * Gets a copy of the favorites list.
     * @return A list of favorite usernames
     */
    public List<String> getFavorites() {
        if (!isInitialized) {
            initialize();
        }
        return new ArrayList<>(favorites);
    }

    /**
     * Clears all favorites.
     */
    public void clearFavorites() {
        favorites.clear();
        saveFavorites();
    }

    /**
     * Extracts the username from a chat message.
     * Supports common Minecraft chat formats like "<username>", "[username]", "username:", etc.
     * @param message The chat message
     * @return The extracted username, or null if no username found
     */
    public String extractUsername(String message) {
        // Try to match common chat formats
        // Format: <username> message
        if (message.matches("^<[^>]+>.*")) {
            int endIndex = message.indexOf('>');
            return message.substring(1, endIndex);
        }

        // Format: [username] message
        if (message.matches("^\\[[^\\]]+\\].*")) {
            int endIndex = message.indexOf(']');
            return message.substring(1, endIndex);
        }

        // Format: username: message
        if (message.matches("^[^:\\s]+:.*")) {
            int colonIndex = message.indexOf(':');
            return message.substring(0, colonIndex);
        }

        // Format: username » message (common in some server plugins)
        if (message.contains("»")) {
            int separatorIndex = message.indexOf("»");
            String potential = message.substring(0, separatorIndex).trim();
            // Remove any prefixes like [Rank]
            if (potential.contains("]")) {
                potential = potential.substring(potential.lastIndexOf("]") + 1).trim();
            }
            if (!potential.isEmpty() && !potential.contains(" ")) {
                return potential;
            }
        }

        return null;
    }
}
