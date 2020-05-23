/*
 * SklepMC Plugin
 * Copyright (C) 2019 SklepMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package eu.okaeri.aicensor.minecraft.bukkit;

import eu.okaeri.aicensor.client.CensorApiContext;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CensorBukkitPlugin extends JavaPlugin {

    public static final String PERM_BYPASS = "aicensor.bypass";
    public static final String PERM_NOTIFY = "aicensor.notify";

    private final Map<UUID, Integer> currentSteps = new ConcurrentHashMap<>();

    private AiCensorBukkit censor;
    private CensorApiContext context;

    public CensorApiContext getContext() {
        return this.context;
    }

    public AiCensorBukkit getCensor() {
        return this.censor;
    }

    @Override
    public void onEnable() {

        // save default configuration if config.yml does not exists
        this.saveDefaultConfig();

        // validate configuration and create ApiContext
        FileConfiguration config = this.getConfig();
        String token = config.getString("token");

        if ((token == null) || "".equals(token)) {
            this.getLogger().log(Level.SEVERE, "Nie znaleziono poprawnie ustawionej wartosci 'token' w config.yml," +
                    " nalezy ja ustawic i zrestartowac serwer.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // create context
        this.context = new CensorApiContext(token);

        // create censor
        this.censor = new AiCensorBukkit(this);

        // custom api url
        String apiUrl = this.getConfig().getString("api-url");
        if (apiUrl != null) {
            this.context.setMainUrl(apiUrl);
        }

        // listeners
        this.getServer().getPluginManager().registerEvents(new AiCensorListener(this), this);
    }

    // it is easy
    protected String message(String key, String... params) {
        String message = this.getConfig().getString("message-" + key);
        message = message.replace("{PREFIX}", this.getConfig().getString("message-prefix"));
        message = ChatColor.translateAlternateColorCodes('&', message);
        for (int i = 0; i < params.length; i++) {
            message = message.replace("{" + i + "}", params[i]);
        }
        return message;
    }

    // it is scary
    protected void messagePlayer(Player player, String key, String... params) {
        String message = this.message(key, params);
        if (message.isEmpty()) {
            return;
        }
        player.sendMessage(message);
    }

    // but it works
    protected void notifyAdmins(String key, String... params) {
        String message = this.message(key, params);
        if (message.isEmpty()) {
            return;
        }
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (!player.hasPermission(PERM_NOTIFY)) {
                continue;
            }
            player.sendMessage(message);
        }
    }

    protected void punish(Player player) {
        Runnable safeExecution = () -> {
            String command = this.getConfig().getString("punishment-command");
            if ((command == null) || "".equals(command)) {
                return;
            }
            int step = this.getAndIncreaseStep(player);
            String stepValue = this.getConfig().getString("punishment-steps." + step);
            if (stepValue == null) {
                stepValue = this.getConfig().getString("punishment-steps.default");
            }
            command = command.replace("{STEP}", stepValue);
            command = command.replace("{NAME}", player.getName());
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), command);
        };
        this.getLogger().info("Wykonywanie kary dla gracza: " + player.getName());
        this.getServer().getScheduler().runTask(this, safeExecution);
    }

    private int getAndIncreaseStep(Player player) {
        UUID uuid = player.getUniqueId();
        Integer current = this.currentSteps.get(uuid);
        if (current == null) {
            this.currentSteps.put(uuid, 1);
            return 1;
        }
        this.currentSteps.put(uuid, current + 1);
        return (current + 1);
    }
}
