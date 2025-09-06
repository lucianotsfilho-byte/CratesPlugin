package br.com.luciano.crateplugin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Crateplugin extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CratePlugin ativado!");
    }

    @EventHandler
    public void onCrateClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasBlock()) return;

        for (String crateName : config.getConfigurationSection("crates").getKeys(false)) {
            String path = "crates." + crateName;
            double x = config.getDouble(path + ".location.x");
            double y = config.getDouble(path + ".location.y");
            double z = config.getDouble(path + ".location.z");
            String world = config.getString(path + ".location.world");
            String keyName = config.getString(path + ".key");

            if (event.getClickedBlock().getLocation().getWorld().getName().equals(world) &&
                    event.getClickedBlock().getLocation().getBlockX() == (int) x &&
                    event.getClickedBlock().getLocation().getBlockY() == (int) y &&
                    event.getClickedBlock().getLocation().getBlockZ() == (int) z) {

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.TRIPWIRE_HOOK) {
                    player.sendMessage("§cVocê precisa de uma chave para abrir esta crate!");
                    return;
                }

                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) return;
                if (!meta.getDisplayName().equals(keyName)) {
                    player.sendMessage("§cChave incorreta!");
                    return;
                }

                item.setAmount(item.getAmount() - 1);

                Inventory fakeInv = Bukkit.createInventory(null, 9, "§6Abrindo Caixa...");
                player.openInventory(fakeInv);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Location loc = player.getLocation().add(0, 1, 0);


                    int particles = 20;
                    double radius = 2;
                    for (int i = 0; i < particles; i++) {
                        double angle = 2 * Math.PI * i / particles;
                        double Px = radius * Math.cos(angle);
                        double Pz = radius * Math.sin(angle);
                        player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(Px, 0, Pz), 1, 0, 0, 0, 0);
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);


                    ConfigurationSection rewardSection = config.getConfigurationSection(path + ".rewards");
                    if (rewardSection == null) return;

                    List<String> items = new ArrayList<>();
                    for (String itemName : rewardSection.getKeys(false)) {
                        int chance = rewardSection.getInt(itemName + ".chance");
                        for (int i = 0; i < chance; i++) {
                            items.add(itemName);
                        }
                    }

                    if (items.isEmpty()) return;
                    String rewardName = items.get(random.nextInt(items.size()));
                    Material rewardMat = Material.getMaterial(rewardName.toUpperCase());
                    if (rewardMat != null) {
                        player.getInventory().addItem(new ItemStack(rewardMat, 1));
                        player.sendTitle("§a§lVocê recebeu: §a", "§a" + rewardName);


                        Firework fw = player.getWorld().spawn(loc, Firework.class);
                        FireworkMeta fwMeta = fw.getFireworkMeta();
                        fwMeta.addEffect(FireworkEffect.builder()
                                .withColor(Color.RED)
                                .withColor(Color.ORANGE)
                                .with(FireworkEffect.Type.BALL)
                                .withTrail()
                                .withFlicker()
                                .build());
                        fwMeta.setPower(1);
                        fw.setFireworkMeta(fwMeta);
                    }

                }, 20L);

                break;
            }
        }
    }
}
