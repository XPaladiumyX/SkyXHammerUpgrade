package skyxnetwork.skyXHammerUpgrade;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyXHammerUpgrade extends JavaPlugin implements Listener {

    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        debug = config.getBoolean("debug", true);

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SkyXHammerUpgrade enabled. Debug mode = " + debug);
    }

    public void debug(String msg) {
        if (debug) {
            getLogger().info("[DEBUG] " + msg);
        }
    }

    /* =======================================================
       FIRST PASS : PrepareItemCraftEvent
       (detect pattern + set provisional result)
       ======================================================= */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        Player player = (Player) event.getView().getPlayer();

        debug("---- PrepareItemCraftEvent triggered ----");
        debug("Player: " + player.getName());

        // CHECK PERMISSION
        if (!player.hasPermission("ia.user.craft.skyxnetworkblocks.ruby_hammer2craft")) {
            debug("Player does NOT have permission: ia.user.craft.skyxnetworkblocks.ruby_hammer2craft");
            return;
        }
        debug("Player has correct permission for hammer tier 2 upgrade.");

        ItemStack[] matrix = inv.getMatrix();

        debug("Checking crafting matrix...");
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            debug("Slot " + i + ": " + (item == null ? "null" : item.getType().toString()));
        }

        debug("Validating pattern C C C / C B C / C C C");

        // TOP ROW
        if (!checkSlot(matrix, 0, "ruby_block")) return;
        if (!checkSlot(matrix, 1, "ruby_block")) return;
        if (!checkSlot(matrix, 2, "ruby_block")) return;

        // MIDDLE ROW
        if (!checkSlot(matrix, 3, "ruby_block")) return;
        if (!checkSlot(matrix, 4, "ruby_hammer")) return;
        if (!checkSlot(matrix, 5, "ruby_block")) return;

        // BOTTOM ROW
        if (!checkSlot(matrix, 6, "ruby_block")) return;
        if (!checkSlot(matrix, 7, "ruby_block")) return;
        if (!checkSlot(matrix, 8, "ruby_block")) return;

        debug("Pattern matched successfully.");

        debug("Attempting to load ItemsAdder item rubyitems:ruby_hammer2");
        CustomStack hammer2 = CustomStack.getInstance("rubyitems:ruby_hammer2");

        if (hammer2 == null) {
            debug("ERROR: ItemsAdder item rubyitems:ruby_hammer2 DOES NOT EXIST!");
            return;
        }

        debug("ItemsAdder item exists. Setting craft result to hammer tier 2...");
        inv.setResult(hammer2.getItemStack().clone());

        debug("Craft override completed successfully.");
        debug("----------------------------------------");
    }

    /* =======================================================
       SECOND PASS : CraftItemEvent
       (final override → stops ItemsAdder fallback)
       ======================================================= */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {

        Player player = (Player) event.getWhoClicked();
        debug("---- CraftItemEvent triggered ----");
        debug("Player: " + player.getName());

        if (!player.hasPermission("ia.user.craft.skyxnetworkblocks.ruby_hammer2craft")) {
            debug("Player does NOT have permission during CraftItemEvent.");
            return;
        }

        ItemStack result = event.getInventory().getResult();
        if (result == null) {
            debug("CraftItemEvent: result = null → stopping.");
            return;
        }

        CustomStack cs = CustomStack.byItemStack(result);
        if (cs != null) {
            debug("CraftItemEvent initial result IA ID: " + cs.getNamespacedID());
        } else {
            debug("CraftItemEvent initial result is NOT a CustomStack.");
        }

        // Re-check hammer slot because IA may alter result
        ItemStack hammerSlot = event.getInventory().getMatrix()[4];
        CustomStack hammerCs = CustomStack.byItemStack(hammerSlot);

        if (hammerCs == null || !hammerCs.getNamespacedID().equals("rubyitems:ruby_hammer")) {
            debug("CraftItemEvent: middle slot is NOT ruby_hammer → abort forced result.");
            return;
        }

        debug("CraftItemEvent: Overriding final craft result to rubyitems:ruby_hammer2");

        CustomStack hammer2 = CustomStack.getInstance("rubyitems:ruby_hammer2");
        if (hammer2 == null) {
            debug("CraftItemEvent: ERROR → rubyitems:ruby_hammer2 does not exist.");
            return;
        }

        event.setCurrentItem(hammer2.getItemStack().clone());
        debug("CraftItemEvent: Final result overridden successfully.");
    }

    /* =======================================================
       Utility : check itemsadder ID in each slot
       ======================================================= */
    private boolean checkSlot(ItemStack[] matrix, int slot, String type) {
        ItemStack stack = matrix[slot];

        debug("Checking slot " + slot + " for " + type + "...");

        if (stack == null || stack.getType() == Material.AIR) {
            debug("Slot " + slot + " is empty → pattern failed.");
            return false;
        }

        CustomStack cs = CustomStack.byItemStack(stack);

        if (cs == null) {
            debug("Slot " + slot + " is NOT a CustomStack → type mismatch.");
            return false;
        }

        debug("Slot " + slot + " ItemsAdder ID: " + cs.getNamespacedID());

        switch (type) {
            case "ruby_block":
                if (cs.getNamespacedID().equals("skyxnetworkblocks:ruby_block")) {
                    debug("Slot " + slot + " matches ruby_block.");
                    return true;
                }
                debug("Slot " + slot + " does NOT match ruby_block.");
                return false;

            case "ruby_hammer":
                if (cs.getNamespacedID().equals("rubyitems:ruby_hammer")) {
                    debug("Slot " + slot + " matches ruby_hammer.");
                    return true;
                }
                debug("Slot " + slot + " does NOT match ruby_hammer.");
                return false;
        }

        debug("Unknown type passed to checkSlot()");
        return false;
    }
}