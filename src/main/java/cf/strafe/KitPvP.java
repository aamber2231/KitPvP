package cf.strafe;

import cf.strafe.command.*;
import cf.strafe.config.Config;
import cf.strafe.data.DataManager;
import cf.strafe.event.EventManager;
import cf.strafe.event.map.MapManager;
import cf.strafe.gui.KitGui;
import cf.strafe.kit.KitManager;
import cf.strafe.listener.DataListener;
import cf.strafe.listener.PlayerListener;
import cf.strafe.managers.BroadcastManager;
import cf.strafe.managers.ScoreboardManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public enum KitPvP {
    INSTANCE;


    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private KitManager kitManager;
    private BroadcastManager broadcastManager;
    private DataManager dataManager;
    private ScoreboardManager scoreboardManager;
    private Scoreboard teamManager;
    private EventManager eventManager;
    private Main plugin;

    /**
     * Called when the plugin is loaded
     */
    public void onLoad(Main plugin) {
        final File f = new File(plugin.getDataFolder(), "config.yml");
        if (!f.exists()) {
            plugin.saveResource("config.yml", true);

        }
        this.plugin = plugin;
    }


    /**
     * Called when the plugin is enabled
     */
    public void onEnable(Main plugin) {
        Config.INSTANCE.loadConfig();
        this.dataManager = new DataManager();
        this.kitManager = new KitManager();
        this.scoreboardManager = new ScoreboardManager();
        this.broadcastManager = new BroadcastManager();
        this.eventManager = new EventManager();
        scoreboard(plugin);
        handleBukkit(plugin);
        MapManager.loadArenas();
        Bukkit.getOnlinePlayers().forEach(player -> dataManager.inject(player));
        teamManager = Bukkit.getScoreboardManager().getMainScoreboard();
        registerHealthBar();
        registerNameTag();

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                            Sound sound = event.getPacket().getSoundEffects().read(0);
                            if (sound.name().toLowerCase().contains("entity_player_attack_strong") ||
                                    sound.name().toLowerCase().contains("entity_player_attack_sweep") ||
                                    sound.name().toLowerCase().contains("entity_player_attack_nodamage") ||
                                    sound.name().toLowerCase().contains("entity_player_attack_knockback") ||
                                    sound.name().toLowerCase().contains("entity_player_attack_crit") ||
                                    sound.name().toLowerCase().contains("entity_player_attack_weak")) {
                                event.setCancelled(true); //The sound will no longer be played
                            }
                        }
                        if (event.getPacketType() == PacketType.Play.Server.WORLD_PARTICLES) {
                            Particle particle = event.getPacket().getNewParticles().read(0).getParticle();
                            Bukkit.broadcast(particle.name(), "wizardpro.core");
                            if (particle == Particle.DAMAGE_INDICATOR || particle == Particle.SWEEP_ATTACK) {
                                event.setCancelled(true);
                            }
                        }
                    }
                });


    }

    public void registerNameTag() {
        if (teamManager.getTeam("vanish") != null) {
            teamManager.getTeam("vanish").unregister();
        }
        Team t = teamManager.registerNewTeam("vanish");
        t.setPrefix(ChatColor.GREEN + "[V] ");
    }

    public void registerHealthBar() {
        if (teamManager.getObjective("health") != null) {
            teamManager.getObjective("health").unregister();
        }
        Objective o = teamManager.registerNewObjective("health", "health");
        o.setDisplayName(ChatColor.RED + "❤");
        o.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    public void scoreboard(Main plugin) {

        for (Player p : Bukkit.getOnlinePlayers()) {
            dataManager.inject(p);
        }
    }

    /**
     * Called when the plugin is disabled
     */
    public void onDisable(Main plugin) {
        Bukkit.getOnlinePlayers().forEach(player -> dataManager.uninject(player));
        MapManager.saveArenas();
    }

    public void handleBukkit(Main plugin) {
        plugin.getServer().getPluginManager().registerEvents(new KitGui(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DataListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(), plugin);
        plugin.getCommand("kit").setExecutor(new KitCommand());
        plugin.getCommand("discord").setExecutor(new DiscordCommand());
        plugin.getCommand("report").setExecutor(new ReportCommand());
        plugin.getCommand("vanish").setExecutor(new VanishCommand());
        plugin.getCommand("staffchat").setExecutor(new StaffChatCommand());
        plugin.getCommand("givelevel").setExecutor(new GiveLevelCommand());
        plugin.getCommand("stats").setExecutor(new StatsCommand());
        plugin.getCommand("sumo").setExecutor(new SumoCommand());
        plugin.getCommand("event").setExecutor(new EventCommand());
        plugin.getCommand("ffa").setExecutor(new FFACommand());
        plugin.getCommand("spawn").setExecutor(new SpawnCommand());
    }


}
