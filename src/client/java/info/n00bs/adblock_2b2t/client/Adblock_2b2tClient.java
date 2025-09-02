package info.n00bs.adblock_2b2t.client;

import info.n00bs.adblock_2b2t.client.command.FilterCommands;
import info.n00bs.adblock_2b2t.client.config.FilterConfig;
import info.n00bs.adblock_2b2t.client.favorites.FavoritesManager;
import info.n00bs.adblock_2b2t.client.filter.MessageFilter;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client initializer for the 2b2t AdBlock mod.
 */
public class Adblock_2b2tClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Load configuration
        FilterConfig.getInstance().loadConfig();

        // Register commands
        FilterCommands.registerCommands();

        // Initialize message filter
        MessageFilter.getInstance().initialize();

        // Initialize favorites manager
        FavoritesManager.getInstance().initialize();

        // Log initialization
        System.out.println("2b2t AdBlock mod initialized");

        // Schedule a task to show welcome message after client is fully loaded
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("2b2t AdBlock mod loaded. Type /adblock help for commands.")
                            .formatted(Formatting.GREEN), false);
                }
            });
        }
    }
}
