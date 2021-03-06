package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemDropHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * This {@link SlimefunItem} allows you to convert any {@link ItemStack} into a
 * {@link SoulboundItem}. It is also one of the very few utilisations of {@link ItemDropHandler}.
 *
 * @author Linox
 * @author Walshy
 * @author TheBusyBiscuit
 * @see ItemDropHandler
 * @see Soulbound
 */
public class SoulboundRune extends SimpleSlimefunItem<ItemDropHandler> {

    private static final double RANGE = 1.5;

    public SoulboundRune(Category category, SlimefunItemStack item, RecipeType type, ItemStack[] recipe) {
        super(category, item, type, recipe);
    }

    @Override
    public ItemDropHandler getItemHandler() {
        return (e, p, item) -> {
            if (isItem(item.getItemStack())) {

                if (!Slimefun.hasUnlocked(p, SlimefunItems.SOULBOUND_RUNE, true)) {
                    return true;
                }

                Slimefun.runSync(() -> activate(p, e, item), 20L);

                return true;
            }
            return false;
        };
    }

    private void activate(Player p, PlayerDropItemEvent e, Item item) {
        // Being sure the entity is still valid and not picked up or whatsoever.
        if (!item.isValid()) {
            return;
        }

        Location l = item.getLocation();
        Collection<Entity> entites = l.getWorld().getNearbyEntities(l, RANGE, RANGE, RANGE, this::findCompatibleItem);
        Optional<Entity> optional = entites.stream().findFirst();

        if (optional.isPresent()) {
            Item entity = (Item) optional.get();
            ItemStack target = entity.getItemStack();

            SlimefunUtils.setSoulbound(target, true);

            if (target.getAmount() == 1) {
                e.setCancelled(true);

                // This lightning is just an effect, it deals no damage.
                l.getWorld().strikeLightningEffect(l);

                Slimefun.runSync(() -> {
                    // Being sure entities are still valid and not picked up or whatsoever.
                    if (item.isValid() && entity.isValid() && target.getAmount() == 1) {

                        l.getWorld().createExplosion(l, 0);
                        l.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.3F, 1);

                        entity.remove();
                        item.remove();
                        l.getWorld().dropItemNaturally(l, target);

                        SlimefunPlugin.getLocalization().sendMessage(p, "messages.soulbound-rune.success", true);
                    }
                }, 10L);
            } else {
                SlimefunPlugin.getLocalization().sendMessage(p, "messages.soulbound-rune.fail", true);
            }
        }
    }

    private boolean findCompatibleItem(Entity n) {
        if (n instanceof Item) {
            Item item = (Item) n;

            return !SlimefunUtils.isSoulbound(item.getItemStack()) && !isItem(item.getItemStack());
        }

        return false;
    }

}