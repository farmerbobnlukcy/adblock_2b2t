package info.n00bs.adblock_2b2t.mixin.client;

import info.n00bs.adblock_2b2t.client.config.FilterConfig;
import info.n00bs.adblock_2b2t.client.favorites.FavoritesManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to add right-click functionality in chat for ignoring users
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Check if right-click (button 1)
        if (button == 1) {
            Style style = client.inGameHud.getChatHud().getText(mouseX, mouseY);

            if (style != null) {
                String clickedText = style.getString();

                // Try to extract username from the clicked text
                String username = FavoritesManager.getInstance().extractUsername(clickedText);

                if (username != null && !username.isEmpty()) {
                    // Toggle ignore status
                    FilterConfig config = FilterConfig.getInstance();
                    if (!config.getIgnoredUsers().contains(username)) {
                        config.addIgnoredUser(username);
                        config.saveConfig();

                        // Send feedback message
                        if (client.player != null) {
                            client.player.sendMessage(
                                Text.literal("Added ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal(username).formatted(Formatting.RED))
                                    .append(Text.literal(" to ignored users list").formatted(Formatting.YELLOW)),
                                false
                            );
                        }
                    } else {
                        // User is already ignored, remove them
                        config.removeIgnoredUser(username);
                        config.saveConfig();

                        if (client.player != null) {
                            client.player.sendMessage(
                                Text.literal("Removed ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal(username).formatted(Formatting.GREEN))
                                    .append(Text.literal(" from ignored users list").formatted(Formatting.YELLOW)),
                                false
                            );
                        }
                    }

                    cir.setReturnValue(true);
                }
            }
        }
    }
}
