package eu.okaeri.aicensor.minecraft.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AiCensorListener implements Listener {

    private final CensorBukkitPlugin plugin;
    private final AiCensorBukkit censor;

    public AiCensorListener(CensorBukkitPlugin plugin) {
        this.plugin = plugin;
        this.censor = plugin.getCensor();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void handleChat(AsyncPlayerChatEvent event) {

        if (!this.plugin.getConfig().getBoolean("check-chat")) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!this.censor.shouldBeBlocked(message)) {
            return;
        }

        event.setCancelled(true);
        this.plugin.messagePlayer(player, "player-info");
        this.plugin.notifyAdmins("admin-info", player.getName(), message);
        this.plugin.punish(player);
    }
}
