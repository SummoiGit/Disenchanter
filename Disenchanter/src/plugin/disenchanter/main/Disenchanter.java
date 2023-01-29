package plugin.disenchanter.main;

import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class Disenchanter extends JavaPlugin implements Listener {
	
	public static Disenchanter plugin;
    private Economy econ;

    @Override
    public void onEnable() {
    	
    	if (!setupEconomy()) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    	
    	plugin = this;
    	getConfig().options().copyDefaults();
    	saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        
    }
    
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	
        double disenchant_cost_money = Disenchanter.plugin.getConfig().getDouble("disenchant_cost_money");
        int disenchant_cost_exp = Disenchanter.plugin.getConfig().getInt("disenchant_cost_exp");
        String disenchant_message = Disenchanter.plugin.getConfig().getString("disenchant_message");
        String disenchant_messagetranslated = ChatColor.translateAlternateColorCodes('&', disenchant_message);
        
    	Economy economy = this.getEconomy();
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        
        if (inv.getType() == InventoryType.ANVIL) {
        	
        	if (event.getSlot() == 2) {
        		
        		ItemStack resultItem = inv.getItem(2);
                
                if (resultItem != null && resultItem.getType() == Material.ENCHANTED_BOOK) {
                    
                    if (player.getLevel() >= disenchant_cost_exp) {
                    	
                    	event.setCancelled(true);
                        event.getWhoClicked().getInventory().addItem(resultItem);
                        inv.setItem(2, null);
                        
                        ItemStack item = inv.getItem(1);
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) resultItem.getItemMeta();
                        Map<Enchantment, Integer> storedEnchants = meta.getStoredEnchants();
                        
                        for(Enchantment e : storedEnchants.keySet()) {
                            item.removeEnchantment(e);
                            player.sendMessage(disenchant_messagetranslated);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 10, 29);
                        }
                        int newLevel = player.getLevel() - disenchant_cost_exp;
                        player.setLevel(newLevel);
                    	economy.withdrawPlayer((OfflinePlayer) event.getWhoClicked(), disenchant_cost_money);
                    	inv.setItem(0, null);
                    } else {
                    	
                        player.sendMessage(ChatColor.RED + "You cannot afford this");
                    }
                    
                }
        		
        	}
        }
    }
    
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
    	
        ItemStack item1 = event.getInventory().getItem(0);
        ItemStack item2 = event.getInventory().getItem(1);
        
        if (item1 != null && item1.getType() == Material.WRITABLE_BOOK 
            && item2 != null && item2.getEnchantments().size() > 0) {
            Map<Enchantment, Integer> enchants = item2.getEnchantments();
            
            if (enchants.isEmpty()) {
                return;
            }
            
            Enchantment enchant = (Enchantment) enchants.keySet().toArray()[new Random().nextInt(enchants.size())];
            int level = enchants.get(enchant);
            ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
            meta.addStoredEnchant(enchant, level, true);
            enchantedBook.setItemMeta(meta);
            event.setResult(enchantedBook);
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getInventory().setRepairCost(Disenchanter.plugin.getConfig().getInt("disenchant_cost_exp")));
            
        }
    }
    
}
