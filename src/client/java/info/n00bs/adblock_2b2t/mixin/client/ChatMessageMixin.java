package info.n00bs.adblock_2b2t.mixin.client;

import info.n00bs.adblock_2b2t.client.config.FilterConfig;
import info.n00bs.adblock_2b2t.client.favorites.FavoritesManager;
import info.n00bs.adblock_2b2t.client.filter.MessageFilter;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mixin to intercept and filter chat messages.
 */
@Mixin(ChatHud.class)
public class ChatMessageMixin {
    // ThreadLocal to prevent recursive mixin invocations
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    /**
     * Injects into the addMessage method to filter chat messages.
     * 
     * @param message The chat message text
     * @param signature The message signature data
     * @param indicator The message indicator
     * @param ci The callback info
     */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", 
            at = @At("HEAD"), 
            cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        // Prevent recursive calls
        if (PROCESSING.get()) {
            return;
        }

        // Convert the Text to a string
        String messageString = message.getString();

        // Colorize usernames if highlighting is enabled
        FilterConfig config = FilterConfig.getInstance();
        if (config.isHighlightFavorites()) {
            String username = FavoritesManager.getInstance().extractUsername(messageString);
            if (username != null) {
                boolean isFavorite = FavoritesManager.getInstance().isFavorite(username);
                Text colorizedMessage = colorizeUsername(messageString, username, isFavorite);

                if (colorizedMessage != null) {
                    // Replace the original message with colorized version
                    try {
                        PROCESSING.set(true);
                        ((ChatHud)(Object)this).addMessage(colorizedMessage, signature, indicator);
                    } finally {
                        PROCESSING.set(false);
                    }
                    ci.cancel();
                    return;
                }
            }
        }

        // Check if the message should be filtered
        if (MessageFilter.getInstance().shouldFilterMessage(messageString)) {
            // If debug mode is enabled, show a notification with the blocked message on hover
            if (config.isDebugMode()) {
                // Get the filter type that matched this message
                String filterType = MessageFilter.getInstance().getMatchingFilterType(messageString);
                String filterName = filterType != null ? 
                    (filterType.equals("CUSTOM") ? "Custom Filter" : "Remote Filter") : 
                    "Unknown Filter";

                // Create a hover event with the original message
                HoverEvent hoverEvent = new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Filter: " + filterName + "\n").formatted(Formatting.GOLD)
                        .append(Text.literal("expression: " + MessageFilter.getInstance().getMatchingPattern(messageString) + "\n").formatted(Formatting.GOLD))
                        .append(Text.literal("Blocked message: ").formatted(Formatting.RED))
                        .append(Text.literal(messageString).formatted(Formatting.WHITE))
                );

                // Create the debug message with hover effect
                Text debugMessage = Text.literal("[AdBlock] ").formatted(Formatting.DARK_RED)
                    .append(Text.literal("Message blocked").formatted(Formatting.RED))
                    .setStyle(Style.EMPTY.withHoverEvent(hoverEvent));

                // Replace the original message with our debug message
                try {
                    PROCESSING.set(true);
                    ((ChatHud)(Object)this).addMessage(debugMessage, null, null);
                } finally {
                    PROCESSING.set(false);
                }
            }
            System.out.println("[AdBlock] Message blocked: \n" +
                    messageString + "\n" +
                    "Filter: " + MessageFilter.getInstance().getMatchingFilterType(messageString) + "\n" +
                    "Pattern: " + MessageFilter.getInstance().getMatchingPattern(messageString) );

            // Cancel the event to prevent the original message from being displayed
            ci.cancel();
        }
    }

    /**
     * Colorizes the username in a chat message.
     * Regular usernames are colored in aqua, favorite usernames are colored in gold with a star prefix.
     * 
     * @param messageString The original message string
     * @param username The username to colorize
     * @param isFavorite Whether the user is a favorite
     * @return The colorized message, or null if unable to parse
     */
    private Text colorizeUsername(String messageString, String username, boolean isFavorite) {
        MutableText result = Text.literal("");

        // Try to match and colorize different chat formats

        // Format: <username> message
        Pattern pattern1 = Pattern.compile("^(<)([^>]+)(>\\s*.*)$");
        Matcher matcher1 = pattern1.matcher(messageString);
        if (matcher1.matches()) {
            result.append(Text.literal(matcher1.group(1)).formatted(Formatting.GRAY)); // < in gray

            if (isFavorite) {
                result.append(Text.literal("★").formatted(Formatting.GOLD, Formatting.BOLD)); // Star for favorites
                result.append(Text.literal(matcher1.group(2)).formatted(Formatting.GOLD, Formatting.BOLD)); // Username in gold
            } else {
                result.append(Text.literal(matcher1.group(2)).formatted(Formatting.AQUA)); // Username in aqua
            }

            result.append(Text.literal(matcher1.group(3)).formatted(Formatting.GRAY)) // > and rest in gray
                    .append(Text.literal(messageString.substring(matcher1.group(0).length())).formatted(Formatting.WHITE));
            return result;
        }

        // Format: [username] message
        Pattern pattern2 = Pattern.compile("^(\\[)([^\\]]+)(\\]\\s*.*)$");
        Matcher matcher2 = pattern2.matcher(messageString);
        if (matcher2.matches()) {
            result.append(Text.literal(matcher2.group(1)).formatted(Formatting.GRAY)); // [ in gray

            if (isFavorite) {
                result.append(Text.literal("★").formatted(Formatting.GOLD, Formatting.BOLD));
                result.append(Text.literal(matcher2.group(2)).formatted(Formatting.GOLD, Formatting.BOLD));
            } else {
                result.append(Text.literal(matcher2.group(2)).formatted(Formatting.AQUA));
            }

            result.append(Text.literal(matcher2.group(3)).formatted(Formatting.GRAY))
                    .append(Text.literal(messageString.substring(matcher2.group(0).length())).formatted(Formatting.WHITE));
            return result;
        }

        // Format: username: message
        Pattern pattern3 = Pattern.compile("^([^:\\s]+)(:\\s*.*)$");
        Matcher matcher3 = pattern3.matcher(messageString);
        if (matcher3.matches() && matcher3.group(1).equals(username)) {
            if (isFavorite) {
                result.append(Text.literal("★").formatted(Formatting.GOLD, Formatting.BOLD));
                result.append(Text.literal(matcher3.group(1)).formatted(Formatting.GOLD, Formatting.BOLD));
            } else {
                result.append(Text.literal(matcher3.group(1)).formatted(Formatting.AQUA));
            }

            result.append(Text.literal(matcher3.group(2)).formatted(Formatting.WHITE));
            return result;
        }

        // Format: username » message or similar (common in server plugins)
        if (messageString.contains("»")) {
            int separatorIndex = messageString.indexOf("»");
            String beforeSeparator = messageString.substring(0, separatorIndex);
            String afterSeparator = messageString.substring(separatorIndex);

            // Check if username is in the before part
            if (beforeSeparator.contains(username)) {
                // Handle potential prefixes like [Rank] username
                int usernameIndex = beforeSeparator.lastIndexOf(username);
                String prefix = beforeSeparator.substring(0, usernameIndex);

                if (!prefix.isEmpty()) {
                    result.append(Text.literal(prefix).formatted(Formatting.GRAY));
                }

                if (isFavorite) {
                    result.append(Text.literal("★").formatted(Formatting.GOLD, Formatting.BOLD));
                    result.append(Text.literal(username).formatted(Formatting.GOLD, Formatting.BOLD));
                } else {
                    result.append(Text.literal(username).formatted(Formatting.AQUA));
                }

                result.append(Text.literal(afterSeparator).formatted(Formatting.WHITE));
                return result;
            }
        }

        return null; // Unable to parse format
    }
}