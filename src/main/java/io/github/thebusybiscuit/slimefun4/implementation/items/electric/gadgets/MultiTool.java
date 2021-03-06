package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MultiTool extends SlimefunItem implements Rechargeable {

    private static final float COST = 0.3F;

    private final Map<UUID, Integer> selectedMode = new HashMap<>();
    private final List<MultiToolMode> modes = new ArrayList<>();
    private final float capacity;

    public MultiTool(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, float capacity, String... items) {
        super(category, item, recipeType, recipe);

        for (int i = 0; i < items.length; i++) {
            modes.add(new MultiToolMode(this, i, items[i]));
        }

        this.capacity = capacity;
    }

    @Override
    public float getMaxItemCharge(ItemStack item) {
        return capacity;
    }

    protected ItemUseHandler getItemUseHandler() {
        return e -> {
            Player p = e.getPlayer();
            ItemStack item = e.getItem();
            e.cancel();

            int index = selectedMode.getOrDefault(p.getUniqueId(), 0);

            if (!p.isSneaking()) {
                if (removeItemCharge(item, COST)) {
                    SlimefunItem sfItem = modes.get(index).getItem();

                    if (sfItem != null) {
                        sfItem.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(e));
                    }
                }
            }
            else {
                index = nextIndex(index);

                SlimefunItem selectedItem = modes.get(index).getItem();
                String itemName = selectedItem != null ? selectedItem.getItemName() : "Unknown";
                SlimefunPlugin.getLocalization().sendMessage(p, "messages.mode-change", true, msg -> msg.replace("%device%", "多功能工具").replace("%mode%", ChatColor.stripColor(itemName)));
                selectedMode.put(p.getUniqueId(), index);
            }
        };
    }

    private int nextIndex(int i) {
        int index = i;

        do {
            index++;

            if (index >= modes.size()) {
                index = 0;
            }
        }
        while (index != i && !modes.get(index).isEnabled());

        return index;
    }

    private BlockBreakHandler getBlockBreakHandler() {
        return (e, item, fortune, drops) -> {
            if (isItem(item)) {
                e.setCancelled(true);
                return true;
            }

            return false;
        };
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler(getItemUseHandler());
        addItemHandler(getBlockBreakHandler());
    }

}