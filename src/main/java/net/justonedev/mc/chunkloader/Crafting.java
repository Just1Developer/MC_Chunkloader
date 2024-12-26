package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.List;

public class Crafting implements Listener {

    private final NamespacedKey RECIPE_KEY;
    public static ItemStack CHUNKLOADER_ITEM;

    public Crafting(Plugin plugin) {
        RECIPE_KEY = new NamespacedKey(plugin, "chunkloader.loaderRecipe");
        initialize();
    }

    private void initialize() {
        CHUNKLOADER_ITEM = new ItemStack(Plugin.MATERIAL);
        ItemMeta meta = CHUNKLOADER_ITEM.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName("§6Chunkloader");
        meta.setLore(List.of("§7Keeps the chunk it", "§7is in loaded forever."));
        CHUNKLOADER_ITEM.setItemMeta(meta);

        addCraftingRecipe();
    }

    private void addCraftingRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, CHUNKLOADER_ITEM);
        recipe.shape("ITI", "IMI", "IOI");
        recipe.setIngredient('T', Material.REDSTONE_TORCH);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('M', Material.MINECART);
        recipe.setCategory(CraftingBookCategory.REDSTONE);
        recipe.setGroup("Chunkloader");
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerLearnRecipe(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipe(RECIPE_KEY);
    }

    @EventHandler
    public void preventCrafting(PrepareItemCraftEvent e) {
        // Top to Bottom, Left to Right. Null if empty
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (!CHUNKLOADER_ITEM.isSimilar(item)) continue;
            e.getInventory().setResult(null);
        }
    }

    public static boolean isChunkloader(ItemStack item) {
        return CHUNKLOADER_ITEM.isSimilar(item);
    }

    public static ItemStack getItem() {
        return CHUNKLOADER_ITEM;
    }
}
