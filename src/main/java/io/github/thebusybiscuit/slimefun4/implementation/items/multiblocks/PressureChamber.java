package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks;

import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class PressureChamber extends MultiBlockMachine {

    public PressureChamber(Category category, SlimefunItemStack item) {
        super(category, item, new ItemStack[]{
                SlimefunPlugin.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_14) ? new ItemStack(Material.SMOOTH_STONE_SLAB) : new ItemStack(Material.STONE_SLAB), new CustomItem(Material.DISPENSER, "发射器(朝下)"), SlimefunPlugin.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_14) ? new ItemStack(Material.SMOOTH_STONE_SLAB) : new ItemStack(Material.STONE_SLAB),
                new ItemStack(Material.PISTON), new ItemStack(Material.GLASS), new ItemStack(Material.PISTON),
                new ItemStack(Material.PISTON), new ItemStack(Material.CAULDRON), new ItemStack(Material.PISTON)
        }, new ItemStack[0], BlockFace.UP);
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        return recipes.stream().map(items -> items[0]).collect(Collectors.toList());
    }

    @Override
    public void onInteract(Player p, Block b) {
        Block dispBlock = b.getRelative(BlockFace.UP).getRelative(BlockFace.UP);
        Dispenser disp = (Dispenser) dispBlock.getState();
        Inventory inv = disp.getInventory();

        for (ItemStack current : inv.getContents()) {
            for (ItemStack convert : RecipeType.getRecipeInputs(this)) {
                if (convert != null && SlimefunUtils.isItemSimilar(current, convert, true)) {
                    ItemStack output = RecipeType.getRecipeOutput(this, convert);
                    Inventory outputInv = findOutputInventory(output, dispBlock, inv);

                    if (outputInv != null) {
                        ItemStack removing = current.clone();
                        removing.setAmount(convert.getAmount());
                        inv.removeItem(removing);

                        craft(p, b, output, outputInv);
                    } else SlimefunPlugin.getLocalization().sendMessage(p, "machines.full-inventory", true);

                    return;
                }
            }
        }
        SlimefunPlugin.getLocalization().sendMessage(p, "machines.unknown-material", true);
    }

    private void craft(Player p, Block b, ItemStack output, Inventory outputInv) {
        for (int i = 0; i < 4; i++) {
            int j = i;

            Bukkit.getScheduler().runTaskLater(SlimefunPlugin.instance(), () -> {
                p.getWorld().playSound(b.getLocation(), Sound.ENTITY_TNT_PRIMED, 1, 1);
                p.getWorld().playEffect(b.getRelative(BlockFace.UP).getLocation(), Effect.SMOKE, 4);
                p.getWorld().playEffect(b.getRelative(BlockFace.UP).getLocation(), Effect.SMOKE, 4);
                p.getWorld().playEffect(b.getRelative(BlockFace.UP).getLocation(), Effect.SMOKE, 4);

                if (j < 3) {
                    p.getWorld().playSound(b.getLocation(), Sound.ENTITY_TNT_PRIMED, 1F, 1F);
                } else {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 1F);
                    outputInv.addItem(output);
                }
            }, i * 20L);
        }
    }

}
